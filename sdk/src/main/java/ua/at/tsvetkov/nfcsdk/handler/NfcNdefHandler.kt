package ua.at.tsvetkov.nfcsdk.handler

/**
 * Created by Alexandr Tsvetkov on 26.08.2025.
 */

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import java.io.IOException
import ua.at.tsvetkov.nfcsdk.NfcError
import ua.at.tsvetkov.nfcsdk.NfcListener
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
 * Error handling and success notifications are delegated to the provided [NfcListener]
 *
 * @param R The high-level, structured data type that the application consumes or produces.
 *          This is the type that the [reader] produces and the [preparer] consumes.
 *          Scan results are delivered as this type via [NfcListener].
 * @param parser The [NfcDataParser] responsible for parsing [NdefMessage] objects
 * read from the tag into the application-specific data type [R].
 * @param preparer The [NfcDataPreparer] responsible for preparing application-specific
 * data of type [R] into an [NdefMessage] for writing to the tag.
 * @param nfcListener  [NfcListener] to receive callbacks for NDEF read
 * and write errors, delivering parsed data of type [R] or scan errors.
 */
abstract class NfcNdefHandler<R>(
    parser: NfcDataParser<NdefMessage, R>,
    preparer: NfcDataPreparer<R, NdefMessage>,
    nfcListener: NfcListener<R>
) : NfcHandler<NdefMessage, R>(parser, preparer, nfcListener) {

    override val supportedTechs = listOf(Ndef::class.java.name, NdefFormatable::class.java.name)

    @Suppress("ReturnCount")
    override fun readDataFromTag(tag: Tag) {
        val ndef = Ndef.get(tag)
        val ndefFormatable = NdefFormatable.get(tag)
        if (ndef == null && ndefFormatable == null) {
            onError(NfcError.READ_TAG_NOT_NDEF_COMPLIANT)
            return
        }
        if (ndef == null) {
            onError(NfcError.READ_TAG_NOT_NDEF_FORMATTED)
            return
        }

        try {
            ndef.connect()
            val ndefMessage: NdefMessage? = ndef.cachedNdefMessage ?: ndef.ndefMessage

            if (ndefMessage == null) {
                onError(NfcError.READ_NDEF_MESSAGE_NULL)
                return
            }

            if (isNdefEmpty(ndefMessage)) {
                onError(NfcError.READ_NDEF_MESSAGE_EMPTY)
                return
            }

            val result = parser.parse(ndefMessage)

            nfcListener.onRead(result)
        } catch (e: IOException) {
            onError(NfcError.READ_IO_EXCEPTION, e)
        } catch (e: Exception) {
            onError(NfcError.READ_GENERAL_ERROR, e)
        } finally {
            try {
                ndef.close()
            } catch (e: IOException) {
                onError(NfcError.READ_CLOSE_CONNECTION_ERROR, e)
            }
        }
    }

    private fun isNdefEmpty(ndefMessage: NdefMessage): Boolean {
        if (ndefMessage.records.isEmpty()) {
            return true
        } else {
            var allEmpty = true
            for (record in ndefMessage.records) {
                if (record.tnf != NdefRecord.TNF_EMPTY) {
                    allEmpty = false
                    break
                }
            }
            return allEmpty
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
                        nfcListener.onError(NfcError.WRITE_TAG_NOT_WRITABLE)
                        // No need to close if not writable
                        // (it wasn't opened for write or is already closed by isWritable check)
                        return
                    }
                    if (ndef.maxSize < message.toByteArray().size) {
                        nfcListener.onError(NfcError.WRITE_NOT_ENOUGH_SPACE)
                        return // Connection will be closed in finally
                    }
                    ndef.writeNdefMessage(message)
                    nfcListener.onWriteSuccess()
                } else {
                    ndefFormatable = NdefFormatable.get(tag)
                    if (ndefFormatable != null) {
                        ndefFormatable.connect()
                        ndefFormatable.format(message)
                        nfcListener.onWriteSuccess()
                    } else {
                        onError(NfcError.WRITE_TAG_NOT_NDEF_COMPLIANT)
                    }
                }
            } catch (e: IOException) {
                onError(NfcError.WRITE_IO_EXCEPTION, e)
            } catch (e: Exception) {
                onError(NfcError.WRITE_GENERAL_ERROR, e)
            } finally {
                try {
                    ndef?.close()
                } catch (e: IOException) {
                    onError(NfcError.WRITE_CLOSE_CONNECTION_ERROR, e)
                }
                try {
                    ndefFormatable?.close()
                } catch (e: IOException) {
                    onError(NfcError.WRITE_CLOSE_CONNECTION_ERROR, e)
                }
            }
            preparedData = null
        } ?: run {
            onError(NfcError.WRITE_NO_DATA_TO_WRITE)
        }
    }

    /**
     * Prepares an NDEF message with a single empty record for writing.
     * This effectively "clears" or "erases" existing NDEF content on a tag
     * by overwriting it with a minimal, empty NDEF message.
     */
    override fun prepareCleaningData() {
        val emptyRecord = NdefRecord(NdefRecord.TNF_EMPTY, null, null, null)
        preparedData = NdefMessage(arrayOf(emptyRecord))
    }

    companion object {

        fun isPossibleEmptyTag(nfcError: NfcError) = nfcError == NfcError.READ_TAG_NOT_NDEF_FORMATTED ||
            nfcError == NfcError.READ_NDEF_MESSAGE_NULL ||
            nfcError == NfcError.READ_NDEF_MESSAGE_EMPTY
    }
}
