package ua.at.tsvetkov.nfcsdk

import android.nfc.NdefRecord

/**
 * Created by Alexandr Tsvetkov on 22.08.2025.
 */

/**
 * Интерфейс для получения обратного вызова при сканировании NFC-метки.
 */
interface NfcScanListener {
    fun onNfcTagScanned(message: Array<out NdefRecord>)
    fun onNfcScanError(error: NfcError)
}