package ua.at.tsvetkov.nfcsdk

/**
 *  Created by Alexandr Tsvetkov on 26.08.2025.
 *
 * Defines various error states that can occur during NFC operations.
 * Each error has a default message that can be used for logging or display.
 *
 * @param message A human-readable message describing the administrative error.
 */
enum class NfcError(val message: String) {
    /** Tag is not NDEF formatted and NDEF operations cannot be performed. */
    TAG_NOT_NDEF_FORMATTED("Tag is not NDEF formatted."),

    /** NDEF message is null or could not be obtained from the tag, possibly an empty tag or read issue. */
    NDEF_MESSAGE_NULL("NDEF message is null or could not be obtained from the tag."),

    /** The NFC tag is not writable (e.g., read-only or protected). */
    TAG_NOT_WRITABLE("Tag is not writable."),

    /** There is not enough space on the NFC tag to write the prepared NDEF message. */
    NOT_ENOUGH_SPACE("Not enough space on the tag."),

    /**
     * The tag is not NDEF compliant for writing operations.
     * This means Ndef.get(tag) is null and NdefFormatable.get(tag) is also null.
     */
    TAG_NOT_NDEF_COMPLIANT("Tag is not NDEF writable or formattable."),

    /** No data (e.g., text or URI) has been prepared for writing to the tag. */
    NO_DATA_TO_WRITE("No data prepared to write."),

    /** An IOException occurred during an NFC read operation. */
    IO_EXCEPTION_WHILE_READING("IOException occurred while reading from the NFC tag."),

    /** A general, non-IOException error occurred during an NFC read operation. */
    GENERAL_READ_ERROR("A general error occurred while reading from the NFC tag."),

    /** An IOException occurred during an NFC write operation. */
    IO_EXCEPTION_WHILE_WRITING("IOException occurred while writing to the NFC tag."),

    /** A general, non-IOException error occurred during an NFC write operation. */
    GENERAL_WRITE_ERROR("A general error occurred while writing to the NFC tag."),

    /** An Exception occurred during an NFC close operation. */
    CLOSE_CONNECTION_ERROR("An error occurred while closing the connection to the NFC tag.");

    /**
     * Returns the human-readable message associated with this administrative error.
     * @return The error message string.
     */
    override fun toString(): String = message
}
