package ua.at.tsvetkov.nfcsdk.handler

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import ua.at.tsvetkov.nfcsdk.NfcError
import ua.at.tsvetkov.nfcsdk.NfcScanListener
import ua.at.tsvetkov.nfcsdk.NfcWriteListener

/**
 * Created by Alexandr Tsvetkov on 24.08.2025.
 */
abstract class NfcHandler<T>(
    protected var nfcScanListener: NfcScanListener<T>? = null,
    protected var nfcWriteListener: NfcWriteListener? = null,
) {

    var isEnabled: Boolean = true

    var preparedNdefMessage: NdefMessage? = null

    fun onNfcTagScanned(message: Array<out NdefRecord>) {
        parse(message)?.let { result ->
            nfcScanListener?.onNfcTagScanned(result)
        }
    }

    fun onNfcTagWritten() {
        nfcWriteListener?.onNfcTagWritten()
    }

    fun onScanError(error: NfcError) {
        nfcScanListener?.onNfcScanError(error)
    }

    fun onWriteError(error: NfcError) {
        nfcWriteListener?.onNfcWriteError(error)
    }

    /**
     *
     */
    abstract fun parse(records: Array<out NdefRecord>): T?

    /**
     * Prepares data for writing to an NFC tag.
     * This method must be called before the tag is brought near the device for writing.
     *
     * @param data Text to write to the label.
     **/
    abstract fun prepareToWrite(data: T): NdefMessage

}