package ua.at.tsvetkov.nfcsdk.handler

import android.nfc.Tag
import ua.at.tsvetkov.nfcsdk.NfcError
import ua.at.tsvetkov.nfcsdk.NfcScanListener
import ua.at.tsvetkov.nfcsdk.NfcWriteListener
import ua.at.tsvetkov.nfcsdk.parser.NfcDataParser
import ua.at.tsvetkov.nfcsdk.preparer.NfcDataPreparer

/**
 * An abstract base class for handling NFC tag interactions, including reading,
 * writing, and data transformation.
 *
 * This class provides a common structure for specific NFC technology or data type handlers.
 * It uses a [NfcDataParser] to convert raw tag data into a structured application format
 * and an [NfcDataPreparer] to convert structured application data into a format suitable
 * for writing to a tag. It also manages listeners for scan and write events.
 *
 * @param D The low-level data type that is directly written to or read from the NFC tag's
 *          technology (e.g., `android.nfc.NdefMessage` for NDEF tags, `ByteArray` for raw
 *          transceive operations). This is the type that [preparer] produces and [reader] consumes.
 * @param R The high-level, structured data type that the application consumes or produces
 *          (e.g., `String` for text content, `android.net.Uri` for URIs, or a custom data model).
 *          This is the type that [reader] produces (as a list) and [preparer] consumes (as a list).
 *          Scan results are delivered as this type via [NfcScanListener].
 *
 * @property reader The [NfcDataParser] responsible for parsing raw data
 * of type [D] from the tag into a list of structured data of type [R].
 * @property preparer The [NfcDataPreparer] responsible for preparing a
 * list of structured data of type [R] into the raw data format [D] for writing to the tag.
 * @property nfcScanListener An optional [NfcScanListener] to receive callbacks for NFC scan events,
 * including successfully scanned data or errors.
 * @property nfcWriteListener An optional [NfcWriteListener] to receive callbacks for NFC write events,
 * indicating success or failure.
 */
abstract class NfcHandler<D, R>(
    var reader: NfcDataParser<D, R>,
    var preparer: NfcDataPreparer<R, D>,
    var nfcScanListener: NfcScanListener<R>? = null,
    var nfcWriteListener: NfcWriteListener? = null
) {
    /**
     * Stores the data (in its low-level format [D]) that has been prepared by
     * [prepareToWrite] and is ready to be written to an NFC tag by [writeDataToTag].
     * Subclasses implementing [prepareToWrite] are responsible for populating this field
     * using the provided [preparer].
     */
    protected var preparedData: D? = null

    /**
     * Prepare a list of structured data items for writing to an NFC tag.
     *
     * Concrete implementations should take the list of application-level data [data] of type [R],
     * use the [preparer] to convert it into the low-level format [D], and store this
     * result in the [preparedData] field.
     *
     * @param data A [List] of structured data items of type [R] to be prepared for writing.
     */
    fun prepareToWrite(data: List<R>) {
        this.preparedData = preparer.prepare(data)
    }

    /**
     * Checks if there is data prepared and ready to be written to an NFC tag.
     * This typically checks if [preparedData] is not null.
     *
     * @return `true` if there is data prepared for writing, `false` otherwise.
     */
    fun isHavePreparedDataToWrite(): Boolean = preparedData != null

    /**
     * Propagates a scan error to the [nfcScanListener], if one is set.
     *
     * @param error The [NfcError] that occurred during scanning.
     * @param throwable An optional [Throwable] that caused or is related to the error.
     */
    fun onScanError(error: NfcError, throwable: Throwable? = null) {
        nfcScanListener?.onNfcScanError(error, throwable)
    }

    /**
     * Propagates a write error to the [nfcWriteListener], if one is set.
     *
     * @param error The [NfcError] that occurred during writing.
     * @param throwable An optional [Throwable] that caused or is related to the error.
     */
    fun onWriteError(error: NfcError, throwable: Throwable? = null) {
        nfcWriteListener?.onNfcWriteError(error, throwable)
    }

    /**
     * Abstract method to read and process data from the given NFC [Tag].
     *
     * Concrete implementations should handle the specifics of interacting with the tag's
     * technology, retrieve raw data, use the [reader] to parse it into structured data of type [R],
     * and then typically communicate the results (or errors) via the [nfcScanListener].
     *
     * @param tag The NFC [Tag] object discovered and to be read from.
     */
    abstract fun readDataFromTag(tag: Tag)

    /**
     * Abstract method to write the prepared data to the given NFC [Tag].
     *
     * Concrete implementations should take the data stored in [preparedData] (which should have
     * been populated by [prepareToWrite]) and write it to the tag using the appropriate
     * NFC technology. Results or errors should be communicated via the [nfcWriteListener].
     *
     * @param tag The NFC [Tag] object to write data to.
     */
    abstract fun writeDataToTag(tag: Tag)
}
