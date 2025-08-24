package ua.at.tsvetkov.nfcsdk

/**
 * Created by Alexandr Tsvetkov on 22.08.2025.
 */
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.FormatException
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.Build
import ua.at.tsvetkov.nfcsdk.handler.NfcHandler
import ua.at.tsvetkov.util.logger.Log
import java.io.IOException

/**
 * Class for managing reading and writing NFC tags.
 *
 * @param activity Activity that will be used for NFC interactions. Required for Foreground Dispatch System and BroadcastReceiver.
 * @param nfcStateListener Listener to receive NFC state.
 * @param nfcIntentFilters Additionally you can add intent filters like ACTION_TAG_DISCOVERED and ACTION_TECH_DISCOVERED
 *                 to handle specific types of tags.
 *                 These can be added if non-NDEF tags or tags with specific technologies need to be processed.
 */
class NfcAdmin(
    private val activity: Activity,
    private val nfcIntentFilters: Array<IntentFilter> = emptyArray<IntentFilter>(),
    private val nfcTechListArray: Array<Array<String>>? = null,
    private var nfcStateListener: NfcStateListener? = null,
    private var isAdminLogEnabled: Boolean = false,
) {

    private val nfcAdapter: NfcAdapter? by lazy { NfcAdapter.getDefaultAdapter(activity) }

    private val handlers: MutableList<NfcHandler<*>> = mutableListOf()

    private val nfcStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == NfcAdapter.ACTION_ADAPTER_STATE_CHANGED) {
                val state = intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE, NfcAdapter.STATE_OFF)
                nfcStateListener?.onNfcStateChanged(state)
            }
        }
    }

    /**
     * Checks if NFC is available on the device.
     */
    fun isNfcAvailable(): Boolean = nfcAdapter != null

    /**
     * Checks if NFC is enabled on the device.
     */
    fun isNfcEnabled(): Boolean = nfcAdapter?.isEnabled == true

    /**
     * Add new NfcHandler
     */
    fun addHandler(handler: NfcHandler<*>) {
        handlers.add(handler)
    }

    /**
     * Remove NfcHandler
     */
    fun removeHandler(handler: NfcHandler<*>) {
        handlers.remove(handler)
    }

    fun registerStateReceiver() {
        val filter = IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
        activity.registerReceiver(nfcStateReceiver, filter)
    }

    fun unregisterNfcStateReceiver() {
        activity.unregisterReceiver(nfcStateReceiver)
    }

    /**
     * Enables the Foreground Dispatch System.
     * This allows your activity to intercept NFC intents when it is in the foreground.
     * Call this method in onResume() of your Activity.
     */
    fun enableForegroundDispatch() {
        if (nfcAdapter == null) {
            if (isAdminLogEnabled) Log.w("${NfcAdminError.NFC_ADAPTER_IS_NOT_AVAILABLE}. Foreground dispatch not enabled.")
            nfcStateListener?.onError(NfcAdminError.NFC_ADAPTER_IS_NOT_AVAILABLE)
            return
        }
        if (nfcAdapter?.isEnabled == false) {
            if (isAdminLogEnabled) Log.w("${NfcAdminError.NFC_ADAPTER_IS_NOT_ENABLED.message}. Foreground dispatch not enabled.")
            nfcStateListener?.onError(NfcAdminError.NFC_ADAPTER_IS_NOT_ENABLED)
            return
        }
        try {
            val intent = Intent(activity, activity.javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getActivity(activity, 0, intent, pendingIntentFlags)

            nfcAdapter?.enableForegroundDispatch(activity, pendingIntent, nfcIntentFilters, nfcTechListArray)

            if (isAdminLogEnabled) Log.d("Foreground dispatch is ON.")
            nfcStateListener?.onNfcStarted()
        } catch (e: Exception) {
            if (isAdminLogEnabled) {
                Log.e("${NfcAdminError.ERROR_WHEN_TURNING_ON_FOREGROUND_DISPATCH.message}: ${e.message}", e)
            }
            nfcStateListener?.onError(NfcAdminError.ERROR_WHEN_TURNING_ON_FOREGROUND_DISPATCH)
        }
    }

    /**
     * Disables the Foreground Dispatch System.
     * Call this method in onPause() of your Activity.
     */
    fun disableForegroundDispatch() {
        try {
            nfcAdapter?.disableForegroundDispatch(activity)
            if (isAdminLogEnabled) Log.d("Foreground dispatch is OFF.")
        } catch (e: Exception) {
            if (isAdminLogEnabled) {
                Log.e("${NfcAdminError.ERROR_WHILE_SHUTTING_DOWN_FOREGROUND_DISPATCH.message}: ${e.message}", e)
            }
            nfcStateListener?.onError(NfcAdminError.ERROR_WHILE_SHUTTING_DOWN_FOREGROUND_DISPATCH)
        }
    }

    /**
     * Processes a received NFC intent. Call this method from onNewIntent() of your Activity.
     * @param intent The intent received from the NFC system.
     */
    fun onNewIntentInActivity(intent: Intent) {
        val action = intent.action
//        if (action == Intent.ACTION_MAIN) return // Ignore main action
        Log.d("Received Intent: $action")

        if (NfcAdapter.ACTION_NDEF_DISCOVERED == action ||
            NfcAdapter.ACTION_TECH_DISCOVERED == action ||
            NfcAdapter.ACTION_TAG_DISCOVERED == action
        ) {
            val tagFromIntent: Tag? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            }
            if (tagFromIntent == null) {
                if (isAdminLogEnabled) Log.w(NfcAdminError.TAG_NOT_FOUND.message)
                nfcStateListener?.onError(NfcAdminError.TAG_NOT_FOUND)
                return
            }

            handlers
                .filter { it.isEnabled }
                .forEach {
                    it.preparedNdefMessage?.let { message -> // Attempt to write if there is data to write
                        writeNdefMessageToTag(message, tagFromIntent)
                        it.preparedNdefMessage = null // Reset the message after attempting to write
                    } ?: run {
                        readNdefMessageFromTag(tagFromIntent, it)
                    }
                }
        } else {
            if (isAdminLogEnabled) Log.e("${NfcError.UNKNOWN_NFC_ACTION.message}: $action")
            passScanErrorToHandlers(NfcError.UNKNOWN_NFC_ACTION)
        }
    }

    private fun readNdefMessageFromTag(tag: Tag, handler: NfcHandler<*>) {
        val ndef = Ndef.get(tag)
        if (ndef == null) {
            //Try reading as NDEF Formatable if there is a scan listener
            val ndefFormatable = NdefFormatable.get(tag)
            if (ndefFormatable != null) {
                if (isAdminLogEnabled) Log.e(NfcError.NDEF_FORMATTABLE_BUT_EMPTY.message)
                passScanErrorToHandlers(NfcError.NDEF_FORMATTABLE_BUT_EMPTY)
            } else {
                if (isAdminLogEnabled) Log.e(NfcError.NDEF_NOT_SUPPORTED.message)
                passScanErrorToHandlers(NfcError.NDEF_NOT_SUPPORTED)
            }
            return
        }

        try {
            ndef.connect()
            val ndefMessage = ndef.ndefMessage ?: ndef.cachedNdefMessage // Try also cache
            if (ndefMessage == null) {
                if (isAdminLogEnabled) Log.e(NfcError.NO_NDEF_MESSAGE.message)
                passScanErrorToHandlers(NfcError.NO_NDEF_MESSAGE)
                ndef.close()
                return
            }

            val records = ndefMessage.records
            if (records.isEmpty()) {
                if (isAdminLogEnabled) Log.e(NfcError.NO_NDEF_RECORDS.message)
                passScanErrorToHandlers(NfcError.NO_NDEF_RECORDS)
                ndef.close()
                return
            }
            handler.onNfcTagScanned(ndefMessage.records)
            ndef.close()

            if (isAdminLogEnabled) Log.i("Reads from NFC:\n${records.joinToString("\n")}")
            handlers.forEach { it.onNfcTagScanned(records) }

        } catch (e: IOException) {
            if (isAdminLogEnabled) Log.e("${NfcError.NFC_IO_ERROR.message}: ${e.message}", e)
            passScanErrorToHandlers(NfcError.NFC_IO_ERROR)
        } catch (e: FormatException) {
            if (isAdminLogEnabled) Log.e("${NfcError.NFC_FORMAT_ERROR.message}: ${e.message}", e)
            passScanErrorToHandlers(NfcError.NFC_FORMAT_ERROR)
        } catch (e: Exception) {
            if (isAdminLogEnabled) Log.e("${NfcError.UNKNOWN_READ_ERROR.message}: ${e.message}", e)
            passScanErrorToHandlers(NfcError.UNKNOWN_READ_ERROR)
        } finally {
            try {
                if (ndef.isConnected) {
                    ndef.close()
                }
            } catch (e: IOException) {
                if (isAdminLogEnabled) {
                    Log.e("${NfcAdminError.ERROR_CLOSING_NDEF_CONNECTION.message}: ${e.message}", e)
                }
                nfcStateListener?.onError(NfcAdminError.ERROR_CLOSING_NDEF_CONNECTION)
            }
        }
    }

    private fun passScanErrorToHandlers(error: NfcError) {
        handlers.forEach { it.onScanError(error) }
    }

    private fun writeNdefMessageToTag(message: NdefMessage, tag: Tag) {
        val ndef = Ndef.get(tag)
        if (ndef != null) {
            try {
                ndef.connect()
                if (!ndef.isWritable) {
                    if (isAdminLogEnabled) Log.e(NfcError.TAG_READ_ONLY.message)
                    passWriteErrorToHandlers(NfcError.TAG_READ_ONLY)
                    ndef.close()
                    return
                }
                val maxSize = ndef.maxSize
                if (message.toByteArray().size > maxSize) {
                    if (isAdminLogEnabled) Log.e(NfcError.DATA_TOO_LARGE.message)
                    passWriteErrorToHandlers(NfcError.DATA_TOO_LARGE)
                    ndef.close()
                    return
                }
                ndef.writeNdefMessage(message)

                if (isAdminLogEnabled) Log.i("NDEF message successfully written.")
                handlers.forEach { it.onNfcTagWritten() }

            } catch (e: IOException) {
                if (isAdminLogEnabled) Log.e("${NfcError.NFC_WRITE_IO_ERROR.message}: ${e.message}", e)
                passWriteErrorToHandlers(NfcError.NFC_WRITE_IO_ERROR)
            } catch (e: FormatException) {
                if (isAdminLogEnabled) Log.e("${NfcError.NFC_WRITE_FORMAT_ERROR.message}: ${e.message}", e)
                passWriteErrorToHandlers(NfcError.NFC_WRITE_FORMAT_ERROR)
            } catch (e: Exception) {
                if (isAdminLogEnabled) Log.e(NfcError.UNKNOWN_WRITE_ERROR.message)
                passWriteErrorToHandlers(NfcError.UNKNOWN_WRITE_ERROR)
            } finally {
                try {
                    if (ndef.isConnected) {
                        ndef.close()
                    }
                } catch (e: IOException) {
                    if (isAdminLogEnabled) {
                        Log.e("${NfcAdminError.ERROR_CLOSING_NDEF_CONNECTION.message}: ${e.message}", e)
                    }
                    nfcStateListener?.onError(NfcAdminError.ERROR_CLOSING_NDEF_CONNECTION)
                }
            }
        } else {
            val ndefFormatable = NdefFormatable.get(tag)
            if (ndefFormatable != null) {
                try {
                    ndefFormatable.connect()
                    ndefFormatable.format(message)

                    if (isAdminLogEnabled) Log.i("NDEF message successfully written.")
                    handlers.forEach { it.onNfcTagWritten() }

                } catch (e: IOException) {
                    if (isAdminLogEnabled) Log.e("${NfcError.NFC_FORMATTABLE_WRITE_IO_ERROR.message}: ${e.message}", e)
                    passWriteErrorToHandlers(NfcError.NFC_FORMATTABLE_WRITE_IO_ERROR)
                } catch (e: FormatException) {
                    if (isAdminLogEnabled) Log.e("${NfcError.NFC_FORMATTABLE_FORMAT_ERROR.message}: ${e.message}", e)
                    passWriteErrorToHandlers(NfcError.NFC_FORMATTABLE_FORMAT_ERROR)
                } catch (e: Exception) {
                    if (isAdminLogEnabled) Log.e("${NfcError.UNKNOWN_FORMATTABLE_ERROR.message}: ${e.message}", e)
                    passWriteErrorToHandlers(NfcError.UNKNOWN_FORMATTABLE_ERROR)
                } finally {
                    try {
                        if (ndefFormatable.isConnected) {
                            ndefFormatable.close()
                        }
                    } catch (e: IOException) {
                        if (isAdminLogEnabled) {
                            Log.e("${NfcAdminError.ERROR_CLOSING_NDEFORMATABLE_CONNECTION.message}: ${e.message}", e)
                        }
                        nfcStateListener?.onError(NfcAdminError.ERROR_CLOSING_NDEFORMATABLE_CONNECTION)
                    }
                }
            } else {
                if (isAdminLogEnabled) Log.e(NfcError.TAG_NOT_NDEF_COMPATIBLE.message)
                passWriteErrorToHandlers(NfcError.TAG_NOT_NDEF_COMPATIBLE)
            }
        }
    }

    private fun passWriteErrorToHandlers(error: NfcError) {
        handlers.forEach { it.onWriteError(error) }
    }

    companion object {
         fun createDefaultNfcIntentFilters(): Array<IntentFilter> {
            return NfcIntentFilterBuilder()
                .addNdefDiscovered()
                .addTechDiscovered()
                .addTagDiscovered()
                .build()
        }

         fun createDefaultNfcTechListArray(): Array<Array<String>> {
            return NfcTechListBuilder()
                .addIsoDep()
                .addNdef()
                .addNdefFormatable()
                .addNfcA()
                .addNfcB()
                .addNfcF()
                .addNfcV()
                .build()
        }
    }
}