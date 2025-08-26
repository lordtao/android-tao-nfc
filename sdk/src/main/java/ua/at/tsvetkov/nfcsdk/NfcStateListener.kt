package ua.at.tsvetkov.nfcsdk

/**
 * Created by Alexandr Tsvetkov on 26.08.2025.
 *
 * Interface for receiving callbacks about NFC adapter status and general NFC operation events.
 * Implement this listener to be notified about changes in the NFC adapter's state
 * (e.g., when NFC is enabled/disabled, or when reader mode starts) and any
 * administrative errors encountered by [NfcAdmin].
 */
interface NfcStateListener {

    /**
     * Called when the state of the NFC adapter changes.
     *
     * This method is invoked when the NFC adapter is enabled, disabled,
     * or transitions between these states (e.g., "turning on", "turning off").
     * It can also be called if an NFC-related error state is encountered that
     * is represented by [NfcAdminState].
     *
     * @param state The new [NfcAdminState] of the NFC adapter.
     *              Implementations should handle the different sealed class subtypes
     *              of [NfcAdminState] to react appropriately to the current NFC status.
     */
    fun onNfcStateChanged(state: NfcAdminState)

    /**
     * Called when an administrative error occurs within the [NfcAdmin] operations,
     * such as issues enabling reader mode, or if the NFC adapter is not available.
     *
     * @param error An [NfcAdminError] enum value indicating the specific
     *              administrative error that occurred.
     */
    fun onError(error: NfcAdminError)
}
