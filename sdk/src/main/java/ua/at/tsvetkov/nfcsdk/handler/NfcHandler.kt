package ua.at.tsvetkov.nfcsdk.handler

import android.nfc.Tag
import ua.at.tsvetkov.nfcsdk.NfcError
import ua.at.tsvetkov.nfcsdk.NfcListener
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
 *          Scan results are delivered as this type via [NfcListener].
 *
 * @property parser The [NfcDataParser] responsible for parsing raw data
 * of type [D] from the tag into a list of structured data of type [R].
 * @property preparer The [NfcDataPreparer] responsible for preparing a
 * list of structured data of type [R] into the raw data format [D] for writing to the tag.
 * @property nfcListener A [NfcListener] to receive callbacks for NFC read/write errors.
 */
abstract class NfcHandler<D, R>(
    var parser: NfcDataParser<D, R>,
    var preparer: NfcDataPreparer<R, D>,
    var nfcListener: NfcListener<R>,
) {
    
    /**
     * Controls whether this NFC handler is currently active and should process NFC events.
     *
     * If set to `false`, the handler might be skipped or ignore incoming NFC tag interactions,
     * even if its [supportedTechs] match the discovered tag. This allows for dynamically
     * enabling or disabling specific handlers without removing them from a list of available handlers.
     * Defaults to `true`.
     */
    var isEnabled = true

    /**
     * Flag indicating whether logging is enabled. Use this flag in your custom handler as you see fit.
     * */
    var isLogEnabled = false

    lateinit var tag: Tag

    /**
     * Stores the data (in its low-level format [D]) that has been prepared by
     * [prepareToWrite] and is ready to be written to an NFC tag by [writeDataToTag].
     * Subclasses implementing [prepareToWrite] are responsible for populating this field
     * using the provided [preparer].
     */
    var preparedData: D? = null

    /**
     * Prepares the data required to "clears" or "erases" existing content on a tag.
     */
    abstract fun prepareCleaningData()

    /**
     * A list of NFC technology class names that this specific handler implementation
     * is designed to work with.
     *
     * For example, a handler for NDEF data might declare
     * `listOf("[android.nfc.tech.Ndef]", "[android.nfc.tech.NdefFormatable]")`.
     * This list is used by [isSupportTech] to determine if this handler can process
     * a discovered NFC tag based on the technologies available on that tag.
     *
     * Concrete implementations of [NfcHandler] must override this property to
     * specify which technologies they support.
     */
    abstract val supportedTechs: List<String>

    /**
     * Checks if this handler supports at least one of the NFC technologies
     * present on a discovered tag.
     *
     * It compares the technologies provided in the [tech] parameter (typically
     * obtained from `tag.getTechList()`) against the handler's own
     * [supportedTechs] list.
     *
     * @param tech A list of strings, where each string is a fully qualified
     * class name of an NFC technology discovered on a tag
     * (e.g., "[android.nfc.tech.NfcA]", "[android.nfc.tech.Ndef]").
     * @return `true` if there is at least one common technology between
     * the [tech] parameter and this handler's [supportedTechs],
     * `false` otherwise.
     */
    fun isSupportTech(tech: List<String>): Boolean = supportedTechs.any { it in tech }

    /**
     * Resets the data previously staged for an NFC tag write operation.
     * Call this after a write or if the prepared data is no longer valid.
     */
    fun clearPreparedData() {
        preparedData = null
    }

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
     * Propagates a error to the [nfcListener], if one is set.
     *
     * @param message The [NfcError] that occurred during scanning/writing.
     * @param throwable An optional [Throwable] that caused or is related to the error.
     */
    fun onError(message: NfcError, throwable: Throwable? = null) {
        nfcListener.onError(message, throwable)
    }

    /**
     * Abstract method to read and process data from the stored NFC [Tag].
     *
     * Concrete implementations should handle the specifics of interacting with the tag's
     * technology, retrieve raw data, use the [parser] to parse it into structured data of type [R],
     * and then typically communicate the results (or errors) via the [nfcListener].
     */
    abstract fun readDataFromTag()

    /**
     * Abstract method to write the prepared data to the stored NFC [Tag].
     *
     * Concrete implementations should take the data stored in [preparedData] (which should have
     * been populated by [prepareToWrite]) and write it to the tag using the appropriate
     * NFC technology. Results or errors should be communicated via the [nfcListener].
     *
     */
    abstract fun writeDataToTag()
}
