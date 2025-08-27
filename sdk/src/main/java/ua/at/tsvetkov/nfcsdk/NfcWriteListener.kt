package ua.at.tsvetkov.nfcsdk

/**
 * Created by Alexandr Tsvetkov on 22.08.2025.
 *
 * Interface for receiving callbacks about NFC tag writing operations.
 * Implement this listener to handle the results of attempting to write
 * data to an NFC tag.
 */
interface NfcWriteListener {
    /**
     * Called when data has been successfully written to the NFC tag.
     */
    fun onWritten()

    /**
     * Called when an event/error occurs during the NFC tag writing process.
     *
     * @param message An [NfcMessage] enum value indicating the specific event/error
     *              that occurred during the write operation.
     */
    fun onWriteEvent(message: NfcMessage, throwable: Throwable? = null)
}
