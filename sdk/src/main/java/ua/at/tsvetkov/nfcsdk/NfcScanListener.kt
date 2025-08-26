package ua.at.tsvetkov.nfcsdk

/**
 * Created by Alexandr Tsvetkov on 26.08.2025.
 *
 * Interface for receiving callbacks about NFC tag scanning events.
 * Implement this interface to handle successful scans and errors.
 *
 * @param T The type of the successfully scanned data.
 */
interface NfcScanListener<T> {
    /**
     * Called when an NFC tag has been successfully scanned and its data processed.
     *
     * @param result The processed data extracted from the NFC tag. The type of this data
     *               is defined by the implementing [ua.at.tsvetkov.nfcsdk.handler.NfcHandler].
     */
    fun onNfcTagScanned(result: List<T>)

    /**
     * Called when an error occurs during the NFC tag scanning or processing.
     *
     * @param error An [NfcError] enum value indicating the specific error that occurred.
     * @param throwable A [Throwable] that occurred.
     */
    fun onNfcScanError(error: NfcError, throwable: Throwable? = null)
}
