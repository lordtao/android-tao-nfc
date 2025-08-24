package ua.at.tsvetkov.nfcsdk

/**
 * Created by Alexandr Tsvetkov on 22.08.2025.
 */

/**
 * Интерфейс для получения обратного вызова при записи на NFC-метку.
 */
interface NfcWriteListener {
    fun onNfcTagWritten()
    fun onNfcWriteError(error: NfcError)
}