package ua.at.tsvetkov.nfcsdk

/**
 * Created by Alexandr Tsvetkov on 22.08.2025.
 */

/**
 * Interface for receiving new Nfc state.
 */
interface NfcStateListener {
    fun onNfcStateChanged(state: Int)
    fun onError(error: NfcAdminError)
}