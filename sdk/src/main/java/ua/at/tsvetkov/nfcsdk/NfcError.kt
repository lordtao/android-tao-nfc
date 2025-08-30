package ua.at.tsvetkov.nfcsdk

/**
 *  Created by Alexandr Tsvetkov on 26.08.2025.
 *
 * Defines various error states that can occur during NFC read or write operations.
 * Each error has a default message that can be used for logging or display.
 *
 * @param errorMsg A human-readable message describing the specific NFC error.
 */
enum class NfcError(val errorMsg: String, val type: Type) {

    /**
     * Read operation failed because the tag is not of the Mifare Ultralight type
     * or is not compatible with Mifare Ultralight commands.
     */
    READ_TAG_NOT_MIFARE_ULTRALIGHT("Tag is not Mifare Ultralight.", Type.READ_ERROR),

    /**
     * Read operation succeeded at a low level, but the tag either contains no meaningful data
     * (e.g., appears empty or uninitialized) or the data read could not be
     * interpreted as valid content by the current handler or parser.
     */
    READ_TAG_EMPTY_OR_UNREADABLE("Tag is empty or unreadable.", Type.READ_ERROR),

    /**
     * NDEF read/write operation failed because the tag is not NDEF formatted.
     * The tag might be empty or contain non-NDEF data.
     * (Primarily a read-side discovery, but can impact write if formatting is expected first)
     */
    READ_TAG_NOT_NDEF_FORMATTED(
        "Tag is not NDEF formatted.",
        Type.READ_ERROR
    ), // Может влиять и на запись, но обнаруживается при чтении

    /**
     * NDEF read operation failed because the NDEF message obtained from the tag is null.
     * This can occur if the tag is empty, not NDEF formatted, or if a read error prevented message retrieval.
     */
    READ_NDEF_MESSAGE_NULL("NDEF message is null or could not be obtained from the tag.", Type.READ_ERROR),

    /**
     * NDEF read operation succeeded, but the NDEF message on the tag is empty
     * (e.g., contains no NDEF records).
     */
    READ_NDEF_MESSAGE_EMPTY("NDEF message is empty.", Type.READ_ERROR),

    /**
     * Read or NDEF operation failed because the tag is neither NDEF writable (`Ndef.get(tag)` is null)
     * nor NDEF formattable (`NdefFormatable.get(tag)` is null).
     * Essential NDEF capabilities are missing.
     * (Can be detected during read or before write)
     */
    READ_TAG_NOT_NDEF_COMPLIANT("Tag is not NDEF writable or formattable.", Type.READ_ERROR),

    /**
     * An I/O exception occurred during a low-level NFC read operation,
     * indicating a problem communicating with the tag.
     */
    READ_IO_EXCEPTION("IOException occurred while reading from the NFC tag.", Type.READ_ERROR),

    /**
     * A general, non-IOException error occurred during an NFC read operation.
     * This covers errors not fitting more specific categories.
     */
    READ_GENERAL_ERROR("A general error occurred while reading from the NFC tag.", Type.READ_ERROR),

    /**
     * An error occurred while trying to close the connection to the NFC tag
     * after a read operation. The read itself might have been successful.
     */
    READ_CLOSE_CONNECTION_ERROR("An error occurred while closing the connection to the NFC tag.", Type.READ_ERROR),

    /**
     * Write operation failed because the tag is not writable.
     * It might be read-only, password-protected, or permanently locked.
     */
    WRITE_TAG_NOT_WRITABLE("Tag is not writable.", Type.WRITE_ERROR),

    /**
     * Write operation failed because a post-write verification step
     * (reading back the data) indicated that the written data does not match
     * the data that was intended to be written.
     */
    WRITE_VERIFICATION_ERROR("Write verification failed.", Type.WRITE_ERROR),

    /**
     * Write operation failed because there is insufficient space on the NFC tag
     * to store the prepared NDEF message or data.
     */
    WRITE_NOT_ENOUGH_SPACE("Not enough space on the tag.", Type.WRITE_ERROR),

    /**
     * Write operation failed because the **current NFC handler** does not support
     * writing to this specific tag type or technology. Use this error in your custom handlers
     * when the tag type is unsupported by the handler's write logic.
     *
     * This error indicates that while the tag might be generally writable
     * or support certain NFC technologies, the active handler responsible
     * for the write operation is not designed or configured to interact
     * with this particular tag for writing purposes.
     *
     * For example, an NDEF handler might encounter this if the tag is not NDEF-formattable,
     * or a custom handler for Mifare Classic might report this if a Mifare Ultralight tag is presented.
     */
    WRITE_TAG_NOT_COMPLIANT_FOR_THIS_HANDLER("Tag is not compliant for write with this handler.", Type.WRITE_ERROR),

    /**
     * Write or NDEF format operation failed because the tag is neither NDEF writable (`Ndef.get(tag)` is null)
     * nor NDEF formattable (`NdefFormatable.get(tag)` is null).
     * Essential NDEF capabilities for writing/formatting are missing.
     */
    WRITE_TAG_NOT_NDEF_COMPLIANT("Tag is not NDEF writable or formattable.", Type.WRITE_ERROR),

    /**
     * Write operation was initiated, but no data (e.g., text, URI, NDEF message)
     * had been prepared or provided for writing to the tag.
     */
    WRITE_NO_DATA_TO_WRITE("No data prepared to write.", Type.WRITE_ERROR),

    /**
     * An I/O exception occurred during a low-level NFC write operation,
     * indicating a problem communicating with the tag during the write attempt.
     */
    WRITE_IO_EXCEPTION("IOException occurred while writing to the NFC tag.", Type.WRITE_ERROR),

    /**
     * A general, non-IOException error occurred during an NFC write operation.
     * This covers errors not fitting more specific categories.
     */
    WRITE_GENERAL_ERROR("A general error occurred while writing to the NFC tag.", Type.WRITE_ERROR),

    /**
     * An error occurred while trying to close the connection to the NFC tag
     * after a write operation. The write itself might have been successful or failed.
     */
    WRITE_CLOSE_CONNECTION_ERROR("An error occurred while closing the connection to the NFC tag.", Type.WRITE_ERROR);

    /**
     * Returns the human-readable message associated with this error.
     * @return The error message string.
     */
    override fun toString(): String = errorMsg

    /**
     * Checks if this error instance pertains to a read operation.
     *
     * An error is considered a "read error" if its [type] is [Type.READ_ERROR].
     * This can be useful for categorizing errors or triggering specific UI feedback
     * related to reading NFC tags.
     *
     * @return `true` if this error is categorized as a read error, `false` otherwise.
     */
    fun isReadError(): Boolean = type == Type.READ_ERROR

    /**
     * Checks if this error instance pertains to a write operation.
     *
     * An error is considered a "write error" if its [type] is [Type.WRITE_ERROR].
     * This can be useful for categorizing errors or triggering specific UI feedback
     * related to writing data to NFC tags.
     *
     * @return `true` if this error is categorized as a write error, `false` otherwise.
     */
    fun isWriteError(): Boolean = type == Type.WRITE_ERROR

    /**
     * Defines categories for NFC errors, typically related to read or write operations.
     */
    enum class Type {
        /** Indicates an error that occurred during a tag read operation or preparation for reading. */
        READ_ERROR,

        /** Indicates an error that occurred during a tag write operation or preparation for writing. */
        WRITE_ERROR
    }
}
