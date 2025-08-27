package ua.at.tsvetkov.nfcsdk

/**
 *  Created by Alexandr Tsvetkov on 26.08.2025.
 *
 * Defines various states that can occur during NFC operations.
 * Each state has a default message that can be used for logging or display.
 *
 * @param message A human-readable message describing the administrative error.
 */
enum class NfcMessage(val message: String) {

    TAG_NOT_MIFARE_ULTRALIGHT("Tag is not Mifare Ultralight."),
    TAG_EMPTY_OR_UNREADABLE("Tag is empty or unreadable."),
    PARSING_ERROR("Error parsing data."),
    WRITE_VERIFICATION_ERROR("Write verification failed."),
    PREPARE_DATA_ERROR("Error preparing data."),

    /** TAG find and start handling (reading, writing)**/
    START_HANDLING("Start Tag handling (reading, writing)."),

    /** Tag is not NDEF formatted (May be EMPTY) and NDEF operations cannot be performed. */
    TAG_NOT_NDEF_FORMATTED("Tag is not NDEF formatted."),

    /** NDEF message is null or could not be obtained from the tag, possibly an empty tag or read issue. */
    NDEF_MESSAGE_NULL("NDEF message is null or could not be obtained from the tag."),

    /** NDEF message is empty tag. */
    NDEF_MESSAGE_EMPTY("NDEF message is empty."),

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
    READ_GENERAL_ERROR("A general error occurred while reading from the NFC tag."),

    /** An IOException occurred during an NFC write operation. */
    IO_EXCEPTION_WHILE_WRITING("IOException occurred while writing to the NFC tag."),

    /** A general, non-IOException error occurred during an NFC write operation. */
    WRITE_GENERAL_ERROR("A general error occurred while writing to the NFC tag."),

    /** An Exception occurred during an NFC close operation. */
    CLOSE_CONNECTION_ERROR("An error occurred while closing the connection to the NFC tag.");

    /**
     * Returns the human-readable message associated with this administrative error.
     * @return The error message string.
     */
    override fun toString(): String = message
}
