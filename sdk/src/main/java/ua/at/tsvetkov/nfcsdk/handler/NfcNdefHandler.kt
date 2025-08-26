package ua.at.tsvetkov.nfcsdk.handler

/**
 * Created by Alexandr Tsvetkov on 26.08.2025.
 */

import android.nfc.NdefMessage
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import java.io.IOException
import ua.at.tsvetkov.nfcsdk.NfcError
import ua.at.tsvetkov.nfcsdk.NfcScanListener
import ua.at.tsvetkov.nfcsdk.NfcWriteListener
import ua.at.tsvetkov.nfcsdk.parser.NfcDataParser
import ua.at.tsvetkov.nfcsdk.preparer.NfcDataPreparer

/**
 * An abstract NFC handler specialized for reading and writing NDEF (NFC Data Exchange Format)
 * messages to and from NFC tags.
 *
 * This class extends [NfcHandler] and provides concrete implementations for interacting
 * with tags that support the [Ndef] technology. It also handles tags that are
 * [NdefFormatable] by formatting them and then writing the NDEF message.
 *
 * It uses an [NfcDataParser] to convert raw [NdefMessage] objects read from a tag
 * into a higher-level, application-specific data type [R]. Conversely, it uses an
 * [NfcDataPreparer] to convert data of type [R] back into an [NdefMessage] for writing.
 *
 * Error handling and success notifications are delegated to the provided [NfcScanListener]
 * and [NfcWriteListener].
 *
 * @param R The high-level, structured data type that the application consumes or produces.
 *          This is the type that the [reader] produces and the [preparer] consumes.
 *          Scan results are delivered as this type via [NfcScanListener].
 * @param reader The [NfcDataParser] responsible for parsing [NdefMessage] objects
 * read from the tag into the application-specific data type [R].
 * @param preparer The [NfcDataPreparer] responsible for preparing application-specific
 * data of type [R] into an [NdefMessage] for writing to the tag.
 * @param nfcScanListener An optional [NfcScanListener] to receive callbacks for NDEF scan
 * events, delivering parsed data of type [R] or scan errors.
 * @param nfcWriteListener An optional [NfcWriteListener] to receive callbacks for NDEF write
 * events, indicating success or failure.
 */
abstract class NfcNdefHandler<R>(
    reader: NfcDataParser<NdefMessage, R>,
    preparer: NfcDataPreparer<R, NdefMessage>,
    nfcScanListener: NfcScanListener<R>? = null,
    nfcWriteListener: NfcWriteListener? = null
) : NfcHandler<NdefMessage, R>(reader, preparer, nfcScanListener, nfcWriteListener) {

    override fun readDataFromTag(tag: Tag) {
        val ndef = Ndef.get(tag)
        if (ndef == null) {
            onScanError(NfcError.TAG_NOT_NDEF_FORMATTED)
            return
        }

        try {
            ndef.connect()
            val ndefMessage: NdefMessage? = ndef.cachedNdefMessage ?: ndef.ndefMessage

            if (ndefMessage == null) {
                onScanError(NfcError.NDEF_MESSAGE_NULL)
                return
            }

            val result = reader.parse(ndefMessage)

            nfcScanListener?.onNfcTagScanned(result)
        } catch (e: IOException) {
            onScanError(NfcError.IO_EXCEPTION_WHILE_READING, e)
        } catch (e: Exception) {
            onScanError(NfcError.GENERAL_READ_ERROR, e)
        } finally {
            try {
                ndef.close()
            } catch (e: IOException) {
                onScanError(NfcError.CLOSE_CONNECTION_ERROR, e)
            }
        }
    }

    override fun writeDataToTag(tag: Tag) {
        preparedData?.let { message ->
            var ndef: Ndef? = null
            var ndefFormatable: NdefFormatable? = null

            try {
                ndef = Ndef.get(tag)
                if (ndef != null) {
                    ndef.connect()
                    if (!ndef.isWritable) {
                        nfcWriteListener?.onNfcWriteError(NfcError.TAG_NOT_WRITABLE)
                        // No need to close if not writable
                        // (it wasn't opened for write or is already closed by isWritable check)
                        return
                    }
                    if (ndef.maxSize < message.toByteArray().size) {
                        nfcWriteListener?.onNfcWriteError(NfcError.NOT_ENOUGH_SPACE)
                        return // Connection will be closed in finally
                    }
                    ndef.writeNdefMessage(message)
                    nfcWriteListener?.onNfcWriteSuccess()
                } else {
                    ndefFormatable = NdefFormatable.get(tag)
                    if (ndefFormatable != null) {
                        ndefFormatable.connect()
                        ndefFormatable.format(message)
                        nfcWriteListener?.onNfcWriteSuccess()
                    } else {
                        onWriteError(NfcError.TAG_NOT_NDEF_COMPLIANT)
                    }
                }
            } catch (e: IOException) {
                onWriteError(NfcError.IO_EXCEPTION_WHILE_WRITING, e)
            } catch (e: Exception) {
                onWriteError(NfcError.GENERAL_WRITE_ERROR, e)
            } finally {
                try {
                    ndef?.close()
                } catch (e: IOException) {
                    onWriteError(NfcError.CLOSE_CONNECTION_ERROR, e)
                }
                try {
                    ndefFormatable?.close()
                } catch (e: IOException) {
                    onWriteError(NfcError.CLOSE_CONNECTION_ERROR, e)
                }
            }
            preparedData = null
        } ?: run {
            onWriteError(NfcError.NO_DATA_TO_WRITE)
        }
    }
}
