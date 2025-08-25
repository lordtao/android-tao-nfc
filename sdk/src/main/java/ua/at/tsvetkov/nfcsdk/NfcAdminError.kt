package ua.at.tsvetkov.nfcsdk

/**
 * Represents various administrative errors that can occur within the [NfcAdmin] class,
 * typically related to NFC adapter state, reader mode management, or tag discovery issues
 * before specific NDEF parsing or writing.
 * Each error type includes a descriptive message.
 *
 * Created by Alexandr Tsvetkov on 22.08.2025.
 */
enum class NfcAdminError(
    /** A human-readable message describing the administrative error. */
    val message: String
) {

    /** Indicates an error occurred while attempting to disable NFC reader mode. */
    ERROR_WHILE_SHUTTING_DOWN_READER_MODE("Error while shutting down reader mode."),

    /** Indicates an error occurred while attempting to enable NFC reader mode. */
    ERROR_WHEN_TURNING_ON_READER_MODE("Error when turning on reader mode."),

    /** Indicates that the device's NFC adapter is not available (e.g., hardware not present). */
    NFC_ADAPTER_IS_NOT_AVAILABLE("NFC Adapter is not available on this device."),

    /** Indicates that the device's NFC adapter is available but currently disabled in system settings. */
    NFC_ADAPTER_IS_NOT_ENABLED("NFC Adapter is not enabled."),

    /**
     * Indicates that an NFC tag was expected (e.g., from an intent or reader callback)
     * but was not found or was null.
     */
    TAG_NOT_FOUND("NFC Tag not found in intent.");

    /**
     * Returns the human-readable message associated with this administrative error.
     * @return The error message string.
     */
    override fun toString(): String {
        return message
    }

}
