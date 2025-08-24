package ua.at.tsvetkov.nfcsdk

/**
 * Created by Alexandr Tsvetkov on 22.08.2025.
 */
enum class NfcAdminError(val message: String) {
    NFC_ADAPTER_IS_NOT_AVAILABLE("NFC Adapter is not available on this device."),
    NFC_ADAPTER_IS_NOT_ENABLED("NFC Adapter is not enabled."),
    TAG_NOT_FOUND("NFC Tag not found in intent."),
    MALFORMED_MIME_TYPE("Malformed MimeType"),
    ERROR_WHEN_TURNING_ON_FOREGROUND_DISPATCH("Error when turning on foreground dispatch"),
    ERROR_WHILE_SHUTTING_DOWN_FOREGROUND_DISPATCH("Error while shutting down foreground dispatch"),
    ERROR_CLOSING_NDEF_CONNECTION("Error closing Ndef connection"),
    ERROR_CLOSING_NDEFORMATABLE_CONNECTION("Error closing NdefFormatable connection");
}