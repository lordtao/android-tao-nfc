package ua.at.tsvetkov.nfcsdk

/**
 * Created by Alexandr Tsvetkov on 22.08.2025.
 */
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import ua.at.tsvetkov.nfcsdk.handler.NfcHandler
import ua.at.tsvetkov.util.logger.Log

/**
 * Manages NFC interactions, including reading and writing NFC tags.
 *
 * This class simplifies NFC operations by handling adapter states, reader mode,
 * and tag discovery. It uses a list of [NfcHandler] instances to process
 * different types of NFC tag content.
 *
 * @param activity The current [Activity] context, required for NFC operations
 *                 such as foreground dispatch and reader mode.
 * @param nfcStateListener An optional listener to receive callbacks about NFC adapter
 *                         state changes (e.g., enabled, disabled) and errors.
 * @param isAdminLogEnabled If `true`, enables detailed logging for debugging purposes.
 *                          Defaults to `false`.
 */
class NfcAdmin(
    private val activity: Activity,
    private var nfcStateListener: NfcStateListener? = null,
    private var isAdminLogEnabled: Boolean = false
) : NfcAdapter.ReaderCallback {
    private val nfcAdapter: NfcAdapter? by lazy { NfcAdapter.getDefaultAdapter(activity) }

    private val handlers: MutableList<NfcHandler<*, *>> = mutableListOf()

    private var currentTechs: List<String> = listOf()

    private val nfcStateReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == NfcAdapter.ACTION_ADAPTER_STATE_CHANGED) {
                    val nfcAdapterState = intent.getIntExtra(
                        NfcAdapter.EXTRA_ADAPTER_STATE,
                        NfcAdapter.STATE_OFF
                    )
                    val state = when (nfcAdapterState) {
                        NfcAdapter.STATE_ON -> NfcAdminState.NfcOn
                        NfcAdapter.STATE_OFF -> NfcAdminState.NfcOff
                        NfcAdapter.STATE_TURNING_ON -> NfcAdminState.NfcTurningOn
                        NfcAdapter.STATE_TURNING_OFF -> NfcAdminState.NfcTurningOff
                        else -> NfcAdminState.NfcUndefined
                    }
                    nfcStateListener?.onNfcStateChanged(state)
                }
            }
        }

    /**
     * Gets the list of NFC technologies from the last processed tag.
     *
     * Each string is a fully qualified class name (e.g., "android.nfc.tech.NfcA").
     *
     * @return [List] of technology class names, or an empty list if none are set.
     */
    fun getCurrentTechs(): List<String> = currentTechs

    /**
     * Sets the list of NFC technologies for the currently processed tag.
     *
     * Typically called after discovering a tag and getting its technologies
     * via [android.nfc.Tag.getTechList].
     *
     * @param currentTechs A [List] of fully qualified NFC technology class names.
     */
    fun setCurrentTechs(currentTechs: List<String>) {
        this.currentTechs = currentTechs
    }

    /**
     * Checks if the device has NFC hardware.
     *
     * @return `true` if NFC hardware is available, `false` otherwise.
     */
    fun isNfcAvailable(): Boolean = nfcAdapter != null

    /**
     * Checks if NFC is enabled on the device.
     * For this to be `true`, NFC hardware must also be available.
     *
     * @return `true` if NFC is enabled, `false` otherwise.
     */
    fun isNfcEnabled(): Boolean = nfcAdapter?.isEnabled == true

    /**
     * Adds an [NfcHandler] to the list of handlers that will process discovered NFC tags.
     * Each handler can be responsible for specific types of NFC data.
     *
     * @param handlers The [NfcHandler] instances to add.
     */
    fun addHandlers(vararg handlers: NfcHandler<*, *>) {
        handlers.forEach { this.handlers.add(it) }
    }

    /**
     * Removes a previously added [NfcHandler] from the list of handlers.
     *
     * @param handler The [NfcHandler] instance to remove.
     */
    fun removeHandler(handler: NfcHandler<*, *>) {
        handlers.remove(handler)
    }

    /**
     * Registers a [BroadcastReceiver] to listen for changes in the NFC adapter's state
     * (e.g., when NFC is turned on or off in the device settings).
     * This method should typically be called in the `onResume` lifecycle method of an Activity.
     */
    fun registerNfcStateReceiver() {
        val filter = IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
        activity.registerReceiver(nfcStateReceiver, filter)
    }

    /**
     * Unregisters the [BroadcastReceiver] that listens for NFC adapter state changes.
     * This method should typically be called in the `onPause` lifecycle method of an Activity
     * to prevent memory leaks.
     */
    fun unregisterNfcStateReceiver() {
        activity.unregisterReceiver(nfcStateReceiver)
    }

    /**
     * Enables NFC reader mode with a predefined set of commonly used flags.
     *
     * This method simplifies the process of enabling reader mode by defaulting to a
     * comprehensive set of NFC technologies and options. The default flags include:
     * - [NfcAdapter.FLAG_READER_NFC_A]
     * - [NfcAdapter.FLAG_READER_NFC_B]
     * - [NfcAdapter.FLAG_READER_NFC_F]
     * - [NfcAdapter.FLAG_READER_NFC_V]
     * - [NfcAdapter.FLAG_READER_NFC_BARCODE]
     * - [NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK]
     *
     * Reader mode gives the calling Activity priority in handling discovered NFC tags.
     * It is typically called in the `onResume()` lifecycle method of an Activity and
     * paired with a call to [disableReaderMode] in `onPause()`.
     *
     * This method internally calls [enableReaderMode] with the configured flags.
     *
     * @param isOnSound If `false` (default), [NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS]
     *                  will be added to the flags, muting the default NFC discovery sound.
     *                  If `true`, the platform's default NFC sound will play upon tag discovery.
     * @param extras An optional [Bundle] of extra data to be passed to the underlying
     *               [NfcAdapter.enableReaderMode] call. This can be used to specify
     *               additional parameters for the NFC polling loop, such as a delay.
     *               Defaults to `null`.
     * @see enableReaderMode
     * @see NfcAdapter.enableReaderMode
     */
    fun enableReaderModeWithDefaults(isOnSound: Boolean = false, extras: Bundle? = null) {
        val baseFlags =
            NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_NFC_BARCODE

        val readerFlags: Int =
            baseFlags or if (!isOnSound) NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS else 0
        enableReaderMode(readerFlags, extras)
    }

    /**
     * Enables NFC reader mode for the current Activity.
     *
     * When reader mode is enabled, the specified Activity will receive NFC tag discovery events
     * through the [NfcAdapter.ReaderCallback] interface (which this class implements).
     * This mode gives the foreground Activity priority in handling NFC tags, and generally
     * prevents other apps from interfering.
     *
     * This method should typically be called in the `onResume` lifecycle method of an Activity.
     *
     * @param flags A bitmask of flags indicating the NFC technologies to poll for.
     *              For example, [NfcAdapter.FLAG_READER_NFC_A], [NfcAdapter.FLAG_READER_NFC_B].
     * @param extras An optional [Bundle] of extra data. Can be `null`.
     *               This can be used to pass specific polling loop parameters, such as delay.
     * @see NfcAdapter.enableReaderMode
     */
    fun enableReaderMode(flags: Int, extras: Bundle? = null) {
        if (nfcAdapter == null) {
            if (isAdminLogEnabled) {
                Log.w("NFC Adapter is not available on this device. Reader mode not enabled.")
            }
            nfcStateListener?.onNfcStateChanged(NfcAdminState.NfcNotAvailable)
            return
        }
        if (nfcAdapter?.isEnabled == false) {
            if (isAdminLogEnabled) {
                Log.w("NFC Adapter is not enabled. Reader mode not enabled.")
            }
            nfcStateListener?.onNfcStateChanged(NfcAdminState.NfcOff)
            return
        }
        try {
            nfcAdapter?.enableReaderMode(activity, this, flags, extras)
            nfcStateListener?.onNfcStateChanged(NfcAdminState.NfcOn)
            if (isAdminLogEnabled) {
                Log.i("NFC Adapter is enabled. Reader mode enabled.")
            }
        } catch (e: Exception) {
            if (isAdminLogEnabled) {
                Log.e("${NfcAdminError.ERROR_WHEN_TURNING_ON_READER_MODE}: ${e.message}", e)
            }
            nfcStateListener?.onError(NfcAdminError.ERROR_WHEN_TURNING_ON_READER_MODE)
        }
    }

    /**
     * Disables NFC reader mode for the current Activity.
     *
     * This method should typically be called in the `onPause` lifecycle method of an Activity
     * to release NFC resources and allow other applications to handle NFC events.
     *
     * @see NfcAdapter.disableReaderMode
     */
    fun disableReaderMode() {
        if (nfcAdapter == null) {
            return
        }
        try {
            nfcAdapter?.disableReaderMode(activity)
            nfcStateListener?.onNfcStateChanged(NfcAdminState.NfcOff)
        } catch (e: Exception) {
            if (isAdminLogEnabled) {
                Log.e("${NfcAdminError.ERROR_WHILE_SHUTTING_DOWN_READER_MODE}: ${e.message}", e)
            }
            nfcStateListener?.onError(NfcAdminError.ERROR_WHILE_SHUTTING_DOWN_READER_MODE)
        }
    }

    /**
     * Callback method from [NfcAdapter.ReaderCallback].
     * This method is invoked when an NFC tag is discovered while reader mode is active.
     *
     * The discovered tag is then passed to registered [NfcHandler] instances for processing.
     * Processing is performed on the UI thread.
     *
     * @param tag The discovered [Tag] object. Can be `null` if an error occurred during discovery.
     */
    override fun onTagDiscovered(tag: Tag?) {
        if (tag == null) {
            if (isAdminLogEnabled) Log.w(NfcAdminError.TAG_NOT_FOUND.message)
            nfcStateListener?.onError(NfcAdminError.TAG_NOT_FOUND)
            return
        }

        if (isAdminLogEnabled) Log.d("Tag discovered: $tag")
        currentTechs = tag.techList.asList()

        activity.runOnUiThread {
            handlers
                .forEach { handler ->
                    handler.setCurrentTechs(currentTechs)
                    handler.onScanEvent(NfcMessage.START_HANDLING)
                    if (handler.isHavePreparedDataToWrite()) {
                        handler.writeDataToTag(tag)
                    } else {
                        handler.readDataFromTag(tag)
                    }
                }
        }
    }
}
