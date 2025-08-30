package ua.at.tsvetkov.nfcsdk.handler

/**
 * Created by Alexandr Tsvetkov on 27.08.2025.
 */

import android.nfc.Tag
import android.nfc.tech.MifareUltralight
import java.io.IOException
import kotlin.math.ceil
import ua.at.tsvetkov.nfcsdk.NfcError
import ua.at.tsvetkov.nfcsdk.NfcListener
import ua.at.tsvetkov.nfcsdk.parser.MifareUltralightStringParser
import ua.at.tsvetkov.nfcsdk.preparer.MifareUltralightStringPreparer

/**
 * An [NfcHandler] implementation specifically designed for reading and writing
 * text data to MIFARE Ultralight NFC tags.
 *
 * It uses [ByteArray] as the intermediate data type (raw tag data) and [String]
 * as the application-level data type.
 *
 * This handler reads a sequence of pages, parses the combined byte array as a string
 * (typically length-prefixed), and prepares a string for writing by converting it
 * into a byte array formatted for MIFARE Ultralight pages.
 *
 * @param nfcListener The listener for NFC read/write events, expecting [String] data.
 * @param startDataPage The starting page index on the MIFARE Ultralight tag for data operations.
 * Defaults to page 4, as earlier pages often contain configuration or lock bits.
 * @param pagesToAccess The total number of pages to be read or that can be written,
 * starting from [startDataPage]. This determines the maximum data capacity.
 * Defaults to 12 pages.
 */
class NfcMifareUltralightTextHandler(
    nfcListener: NfcListener<String>, // R = String
    private val startDataPage: Int = START_DATA_PAGE,
    private val pagesToAccess: Int = MAX_PAGES_TO_ACCESS
) : NfcHandler<ByteArray, String>( // D = ByteArray, R = String
    MifareUltralightStringParser(pagesToAccess * MifareUltralightStringPreparer.PAGE_SIZE),
    MifareUltralightStringPreparer(startDataPage, pagesToAccess * MifareUltralightStringPreparer.PAGE_SIZE),
    nfcListener
) {
    companion object {
        const val START_DATA_PAGE = 4
        const val PAGE_SIZE = 4
        const val MAX_PAGES_TO_ACCESS = 12
    }

    private val totalBytesToAccess = pagesToAccess * PAGE_SIZE

    init {
        if (startDataPage < START_DATA_PAGE) {
            System.err.println(
                "Warning: startDataPage ($startDataPage) is less than 4. " +
                    "Writing to early pages can be risky."
            )
        }
        if (totalBytesToAccess <= 1) {
            throw IllegalArgumentException(
                "pagesToAccess must result in at least 2 bytes " +
                    "of space for length byte and data."
            )
        }
    }

    override val supportedTechs: List<String> = listOf(MifareUltralight::class.java.name)

    override fun prepareCleaningData() {
        preparedData = byteArrayOf(0x00.toByte())
    }

    override fun readDataFromTag(tag: Tag) {
        val ultralight = MifareUltralight.get(tag)
        if (ultralight == null) {
            onError(NfcError.READ_TAG_NOT_MIFARE_ULTRALIGHT)
            return
        }

        var rawDataBuffer = byteArrayOf()

        try {
            ultralight.connect()
            for (i in 0 until pagesToAccess step START_DATA_PAGE) {
                val currentOffset = startDataPage + i
                if (i < pagesToAccess) {
                    val pagesRead = ultralight.readPages(currentOffset)
                    val bytesToTakeFromThisBlock = if (i + START_DATA_PAGE <= pagesToAccess) {
                        pagesRead.size
                    } else {
                        (pagesToAccess - i) * PAGE_SIZE
                    }
                    rawDataBuffer += pagesRead.copyOfRange(0, bytesToTakeFromThisBlock.coerceAtMost(pagesRead.size))
                }
            }

            if (rawDataBuffer.isEmpty() && pagesToAccess > 0) {
                onError(NfcError.READ_TAG_EMPTY_OR_UNREADABLE)
                return
            }

            val parsedStringList = parser.parse(rawDataBuffer)
            nfcListener.onRead(parsedStringList)
        } catch (e: IOException) {
            onError(NfcError.READ_IO_EXCEPTION, e)
        } catch (e: Exception) {
            onError(NfcError.READ_GENERAL_ERROR, e)
        } finally {
            try {
                ultralight.close()
            } catch (e: IOException) {
                // Логируем или обрабатываем ошибку закрытия, если необходимо
                System.err.println("IOException while closing MifareUltralight: ${e.message}")
            }
        }
    }

    override fun writeDataToTag(tag: Tag) {
        val dataBytesToWrite = this.preparedData
        if (dataBytesToWrite == null || dataBytesToWrite.isEmpty()) {
            onError(NfcError.WRITE_NO_DATA_TO_WRITE)
            return
        }

        val ultralight = MifareUltralight.get(tag)
        if (ultralight == null) {
            onError(NfcError.READ_TAG_NOT_MIFARE_ULTRALIGHT)
            return
        }

        try {
            ultralight.connect()
            var bytesActuallyWrittenCount = 0 // Сколько полезных байт (длина + строка) было записано
            for (i in 0 until ceil(dataBytesToWrite.size.toDouble() / PAGE_SIZE).toInt()) {
                val pageOffset = startDataPage + i
                val dataIndexStart = i * PAGE_SIZE

                if (i >= pagesToAccess) {
                    onError(
                        NfcError.WRITE_NOT_ENOUGH_SPACE,
                        IllegalStateException(
                            "Data too long for configured pagesToAccess ($pagesToAccess pages)." +
                                " Need to write to page index $i."
                        )
                    )
                    return
                }

                val pageData = ByteArray(PAGE_SIZE)
                val bytesToCopyForThisPage = (dataBytesToWrite.size - dataIndexStart).coerceAtMost(PAGE_SIZE)

                System.arraycopy(dataBytesToWrite, dataIndexStart, pageData, 0, bytesToCopyForThisPage)
                ultralight.writePage(pageOffset, pageData)
                bytesActuallyWrittenCount += bytesToCopyForThisPage // Считаем только полезные байты
            }

            // Убедимся, что количество записанных "полезных" байт совпадает с размером подготовленных данных
            if (bytesActuallyWrittenCount == dataBytesToWrite.size) {
                nfcListener.onWriteSuccess()
            } else {
                // Этого не должно произойти, если логика выше верна
                onError(
                    NfcError.WRITE_VERIFICATION_ERROR,
                    IllegalStateException(
                        "Mismatch in written bytes count. " +
                            "Expected ${dataBytesToWrite.size}, wrote $bytesActuallyWrittenCount"
                    )
                )
            }
        } catch (e: IOException) {
            onError(NfcError.WRITE_IO_EXCEPTION, e)
        } catch (e: Exception) {
            onError(NfcError.WRITE_GENERAL_ERROR, e)
        } finally {
            try {
                ultralight.close()
            } catch (e: IOException) {
                System.err.println("IOException while closing MifareUltralight after write: ${e.message}")
            }
            clearPreparedData()
        }
    }
}
