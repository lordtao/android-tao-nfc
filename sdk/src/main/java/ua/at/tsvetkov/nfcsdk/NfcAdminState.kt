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

    /**
     * Represents the state where an NFC tag has been discovered and its
     * supported technologies have been identified.
     *
     * This state is typically triggered when [android.nfc.NfcAdapter.ReaderCallback.onTagDiscovered]
     * is called, providing information about the tag's capabilities.
     * The associated [message] in the parent class will contain a comma-separated
     * list of these discovered technologies.
     *
     * @param tech A list of strings, where each string is a fully qualified
     * class name of a discovered NFC technology (e.g., "[android.nfc.tech.NfcA]", "[android.nfc.tech.Ndef]").
     */
    class NfcTechDiscovered(val tech: List<String>) :
        NfcAdminState(
            "NFC tech discovered: ${tech.joinToString(", ")}"
        ){
        /**
         * Extracts and returns the simple names of the discovered NFC technologies.
         *
         * For example, if a technology is reported as "[android.nfc.tech.NfcA]",
         * this function will return `NfcA`.
         *
         * @return A list of strings, where each string is the simple class name
         * of a discovered NFC technology.
         */
        fun getTechNames(): List<String> {
                return tech.map { it.substringAfterLast('.') }
            }
        }

    fun getName() = this::class.simpleName

    /**
     * Returns the human-readable message associated with NFC adapter state.
     * @return The error message string.
     */
    override fun toString(): String = message
}
