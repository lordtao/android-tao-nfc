package ua.at.tsvetkov.nfcsdk

import android.nfc.NdefRecord

/**
 * Created by Alexandr Tsvetkov on 22.08.2025.
 */

/**
 * Интерфейс для получения обратного вызова при сканировании NFC-метки.
 */
interface NfcSAvailableListener {
    fun onNfcAvailable(isAvailable: Boolean)
    fun onNfcAvailability(error: NfcError)
}