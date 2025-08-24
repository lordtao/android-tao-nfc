package ua.at.tsvetkov.nfcsdk

/**
 * Created by Alexandr Tsvetkov on 22.08.2025.
 */
enum class NfcError(val message: String) {
    MALFORMED_MIME_TYPE("Malformed MimeType"),
    ERROR_WHEN_TURNING_ON_FOREGROUND_DISPATCH("Error when turning on foreground dispatch"),
    ERROR_WHILE_SHUTTING_DOWN_FOREGROUND_DISPATCH("Error while shutting down foreground dispatch"),
    ERROR_CLOSING_NDEF_CONNECTION("Error closing Ndef connection"),
    ERROR_CLOSING_NDEFORMATABLE_CONNECTION("Error closing NdefFormatable connection"),

    NFC_ADAPTER_IS_NOT_AVAILABLE("NFC Adapter is not available on this device."),
    NFC_ADAPTER_IS_NOT_ENABLED("NFC Adapter is not enabled."),
    EMPTY_DATA("Data to write cannot be empty."),
    TAG_NOT_FOUND("NFC Tag not found in intent."),
    UNKNOWN_NFC_ACTION("Unknown NFC action."),
    NDEF_FORMATTABLE_BUT_EMPTY("Tag is NDEF formattable but contains no NDEF message."),
    NDEF_NOT_SUPPORTED("Tag does not support NDEF."),
    NO_NDEF_MESSAGE("No NDEF message on the tag (null)."),
    NO_NDEF_RECORDS("NDEF message contains no records."),
    CANNOT_EXTRACT_DATA("Could not extract data from NDEF message."),
    NFC_IO_ERROR("Error reading/writing NFC (IO exception)."),
    NFC_FORMAT_ERROR("NFC format exception."),
    UNKNOWN_READ_ERROR("Unknown error reading NFC."),
    TAG_READ_ONLY("Tag is not writable (read-only)."),
    DATA_TOO_LARGE("Data size exceeds maximum tag size."),
    NFC_WRITE_IO_ERROR("Error writing NFC (IO exception)."),
    NFC_WRITE_FORMAT_ERROR("Format exception when writing NFC."),
    UNKNOWN_WRITE_ERROR("Unknown error writing NFC."),
    NFC_FORMATTABLE_WRITE_IO_ERROR("Error formatting/writing NFC (IO exception)."),
    NFC_FORMATTABLE_FORMAT_ERROR("Format exception when formatting/writing NFC."),
    UNKNOWN_FORMATTABLE_ERROR("Unknown error formatting/writing NFC."),
    TAG_NOT_NDEF_COMPATIBLE("Tag does not support NDEF and cannot be formatted.");
}