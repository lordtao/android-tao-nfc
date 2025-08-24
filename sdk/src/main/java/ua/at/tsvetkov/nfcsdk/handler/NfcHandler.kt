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
    private var nfcScanListener: NfcScanListener? = null,
    private var nfcWriteListener: NfcWriteListener? = null,
) {

    var isEnabled: Boolean = true

    private var result: T? = null

    protected var ndefMessage: NdefMessage? = null

    fun getResult(): T? {
        return result
    }

    fun getNdefMessage(): NdefMessage? {
        return ndefMessage
    }

    fun onNfcTagScanned(message: Array<out NdefRecord>) {
        result = parse(message)
        nfcScanListener?.onNfcTagScanned(message)
    }

    fun onNfcTagWritten() {
        nfcWriteListener?.onNfcTagWritten()
    }

    fun onError(error: NfcError) {
        nfcScanListener?.onNfcScanError(error)
        nfcWriteListener?.onNfcWriteError(error)
    }

    fun onScanError(error: NfcError) {
        nfcScanListener?.onNfcScanError(error)
    }

    fun onWriteError(error: NfcError) {
        nfcWriteListener?.onNfcWriteError(error)
    }

    abstract fun parse(records: Array<out NdefRecord>): T

    abstract fun prepareToWrite(data: T): NdefMessage

}