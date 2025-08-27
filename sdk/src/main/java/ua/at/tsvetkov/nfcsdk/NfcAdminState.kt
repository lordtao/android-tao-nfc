package ua.at.tsvetkov.nfcsdk

/**
 * Created by Alexandr Tsvetkov on 26.08.2025.
 *
 * Represents the various states of the NFC adapter on the device.
 * This sealed class is used to inform about the current status of NFC
 * functionality, including transitional states and error conditions.
 *
 */
sealed class NfcAdminState(val message: String) {
    /**
     * Indicates that the NFC adapter is currently enabled and ready for use.
     */
    object NfcOn : NfcAdminState("NFC adapter is currently enabled and ready for use.")

    /**
     * Indicates that the NFC adapter is currently disabled.
     */
    object NfcOff : NfcAdminState("NFC adapter is currently disabled.")

    /**
     * Indicates that the NFC adapter is in the process of being enabled.
     * This is a transitional state.
     */
    object NfcTurningOn : NfcAdminState("NFC adapter is in the process of being enabled.")

    /**
     * Indicates that the NFC adapter is in the process of being disabled.
     * This is a transitional state.
     */
    object NfcTurningOff : NfcAdminState("NFC adapter is in the process of being disabled.")

    /**
     * Indicates that the device's NFC adapter is not available (e.g., hardware not present).
     */
    object NfcNotAvailable : NfcAdminState(
        "The device's NFC adapter is not available (e.g., hardware not present)."
    )

    /**
     * Represents an undefined or indeterminate state of the NFC adapter.
     * This state can be used as an initial state before the actual NFC status
     * has been determined, or if the NFC status cannot be clearly ascertained.
     */
    object NfcUndefined : NfcAdminState("Undefined or indeterminate state of the NFC adapter.")

    fun getName() = this::class.simpleName
}
