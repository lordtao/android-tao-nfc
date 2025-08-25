package ua.at.tsvetkov.nfcsdk.handler

import android.nfc.Tag
import ua.at.tsvetkov.nfcsdk.NfcError
import ua.at.tsvetkov.nfcsdk.NfcScanListener
import ua.at.tsvetkov.nfcsdk.NfcWriteListener

/**
 * Abstract base class for handling NFC tag interactions (reading and writing).
 * Concrete implementations will define how to parse and prepare specific types of data.
 *
 * @param T The type of data this handler reads from an NFC tag (e.g., String for text, Uri for URI).
 * @param K The type of record identifier associated with the data (e.g., ByteArray for NDEF record types).
 * @property nfcScanListener Optional listener to be notified of scan events (success or error).
 * @property nfcWriteListener Optional listener to be notified of write events (success or error).
 */
abstract class NfcHandler<T, K>(
    protected var nfcScanListener: NfcScanListener<T, K>? = null,
    protected var nfcWriteListener: NfcWriteListener? = null,
) {
    /**
     * A list of NFC technologies (e.g., `android.nfc.tech.Ndef`, `android.nfc.tech.IsoDep`)
     * that this handler supports. This can be used to filter tags or determine compatibility.
     */
    abstract val techList: List<String>

    /**
     * Propagates a scan error to the [nfcScanListener], if one is set.
     *
     * @param error The [NfcError] that occurred during scanning.
     */
    fun onScanError(error: NfcError) {
        nfcScanListener?.onNfcScanError(error)
    }

    /**
     * Propagates a write error to the [nfcWriteListener], if one is set.
     *
     * @param error The [NfcError] that occurred during writing.
     */
    fun onWriteError(error: NfcError) {
        nfcWriteListener?.onNfcWriteError(error)
    }

    /**
     * Checks if the provided NFC [Tag] supports any of the technologies
     * listed in this handler's [techList].
     *
     * @param tag The NFC [Tag] to check.
     * @return `true` if the tag supports at least one technology from [techList],
     * `false` otherwise or if the tag is null.
     */
    fun containsSupportedRecord(tag: Tag?): Boolean {
        tag
            ?.techList
            ?.firstOrNull { tech ->
                techList.contains(tech)
            }?.let {
                return true
            }
        return false
    }

    /**
     * Checks if there is a prepared message ready to be written to an NFC tag.
     *
     * @return `true` if there is data prepared for writing, `false` otherwise.
     */
    abstract fun isHavePreparedMessageToWrite(): Boolean

    /**
     * Reads and processes data from the given NFC [Tag].
     * The specific parsing logic is implemented in concrete subclasses.
     * Results are typically communicated via the [nfcScanListener].
     *
     * @param tag The NFC [Tag] to read from.
     */
    abstract fun readMessageFromTag(tag: Tag)

    /**
     * Writes the prepared data (set via [prepareToWrite]) to the given NFC [Tag].
     * The specific writing logic is implemented in concrete subclasses.
     * Results are typically communicated via the [nfcWriteListener].
     *
     * @param tag The NFC [Tag] to write to.
     */
    abstract fun writeMessageToTag(tag: Tag)

    /**
     * Prepares data for writing to an NFC tag.
     * This method must be called before the tag is brought near the device for writing.
     * The prepared data is typically stored internally by the handler until [writeMessageToTag] is called.
     *
     * @param data The data of type [T] to be written to the tag.
     */
    abstract fun prepareToWrite(data: T)
}
