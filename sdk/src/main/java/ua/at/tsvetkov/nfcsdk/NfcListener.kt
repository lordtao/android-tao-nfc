package ua.at.tsvetkov.nfcsdk

/**
 * Created by Alexandr Tsvetkov on 26.08.2025.
 *
 * Interface for receiving callbacks about NFC tag scanning events.
 * Implement this interface to handle successful scans and errors.
 *
 * @param T The type of the successfully scanned data.
 */
interface NfcListener<T> {
    /**
     * Called when an NFC tag has been successfully scanned and its data processed.
     *
     * @param result The processed data extracted from the NFC tag. The type of this data
     *               is defined by the implementing [ua.at.tsvetkov.nfcsdk.handler.NfcHandler].
     */
    fun onRead(result: List<T>)

    /**
     * Called when data has been successfully written to the NFC tag.
     */
    fun onWriteSuccess()

    /**
     * Called when an event/error occurs during the NFC tag scanning or processing.
     *
     * @param nfcError An [NfcError] enum value indicating the specific event/error that occurred.
     * @param throwable A [Throwable] that occurred.
     */
    fun onError(nfcError: NfcError, throwable: Throwable? = null)
}
