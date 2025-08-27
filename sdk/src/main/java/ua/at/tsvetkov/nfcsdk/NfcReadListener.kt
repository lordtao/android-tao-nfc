package ua.at.tsvetkov.nfcsdk

/**
 * Created by Alexandr Tsvetkov on 26.08.2025.
 *
 * Interface for receiving callbacks about NFC tag scanning events.
 * Implement this interface to handle successful scans and errors.
 *
 * @param T The type of the successfully scanned data.
 */
interface NfcReadListener<T> {
    /**
     * Called when an NFC tag has been successfully scanned and its data processed.
     *
     * @param result The processed data extracted from the NFC tag. The type of this data
     *               is defined by the implementing [ua.at.tsvetkov.nfcsdk.handler.NfcHandler].
     */
    fun onRead(result: List<T>)

    /**
     * Called when an event/error occurs during the NFC tag scanning or processing.
     *
     * @param message An [NfcMessage] enum value indicating the specific event/error that occurred.
     * @param throwable A [Throwable] that occurred.
     */
    fun onReadEvent(message: NfcMessage, throwable: Throwable? = null)
}
