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
    private var isAdminLogEnabled: Boolean = false,
) : NfcAdapter.ReaderCallback {

    private val nfcAdapter: NfcAdapter? by lazy { NfcAdapter.getDefaultAdapter(activity) }

    private val handlers: MutableList<NfcHandler<*,*>> = mutableListOf()

    private val nfcStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == NfcAdapter.ACTION_ADAPTER_STATE_CHANGED) {
                val state = intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE, NfcAdapter.STATE_OFF)
                nfcStateListener?.onNfcStateChanged(state)
            }
        }
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
     * @param handler The [NfcHandler] instance to add.
     */
    fun addHandler(handler: NfcHandler<*,*>) {
        handlers.add(handler)
    }

    /**
     * Removes a previously added [NfcHandler] from the list of handlers.
     *
     * @param handler The [NfcHandler] instance to remove.
     */
    fun removeHandler(handler: NfcHandler<*,*>) {
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
     * Enables NFC reader mode with a default set of flags.
     * The default flags include:
     * - [NfcAdapter.FLAG_READER_NFC_A]
     * - [NfcAdapter.FLAG_READER_NFC_B]
     * - [NfcAdapter.FLAG_READER_NFC_F]
     * - [NfcAdapter.FLAG_READER_NFC_V]
     * - [NfcAdapter.FLAG_READER_NFC_BARCODE]
     * - [NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK]
     * - [NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS]
     *
     * This method should typically be called in the `onResume` lifecycle method of an Activity.
     * When reader mode is enabled, the calling Activity has priority in handling discovered NFC tags.
     *
     * @param extras An optional [Bundle] of extra data. Can be `null`.
     *               This can be used to pass specific polling loop parameters.
     */
    fun enableReaderModeWithDefaults(extras: Bundle? = null) {
        val readerFlags = NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_NFC_BARCODE or
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK or
                NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS
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
     *              For example, [NfcAdapter.FLAG_READER_NFC_A], [NfcAdapter.FLAG_READER_ISO_DEP].
     * @param extras An optional [Bundle] of extra data. Can be `null`.
     *               This can be used to pass specific polling loop parameters, such as delay.
     * @see NfcAdapter.enableReaderMode
     */
    fun enableReaderMode(flags: Int, extras: Bundle? = null) {
        if (nfcAdapter == null) {
            if (isAdminLogEnabled) Log.w("${NfcAdminError.NFC_ADAPTER_IS_NOT_AVAILABLE}. Reader mode not enabled.")
            nfcStateListener?.onError(NfcAdminError.NFC_ADAPTER_IS_NOT_AVAILABLE)
            return
        }
        if (nfcAdapter?.isEnabled == false) {
            if (isAdminLogEnabled) Log.w("${NfcAdminError.NFC_ADAPTER_IS_NOT_ENABLED}. Reader mode not enabled.")
            nfcStateListener?.onError(NfcAdminError.NFC_ADAPTER_IS_NOT_ENABLED)
            return
        }
        try {
            nfcAdapter?.enableReaderMode(activity, this, flags, extras)
            if (isAdminLogEnabled) Log.d("Reader mode is ON.")
            nfcStateListener?.onNfcStarted()
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
            if (isAdminLogEnabled) Log.d("Reader mode is OFF.")
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

        if (isAdminLogEnabled) Log.d("Tag discovered in ReaderMode: $tag")

        activity.runOnUiThread {
            handlers
                .filter {
                    it.containsSupportedRecord(tag)
                }
                .forEach { handler ->
                    if (handler.isHavePreparedMessageToWrite()) {
                        handler.writeMessageToTag(tag)
                    } else {
                        handler.readMessageFromTag(tag)
                    }
                }
        }
    }

}
