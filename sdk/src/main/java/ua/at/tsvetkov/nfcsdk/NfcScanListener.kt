package ua.at.tsvetkov.nfcsdk

/**
 * Created by Alexandr Tsvetkov on 22.08.2025.
 */

/**
 * Интерфейс для получения обратного вызова при сканировании NFC-метки.
 */
interface NfcScanListener<K> {
    fun onNfcTagScanned(message: K)
    fun onNfcScanError(error: NfcError)
}