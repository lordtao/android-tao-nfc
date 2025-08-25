package ua.at.tsvetkov.nfcsdk

/**
 * Interface for receiving callbacks about NFC tag scanning events.
 * Implement this interface to handle successful scans and errors.
 *
 * @param T The type of the successfully scanned data.
 * @param K The type or category of the scanned NFC record (e.g., NDEF record type).
 */
interface NfcScanListener<T, K> {

    /**
     * Called when an NFC tag has been successfully scanned and its data processed.
     *
     * @param result The processed data extracted from the NFC tag. The type of this data
     *               is defined by the implementing [ua.at.tsvetkov.nfcsdk.handler.NfcHandler].
     * @param type An identifier for the type of NFC record that was scanned,
     *             providing context to the [result]. This could be, for example,
     *             an NDEF record type like [android.nfc.NdefRecord.RTD_TEXT] or
     *             [android.nfc.NdefRecord.RTD_URI].
     */
    fun onNfcTagScanned(result: T, type: K)

    /**
     * Called when an error occurs during the NFC tag scanning or processing.
     *
     * @param error An [NfcError] enum value indicating the specific error that occurred.
     */
    fun onNfcScanError(error: NfcError)
}