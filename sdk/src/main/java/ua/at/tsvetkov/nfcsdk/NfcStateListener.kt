package ua.at.tsvetkov.nfcsdk

/**
 * Interface for receiving callbacks about NFC adapter status and general NFC operation events.
 * Implement this listener to be notified about changes in the NFC adapter's state
 * (e.g., when NFC is enabled/disabled, or when reader mode starts) and any
 * administrative errors encountered by [NfcAdmin].
 */
interface NfcStateListener {

    /**
     * Called when the NFC reader mode has been successfully started by [NfcAdmin].
     * This indicates that the app is now actively listening for NFC tags.
     */
    fun onNfcStarted()

    /**
     * Called when the state of the device's NFC adapter changes.
     *
     * @param state The new state of the NFC adapter. Common values include:
     *              - [NfcAdapter.STATE_OFF]
     *              - [NfcAdapter.STATE_TURNING_ON]
     *              - [NfcAdapter.STATE_ON]
     *              - [NfcAdapter.STATE_TURNING_OFF]
     */
    fun onNfcStateChanged(state: Int)

    /**
     * Called when an administrative error occurs within the [NfcAdmin] operations,
     * such as issues enabling reader mode, or if the NFC adapter is not available.
     *
     * @param error An [NfcAdminError] enum value indicating the specific
     *              administrative error that occurred.
     */
    fun onError(error: NfcAdminError)
}
