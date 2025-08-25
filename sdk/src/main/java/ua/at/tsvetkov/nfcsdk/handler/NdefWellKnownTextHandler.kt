package ua.at.tsvetkov.nfcsdk.handler

/**
 * Created by Alexandr Tsvetkov on 22.08.2025.
 */
import android.nfc.FormatException
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import ua.at.tsvetkov.nfcsdk.NfcError
import ua.at.tsvetkov.nfcsdk.NfcScanListener
import ua.at.tsvetkov.nfcsdk.NfcWriteListener
import ua.at.tsvetkov.util.logger.Log
import java.io.IOException
import java.nio.charset.Charset

private const val FIRST_6_BIT = 0x3F

private const val SEVENTH_BIT = 0x80

/**
 *  An [NfcHandler] implementation specifically for reading and writing
 * NDEF (NFC Data Exchange Format) Well-Known Text records.
 *
 * This handler can parse NDEF messages containing Text records and also
 * prepare NDEF messages for writing text to compatible NFC tags.
 * It primarily focuses on [NdefRecord.RTD_TEXT] and can also attempt to
 * interpret [NdefRecord.RTD_URI] as text.
 *
 * @param languageCode The language code for the text to be written or parsed.
 *      Defaults to "en" (English). This is used when creating
 *      NDEF Text records according to the NDEF RTD_TEXT specification.
 */
class NdefWellKnownTextHandler(
    var languageCode: String = "en",
    nfcScanListener: NfcScanListener<String, ByteArray>? = null,
    nfcWriteListener: NfcWriteListener? = null,
) : NfcHandler<String, ByteArray>(nfcScanListener, nfcWriteListener) {
    override val techList = listOf(Ndef::class.java.name, NdefFormatable::class.java.name)

    override fun isHavePreparedMessageToWrite(): Boolean = preparedNdefMessage != null

    /**
     * Holds the NDEF message that has been prepared for writing to an NFC tag.
     * This property is populated by the [prepareToWrite] method and is set to `null`
     * after a successful write operation or if a new message is prepared,
     * effectively allowing only one message to be pending for write at a time.
     */
    var preparedNdefMessage: NdefMessage? = null

    override fun readMessageFromTag(tag: Tag) {
        val ndef = Ndef.get(tag)
        if (ndef == null) {
            // Try reading as NDEF Formatable if there is a scan listener
            val ndefFormatable = NdefFormatable.get(tag)
            if (ndefFormatable != null) {
                Log.e(NfcError.NDEF_FORMATTABLE_BUT_EMPTY.message)
                onScanError(NfcError.NDEF_FORMATTABLE_BUT_EMPTY)
            } else {
                Log.e(NfcError.NDEF_NOT_SUPPORTED.message)
                onScanError(NfcError.NDEF_NOT_SUPPORTED)
            }
            return
        }

        try {
            ndef.connect()
            val ndefMessage = ndef.ndefMessage ?: ndef.cachedNdefMessage // Try also cache
            if (ndefMessage == null) {
                Log.e(NfcError.NO_NDEF_MESSAGE.message)
                onScanError(NfcError.NO_NDEF_MESSAGE)
                ndef.close()
                return
            }

            val records = ndefMessage.records

            if (records.isEmpty()) {
                Log.e(NfcError.NO_NDEF_RECORDS.message)
                onScanError(NfcError.NO_NDEF_RECORDS)
                ndef.close()
                return
            }
            parse(records)

            ndef.close()

            Log.i("Reads from NFC:\n${records.joinToString("\n")}")
        } catch (e: IOException) {
            Log.e("${NfcError.NFC_IO_ERROR}: ${e.message}", e)
            onScanError(NfcError.NFC_IO_ERROR)
        } catch (e: FormatException) {
            Log.e("${NfcError.NFC_FORMAT_ERROR}: ${e.message}", e)
            onScanError(NfcError.NFC_FORMAT_ERROR)
        } catch (e: Exception) {
            Log.e("${NfcError.UNKNOWN_READ_ERROR}: ${e.message}", e)
            onScanError(NfcError.UNKNOWN_READ_ERROR)
        } finally {
            try {
                if (ndef.isConnected) {
                    ndef.close()
                }
            } catch (e: IOException) {
                {
                    Log.e("${NfcError.ERROR_CLOSING_NDEF_CONNECTION}: ${e.message}", e)
                }
                onScanError(NfcError.ERROR_CLOSING_NDEF_CONNECTION)
            }
        }
    }

    /**
     * Parses an array of [NdefRecord] objects extracted from an NDEF message.
     * This method iterates through the records. For records of TNF_WELL_KNOWN type,
     * it specifically handles RTD_TEXT and RTD_URI types.
     * Text from RTD_TEXT records is accumulated using [appendText].
     * Each valid RTD_URI record triggers an immediate call to [ua.at.tsvetkov.nfcsdk.NfcScanListener.onNfcTagScanned].
     * If any text was accumulated from RTD_TEXT records, [ua.at.tsvetkov.nfcsdk.NfcScanListener.onNfcTagScanned]
     * is called once with the combined text.
     * Unsupported record types are logged.
     *
     * @param records The array of [NdefRecord] instances to parse.
     */
    private fun parse(records: Array<out NdefRecord>) {
        val builder = StringBuilder()
        for (record in records) {
            if (record.tnf == NdefRecord.TNF_WELL_KNOWN) {
                // It is possible to add processing of other types of records (URI, Smart Poster, etc.)
                when (record.type) {
                    NdefRecord.RTD_TEXT, NdefRecord.RTD_URI -> appendText(record, builder)
                    else ->
                        Log.e(
                            "${NfcError.UNSUPPORTED_NDFE_RECORD_TYPE.message}:" +
                                " tnf=${record.tnf}, type=${record.type}",
                        )
                }
            } else {
                Log.e("${NfcError.UNSUPPORTED_NDFE_RECORD_TYPE.message}: tnf=${record.tnf}, type=${record.type}")
            }
        }
        val result = builder.toString().trim()
        if (result.isNotBlank()) {
            nfcScanListener?.onNfcTagScanned(result, NdefRecord.RTD_TEXT)
        }
    }

    /**
     * Decodes the payload of an NDEF Text Record ([NdefRecord.RTD_TEXT]) and appends
     * the extracted text to the provided [StringBuilder].
     * The method determines the text encoding (UTF-8 or UTF-16) and language code
     * length from the status byte of the payload, as per NDEF RTD_TEXT specification.
     * If parsing fails or the payload is malformed, an error is reported via
     * [ua.at.tsvetkov.nfcsdk.NfcScanListener.onNfcScanError].
     *
     * @param record The NDEF record of type RTD_TEXT. Its payload must conform to the
     *               NDEF Text Record Type Definition.
     * @param builder The [StringBuilder] to which the decoded text will be appended,
     *                followed by a newline character.
     */
    private fun appendText(
        record: NdefRecord,
        builder: StringBuilder,
    ) {
        try {
            val payload = record.payload
            val status = payload[0].toInt()
            val languageCodeLength = status and FIRST_6_BIT
            val encoding = if ((status and SEVENTH_BIT) == 0) "UTF-8" else "UTF-16"
            val text =
                String(
                    payload,
                    languageCodeLength + 1,
                    payload.size - languageCodeLength - 1,
                    Charset.forName(encoding),
                )
            builder.append(text).append("\n")
        } catch (e: Exception) {
            Log.e("${NfcError.ERROR_PARSING_NDEF_TEXT_RECORD.message}: ${e.message}", e)
            nfcScanListener?.onNfcScanError(NfcError.ERROR_PARSING_NDEF_TEXT_RECORD)
        }
    }

    override fun writeMessageToTag(tag: Tag) {
        val ndef = Ndef.get(tag)
        preparedNdefMessage?.let { message ->
            if (ndef != null) {
                writeWithNdef(ndef, message)
            } else {
                writeWithNdefFormatable(tag, message)
            }
        }
    }

    private fun writeWithNdef(
        ndef: Ndef,
        message: NdefMessage,
    ) {
        try {
            ndef.connect()
            if (!ndef.isWritable) {
                Log.e(NfcError.TAG_READ_ONLY.message)
                onWriteError(NfcError.TAG_READ_ONLY)
                ndef.close()
                return
            }
            val maxSize = ndef.maxSize
            if (message.toByteArray().size > maxSize) {
                Log.e(NfcError.DATA_TOO_LARGE.message)
                onWriteError(NfcError.DATA_TOO_LARGE)
                ndef.close()
                return
            }
            ndef.writeNdefMessage(message)

            Log.i("NDEF message successfully written.")
            nfcWriteListener?.onNfcTagWritten()
            preparedNdefMessage = null
        } catch (e: IOException) {
            Log.e("${NfcError.NFC_WRITE_IO_ERROR}: ${e.message}", e)
            onWriteError(NfcError.NFC_WRITE_IO_ERROR)
        } catch (e: FormatException) {
            Log.e("${NfcError.NFC_WRITE_FORMAT_ERROR}: ${e.message}", e)
            onWriteError(NfcError.NFC_WRITE_FORMAT_ERROR)
        } catch (e: Exception) {
            Log.e("${NfcError.UNKNOWN_WRITE_ERROR}: ${e.message}", e)
            onWriteError(NfcError.UNKNOWN_WRITE_ERROR)
        } finally {
            try {
                if (ndef.isConnected) {
                    ndef.close()
                }
            } catch (e: IOException) {
                {
                    Log.e("${NfcError.ERROR_CLOSING_NDEF_CONNECTION}: ${e.message}", e)
                }
                onWriteError(NfcError.ERROR_CLOSING_NDEF_CONNECTION)
            }
        }
        return
    }

    private fun writeWithNdefFormatable(
        tag: Tag,
        message: NdefMessage,
    ) {
        val ndefFormatable = NdefFormatable.get(tag)
        if (ndefFormatable != null) {
            try {
                ndefFormatable.connect()
                ndefFormatable.format(message)

                Log.i("NDEF message successfully written.")
                nfcWriteListener?.onNfcTagWritten()
                preparedNdefMessage = null
            } catch (e: IOException) {
                Log.e("${NfcError.NFC_FORMATTABLE_WRITE_IO_ERROR}: ${e.message}", e)
                onWriteError(NfcError.NFC_FORMATTABLE_WRITE_IO_ERROR)
            } catch (e: FormatException) {
                Log.e("${NfcError.NFC_FORMATTABLE_FORMAT_ERROR}: ${e.message}", e)
                onWriteError(NfcError.NFC_FORMATTABLE_FORMAT_ERROR)
            } catch (e: Exception) {
                Log.e("${NfcError.UNKNOWN_FORMATTABLE_ERROR}: ${e.message}", e)
                onWriteError(NfcError.UNKNOWN_FORMATTABLE_ERROR)
            } finally {
                try {
                    if (ndefFormatable.isConnected) {
                        ndefFormatable.close()
                    }
                } catch (e: IOException) {
                    {
                        Log.e("${NfcError.ERROR_CLOSING_NDEF_FORMATABLE_CONNECTION}: ${e.message}", e)
                    }
                    onWriteError(NfcError.ERROR_CLOSING_NDEF_FORMATABLE_CONNECTION)
                }
            }
        } else {
            Log.e(NfcError.TAG_NOT_NDEF_COMPATIBLE.message)
            onWriteError(NfcError.TAG_NOT_NDEF_COMPATIBLE)
        }
    }

    override fun prepareToWrite(data: String) {
        val langBytes = languageCode.toByteArray(Charset.forName("US-ASCII"))
        val textBytes = data.toByteArray(Charset.forName("UTF-8"))
        val payload = ByteArray(1 + langBytes.size + textBytes.size)
        payload[0] = (langBytes.size).toByte() // Status byte: UTF-8 encoding, language code length
        System.arraycopy(langBytes, 0, payload, 1, langBytes.size)
        System.arraycopy(textBytes, 0, payload, 1 + langBytes.size, textBytes.size)

        val record = NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, ByteArray(0), payload)
        Log.d("Data for writing has been prepared.: '$data'")
        preparedNdefMessage = NdefMessage(arrayOf(record))
    }
}
