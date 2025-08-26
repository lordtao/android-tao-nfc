package ua.at.tsvetkov.nfcsdk.preparer

/**
 * Created by Alexandr Tsvetkov on 26.08.2025.
 *
 * Defines a generic contract for preparing a list of data items
 * into a single, consolidated representation suitable for NFC operations,
 * such as writing to an NFC tag.
 *
 * This interface is intended to be implemented by classes that can take multiple
 * pieces of structured data and convert them into a format like an `NdefMessage`
 * or a raw byte array for low-level tag communication.
 *
 * @param D The type of the individual data items in the input list.
 *          This could be, for example, `Uri`, `String` (for text), or a custom model class.
 * @param R The type of the resulting prepared data.
 *          This is typically a format ready for NFC interaction, e.g., `android.nfc.NdefMessage`
 *          or `ByteArray`.
 */
interface NfcDataPreparer<D, R> {

    /**
     * Prepares a list of data items of type [D] into a single result of type [R].
     *
     * Implementations should process the input `data` list and aggregate or
     * convert it into the target result format [R]. For example, this might involve
     * creating multiple `NdefRecord` instances from the input list and then
     * packaging them into a single `NdefMessage`.
     *
     * If the input data cannot be prepared (e.g., due to invalid items or an
     * inability to represent them in the target format), this method might
     * throw an exception or return a representation indicating failure,
     * depending on the implementer's error handling strategy.
     *
     * @param data A [List] of data items of type [D] to be prepared.
     * @return The prepared data of type [R], ready for an NFC operation.
     */
    fun prepare(data: List<D>): R
}
