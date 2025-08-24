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
import android.nfc.NdefRecord
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
 * @param additionalIntentFilters Additionally you can add intent filters like ACTION_TAG_DISCOVERED and ACTION_TECH_DISCOVERED
 *                 to handle specific types of tags.
 *                 These can be added if non-NDEF tags or tags with specific technologies need to be processed.
 */
class NfcManager(
    private val activity: Activity,
    private var nfcStateListener: NfcStateListener? = null,
    private val additionalIntentFilters: List<IntentFilter>? = null,
) {

    private val nfcAdapter: NfcAdapter? by lazy { NfcAdapter.getDefaultAdapter(activity) }

    private val handlers: MutableList<NfcHandler<*>> = mutableListOf()

    private val nfcStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == NfcAdapter.ACTION_ADAPTER_STATE_CHANGED) {
                val state = intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE, NfcAdapter.STATE_OFF)
                nfcStateListener?.onNfcStateChanged(state)
//                when (state) {
//                    NfcAdapter.STATE_ON -> {
//                        // Здесь можно добавить логику для действий при включении NFC
//                        // Например, автоматически вызывать nfcManager.enableForegroundDispatch()
//                        // если это соответствует логике вашего приложения.
//                    }
//                    NfcAdapter.STATE_OFF -> {
//                        // Здесь можно добавить логику для действий при выключении NFC
//                        // Например, информировать пользователя или отключать связанные функции.
//                    }
//                    NfcAdapter.STATE_TURNING_ON -> {
//                        // NFC включается, можно показать индикатор загрузки
//                    }
//                    NfcAdapter.STATE_TURNING_OFF -> {
//                        // NFC выключается
//                    }
//                }
            }
        }
    }

    /**
     * Проверяет, доступен ли NFC на устройстве.
     */
    fun isNfcAvailable(): Boolean = nfcAdapter != null

    /**
     * Проверяет, включен ли NFC на устройстве.
     */
    fun isNfcEnabled(): Boolean = nfcAdapter?.isEnabled == true

    fun addHandler(handler: NfcHandler<*>) {
        handlers.add(handler)
    }

    fun removeHandler(handler: NfcHandler<*>) {
        handlers.remove(handler)
    }

    /**
     * Enables the Foreground Dispatch System.
     * This allows your activity to intercept NFC intents when it is in the foreground.
     * Call this method in onResume() of your Activity.
     */
    fun onResumeInActivity() {
        if (nfcAdapter == null) {
            onCommonError(NfcError.NFC_ADAPTER_IS_NOT_AVAILABLE, "Foreground dispatch not enabled.")
            return
        }
        if (nfcAdapter?.isEnabled != false) {
            onCommonError(NfcError.NFC_ADAPTER_IS_NOT_ENABLED, "Foreground dispatch not enabled.")
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

            // Фильтр для NDEF обнаруженных меток
            val ndefIntentFilter = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
                try {
                    addDataType("*/*") // Принимаем любые NDEF-совместимые типы данных
                } catch (e: IntentFilter.MalformedMimeTypeException) {
                    onCommonError(NfcError.MALFORMED_MIME_TYPE, e, "Failed to add NDEF data type")
                }
            }


            val intentFiltersArray = if (!additionalIntentFilters.isNullOrEmpty()) {
                val combinedFilters = mutableListOf(ndefIntentFilter)
                combinedFilters.addAll(additionalIntentFilters)
                combinedFilters.toTypedArray()
            } else {
                arrayOf(ndefIntentFilter)
            }

            // Технологии, которые мы хотим обрабатывать (Ndef и NdefFormatable для записи)
            val techListsArray = arrayOf(
                arrayOf(Ndef::class.java.name),
                arrayOf(NdefFormatable::class.java.name)
            )

            nfcAdapter?.enableForegroundDispatch(activity, pendingIntent, intentFiltersArray, techListsArray)

            // Registering a BroadcastReceiver to track changes in the state of the NFC adapter
            val filter = IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
            activity.registerReceiver(nfcStateReceiver, filter)

            Log.d("Foreground dispatch is ON.")
        } catch (e: Exception) {
            onCommonError(NfcError.ERROR_WHEN_TURNING_ON_FOREGROUND_DISPATCH, e)
        }
    }

    /**
     * Disables the Foreground Dispatch System.
     * Call this method in onPause() of your Activity.
     */
    fun onPauseInActivity() {
        try {
            // Unregister BroadcastReceiver to avoid memory leaks
            activity.unregisterReceiver(nfcStateReceiver)

            nfcAdapter?.disableForegroundDispatch(activity)
            Log.d("Foreground dispatch is OFF.")
        } catch (e: Exception) {
            onCommonError(NfcError.ERROR_WHILE_SHUTTING_DOWN_FOREGROUND_DISPATCH, e)
        }
    }

    /**
     * Processes a received NFC intent. Call this method from onNewIntent() of your Activity.
     * @param intent The intent received from the NFC system.
     */
    fun handleIntent(intent: Intent) {
        val action = intent.action
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
                Log.w("Tag not found in the Intent.")
                onError(NfcError.TAG_NOT_FOUND)
                return
            }

            // Попытка записи, если есть данные для записи
            handlers
                .filter { it.isEnabled }
                .forEach {
                    it.getNdefMessage()?.let { message ->
                        writeNdefMessageToTag(message, tagFromIntent)
                    } ?: run {
                        readNdefMessageFromTag(tagFromIntent, it)
                    }
                }
        } else {
            Log.w("Unknown NFC action: $action")
            onScanError(NfcError.UNKNOWN_NFC_ACTION)
        }
    }

    private fun onScanSuccessful(records: Array<NdefRecord>) {
        Log.list(records.asList(), "Reads from NFC")
        handlers.forEach { it.onNfcTagScanned(records) }
    }

    private fun onWrittenSuccessful() {
        Log.i("NDEF message successfully written.")
        handlers.forEach { it.onNfcTagWritten() }
    }

    private fun onCommonError(error: NfcError, message: String = "") {
        Log.w("${error.message}. $message")
        nfcStateListener?.onError(error)
    }

    private fun onCommonError(error: NfcError, e: Exception, message: String = "") {
        if (message.isNotEmpty()) {
            Log.e("${error.message}. $message: ${e.message}", e)
        } else {
            Log.e("${error.message}: ${e.message}", e)
        }
        nfcStateListener?.onError(error)
    }

    private fun onError(error: NfcError) {
        handlers.forEach { it.onError(error) }
    }

    private fun onScanError(error: NfcError) {
        handlers.forEach { it.onScanError(error) }
    }

    private fun onScanError(error: NfcError, e: Exception) {
        Log.e("${error.message}: ${e.message}", e)
        handlers.forEach { it.onScanError(error) }
    }

    private fun onWriteError(error: NfcError) {
        handlers.forEach { it.onWriteError(error) }
    }

    private fun onWriteError(error: NfcError, e: Exception) {
        Log.e("${error.message}: ${e.message}", e)
        handlers.forEach { it.onWriteError(error) }
    }

    private fun readNdefMessageFromTag(tag: Tag, handler: NfcHandler<*>) {
        val ndef = Ndef.get(tag)
        if (ndef == null) {
            //Try reading as NDEF Formatable if there is a scan listener
            val ndefFormatable = NdefFormatable.get(tag)
            if (ndefFormatable != null) {
                onScanError(NfcError.NDEF_FORMATTABLE_BUT_EMPTY)
            } else {
                onScanError(NfcError.NDEF_NOT_SUPPORTED)
            }
            return
        }

        try {
            ndef.connect()
            val ndefMessage = ndef.ndefMessage ?: ndef.cachedNdefMessage // Try also cache
            if (ndefMessage == null) {
                onScanError(NfcError.NO_NDEF_MESSAGE)
                ndef.close()
                return
            }

            val records = ndefMessage.records
            if (records.isEmpty()) {
                onScanError(NfcError.NO_NDEF_RECORDS)
                ndef.close()
                return
            }
            handler.parse(ndefMessage.records)
            ndef.close()
            onScanSuccessful(records)
        } catch (e: IOException) {
            onScanError(NfcError.NFC_IO_ERROR, e)
        } catch (e: FormatException) {
            onScanError(NfcError.NFC_FORMAT_ERROR, e)
        } catch (e: Exception) {
            onScanError(NfcError.UNKNOWN_READ_ERROR, e)
        } finally {
            try {
                if (ndef.isConnected) {
                    ndef.close()
                }
            } catch (e: IOException) {
                onCommonError(NfcError.ERROR_CLOSING_NDEF_CONNECTION, e)
            }
        }
    }

    private fun writeNdefMessageToTag(message: NdefMessage, tag: Tag) {
        val ndef = Ndef.get(tag)
        if (ndef != null) {
            try {
                ndef.connect()
                if (!ndef.isWritable) {
                    onWriteError(NfcError.TAG_READ_ONLY)
                    ndef.close()
                    return
                }
                val maxSize = ndef.maxSize
                if (message.toByteArray().size > maxSize) {
                    onWriteError(NfcError.DATA_TOO_LARGE)
                    ndef.close()
                    return
                }
                ndef.writeNdefMessage(message)
                onWrittenSuccessful()
            } catch (e: IOException) {
                onWriteError(NfcError.NFC_WRITE_IO_ERROR, e)
            } catch (e: FormatException) {
                onWriteError(NfcError.NFC_WRITE_FORMAT_ERROR, e)
            } catch (e: Exception) {
                onWriteError(NfcError.UNKNOWN_WRITE_ERROR)
            } finally {
                try {
                    if (ndef.isConnected) {
                        ndef.close()
                    }
                } catch (e: IOException) {
                    onCommonError(NfcError.ERROR_CLOSING_NDEF_CONNECTION, e)
                }
            }
        } else {
            val ndefFormatable = NdefFormatable.get(tag)
            if (ndefFormatable != null) {
                try {
                    ndefFormatable.connect()
                    ndefFormatable.format(message)
                    onWrittenSuccessful()
                } catch (e: IOException) {
                    onWriteError(NfcError.NFC_FORMATTABLE_WRITE_IO_ERROR, e)
                } catch (e: FormatException) {
                    onWriteError(NfcError.NFC_FORMATTABLE_FORMAT_ERROR, e)
                } catch (e: Exception) {
                    onWriteError(NfcError.UNKNOWN_FORMATTABLE_ERROR, e)
                } finally {
                    try {
                        if (ndefFormatable.isConnected) {
                            ndefFormatable.close()
                        }
                    } catch (e: IOException) {
                        onCommonError(NfcError.ERROR_CLOSING_NDEFORMATABLE_CONNECTION, e)
                    }
                }
            } else {
                onWriteError(NfcError.TAG_NOT_NDEF_COMPATIBLE)
            }
        }
    }
}