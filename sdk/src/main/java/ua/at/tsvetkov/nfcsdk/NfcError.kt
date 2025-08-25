package ua.at.tsvetkov.nfcsdk

/**
 * Created by Alexandr Tsvetkov on 22.08.2025.
 */

/**
 * Represents various errors that can occur during NFC operations,
 * particularly related to NDEF message parsing, reading, and writing.
 * Each error type includes a descriptive message.
 */
enum class NfcError(
    /** A human-readable message describing the error. */
    val message: String,
) {
    /** Indicates an error occurred while parsing an NDEF Text Record. */
    ERROR_PARSING_NDEF_TEXT_RECORD("Error parsing NDEF text record"),

    /** Indicates an error occurred while parsing an NDEF URI Record. */
    ERROR_PARSING_NDEF_URI_RECORD("Error parsing NDEF URI record"), // Added based on previous discussions

    /** Indicates that the encountered NDEF record type is not supported by the current handler. */
    UNSUPPORTED_NDFE_RECORD_TYPE("This handler does not support this NDEF record type."),

    /** Indicates that the tag is NDEF formattable but currently contains no NDEF message. */
    NDEF_FORMATTABLE_BUT_EMPTY("Tag is NDEF formattable but contains no NDEF message."),

    /** Indicates that the tag does not support NDEF technology. */
    NDEF_NOT_SUPPORTED("Tag does not support NDEF."),

    /** Indicates that there is no NDEF message present on the tag (the message is null). */
    NO_NDEF_MESSAGE("No NDEF message on the tag (null)."),

    /** Indicates that an NDEF message was found, but it contains no NDEF records. */
    NO_NDEF_RECORDS("NDEF message contains no records."),

    /** Indicates a general I/O error occurred during NFC read or write operations. */
    NFC_IO_ERROR("Error reading/writing NFC (IO exception)."),

    /** Indicates a format exception occurred during NFC operations (e.g., malformed NDEF message). */
    NFC_FORMAT_ERROR("NFC format exception."),

    /** Indicates an unknown or unspecified error occurred while reading from an NFC tag. */
    UNKNOWN_READ_ERROR("Unknown error reading NFC."),

    /** Indicates that the NFC tag is read-only and cannot be written to. */
    TAG_READ_ONLY("Tag is not writable (read-only)."),

    /** Indicates that the data to be written exceeds the maximum storage capacity of the NFC tag. */
    DATA_TOO_LARGE("Data size exceeds maximum tag size."),

    /** Indicates an I/O error occurred specifically during an NFC write operation. */
    NFC_WRITE_IO_ERROR("Error writing NFC (IO exception)."),

    /** Indicates a format exception occurred specifically during an NFC write operation. */
    NFC_WRITE_FORMAT_ERROR("Format exception when writing NFC."),

    /** Indicates an unknown or unspecified error occurred while writing to an NFC tag. */
    UNKNOWN_WRITE_ERROR("Unknown error writing NFC."),

    /** Indicates an I/O error occurred while formatting a tag and then writing to it. */
    NFC_FORMATTABLE_WRITE_IO_ERROR("Error formatting/writing NFC (IO exception)."),

    /** Indicates a format exception occurred while formatting a tag and then writing to it. */
    NFC_FORMATTABLE_FORMAT_ERROR("Format exception when formatting/writing NFC."),

    /** Indicates an unknown error occurred while formatting a tag and then writing to it. */
    UNKNOWN_FORMATTABLE_ERROR("Unknown error formatting/writing NFC."),

    /** Indicates that the tag does not support NDEF and also cannot be formatted to NDEF. */
    TAG_NOT_NDEF_COMPATIBLE("Tag does not support NDEF and cannot be formatted."),

    /** Indicates an error occurred while trying to close an Ndef connection. */
    ERROR_CLOSING_NDEF_CONNECTION("Error closing Ndef connection"),

    /** Indicates an error occurred while trying to close an NdefFormatable connection. */
    ERROR_CLOSING_NDEFORMATABLE_CONNECTION("Error closing NdefFormatable connection"),

    /** Indicates that there is no message prepared for writing. */
    NO_MESSAGE_TO_WRITE("No message has been prepared for writing to the tag."), // Added based on NdefWellKnownTextHandler
    ;

    /**
     * Returns the human-readable message associated with this error.
     * @return The error message string.
     */
    override fun toString(): String = message
}
