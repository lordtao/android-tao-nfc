package ua.at.tsvetkov.nfcsdk.handler

/**
 * Created by Alexandr Tsvetkov on 27.08.2025.
 */

import android.nfc.Tag
import android.nfc.tech.MifareUltralight
import java.io.IOException
import kotlin.math.ceil
import ua.at.tsvetkov.nfcsdk.NfcMessage
import ua.at.tsvetkov.nfcsdk.NfcReadListener
import ua.at.tsvetkov.nfcsdk.NfcWriteListener
import ua.at.tsvetkov.nfcsdk.parser.MifareUltralightStringParser
import ua.at.tsvetkov.nfcsdk.preparer.MifareUltralightStringPreparer

class NfcMifareUltralightTextHandler(
    nfcReadListener: NfcReadListener<String>?, // R = String
    nfcWriteListener: NfcWriteListener?,
    private val startDataPage: Int = 4,
    private val pagesToAccess: Int = 12
) : NfcHandler<ByteArray, String>( // D = ByteArray, R = String
    MifareUltralightStringParser(pagesToAccess * MifareUltralightStringPreparer.PAGE_SIZE),
    MifareUltralightStringPreparer(startDataPage, pagesToAccess * MifareUltralightStringPreparer.PAGE_SIZE),
    nfcReadListener,
    nfcWriteListener
) {
    companion object {
        const val PAGE_SIZE = 4
    }

    private val totalBytesToAccess = pagesToAccess * PAGE_SIZE

    init {
        if (startDataPage < 4) {
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

    override fun prepareCleaningData() {
        preparedData = byteArrayOf(0x00.toByte())
    }

    override fun readDataFromTag(tag: Tag) {
        val ultralight = MifareUltralight.get(tag)
        if (ultralight == null) {
            onScanEvent(NfcMessage.TAG_NOT_MIFARE_ULTRALIGHT)
            return
        }

        var rawDataBuffer = byteArrayOf()

        try {
            ultralight.connect()
            for (i in 0 until pagesToAccess step 4) {
                val currentOffset = startDataPage + i
                if (i < pagesToAccess) {
                    val pagesRead = ultralight.readPages(currentOffset)
                    val bytesToTakeFromThisBlock = if (i + 4 <= pagesToAccess) {
                        pagesRead.size
                    } else {
                        (pagesToAccess - i) * PAGE_SIZE
                    }
                    rawDataBuffer += pagesRead.copyOfRange(0, bytesToTakeFromThisBlock.coerceAtMost(pagesRead.size))
                }
            }

            if (rawDataBuffer.isEmpty() && pagesToAccess > 0) {
                onScanEvent(NfcMessage.TAG_EMPTY_OR_UNREADABLE)
                return
            }

            val parsedStringList = parser.parse(rawDataBuffer)
            nfcReadListener?.onRead(parsedStringList)
        } catch (e: IOException) {
            onScanEvent(NfcMessage.IO_EXCEPTION_WHILE_READING, e)
        } catch (e: IllegalArgumentException) {
            onScanEvent(NfcMessage.PARSING_ERROR, e)
        } catch (e: Exception) {
            onScanEvent(NfcMessage.READ_GENERAL_ERROR, e)
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
            onWriteEvent(NfcMessage.NO_DATA_TO_WRITE)
            return
        }

        val ultralight = MifareUltralight.get(tag)
        if (ultralight == null) {
            onWriteEvent(NfcMessage.TAG_NOT_MIFARE_ULTRALIGHT)
            return
        }

        try {
            ultralight.connect()
            var bytesActuallyWrittenCount = 0 // Сколько полезных байт (длина + строка) было записано
            for (i in 0 until ceil(dataBytesToWrite.size.toDouble() / PAGE_SIZE).toInt()) {
                val pageOffset = startDataPage + i
                val dataIndexStart = i * PAGE_SIZE

                if (i >= pagesToAccess) {
                    onWriteEvent(
                        NfcMessage.NOT_ENOUGH_SPACE,
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
                nfcWriteListener?.onWritten()
            } else {
                // Этого не должно произойти, если логика выше верна
                onWriteEvent(
                    NfcMessage.WRITE_VERIFICATION_ERROR,
                    IllegalStateException(
                        "Mismatch in written bytes count. " +
                            "Expected ${dataBytesToWrite.size}, wrote $bytesActuallyWrittenCount"
                    )
                )
            }
        } catch (e: IOException) {
            onWriteEvent(NfcMessage.IO_EXCEPTION_WHILE_WRITING, e)
        } catch (e: IllegalArgumentException) { // От preparer или внутренних проверок
            onWriteEvent(NfcMessage.PREPARE_DATA_ERROR, e)
        } catch (e: Exception) {
            onWriteEvent(NfcMessage.WRITE_GENERAL_ERROR, e)
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
