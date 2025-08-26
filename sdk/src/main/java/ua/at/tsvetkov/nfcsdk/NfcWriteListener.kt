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
    fun onNfcWriteSuccess()

    /**
     * Called when an error occurs during the NFC tag writing process.
     *
     * @param error An [NfcError] enum value indicating the specific error
     *              that occurred during the write operation.
     */
    fun onNfcWriteError(error: NfcError, throwable: Throwable? = null)
}
