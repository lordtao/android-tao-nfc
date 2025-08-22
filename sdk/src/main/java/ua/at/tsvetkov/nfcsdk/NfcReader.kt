package ua.at.tsvetkov.nfcsdk

/**
 * Created by Alexandr Tsvetkov on 22.08.2025.
 */
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.FormatException
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.Build
import androidx.fragment.app.Fragment
import ua.at.tsvetkov.util.logger.Log
import java.io.IOException

/**
 * Класс для чтения и записи NFC-меток.
 *
 * @param activity Активность, которая будет использоваться для NFC взаимодействий.
 *                 Необходимо для Foreground Dispatch System.
 * @param nfcScanListener Слушатель для получения результатов сканирования NFC-меток.
 * @param nfcWriteListener Слушатель для получения результатов записи на NFC-метку.
 */
class NfcReader(
    private val activity: Activity,
    private var nfcScanListener: NfcScanListener? = null,
    private var nfcWriteListener: NfcWriteListener? = null,
) {

    /**
     * Альтернативный конструктор для использования с Fragment.
     *
     * @param fragment Фрагмент, который будет использоваться для NFC взаимодействий.
     *                 Активность будет получена из фрагмента.
     * @param nfcScanListener Слушатель для получения результатов сканирования NFC-меток.
     * @param nfcWriteListener Слушатель для получения результатов записи на NFC-метку.
     */
    constructor(
        fragment: Fragment,
        nfcScanListener: NfcScanListener? = null,
        nfcWriteListener: NfcWriteListener? = null,
    ) : this(fragment.requireActivity(), nfcScanListener, nfcWriteListener)

    private val nfcAdapter: NfcAdapter? by lazy { NfcAdapter.getDefaultAdapter(activity) }

    private var messageToWrite: NdefMessage? = null

    /**
     * Проверяет, доступен ли NFC на устройстве.
     */
    fun isNfcAvailable(): Boolean = nfcAdapter != null

    /**
     * Проверяет, включен ли NFC на устройстве.
     */
    fun isNfcEnabled(): Boolean = nfcAdapter?.isEnabled == true

    /**
     * Устанавливает слушатель для событий сканирования NFC.
     */
    fun setNfcScanListener(listener: NfcScanListener?) {
        this.nfcScanListener = listener
    }

    /**
     * Устанавливает слушатель для событий записи NFC.
     */
    fun setNfcWriteListener(listener: NfcWriteListener?) {
        this.nfcWriteListener = listener
    }

    /**
     * Включает Foreground Dispatch System.
     * Это позволяет вашей активности перехватывать NFC-интенты, когда она на переднем плане.
     * Вызывайте этот метод в onResume() вашей Activity.
     */
    fun enableForegroundDispatch() {
        if (nfcAdapter == null) {
            Log.w("${NfcError.NFC_ADAPTER_IS_NOT_AVAILABLE.message}. Foreground dispatch not enabled.")
            return
        }
        if (nfcAdapter?.isEnabled != false) {
            Log.w("${NfcError.NFC_ADAPTER_IS_NOT_ENABLED.message}. Foreground dispatch not enabled.")
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
                    Log.e("Malformed MimeType: ", e)
                    throw RuntimeException("Failed to add NDEF data type", e)
                }
            }

            // Дополнительно можно добавить фильтры для ACTION_TAG_DISCOVERED и ACTION_TECH_DISCOVERED,
            // если нужно обрабатывать не-NDEF метки или метки с определенными технологиями.
            val intentFiltersArray = arrayOf(ndefIntentFilter)

            // Технологии, которые мы хотим обрабатывать (Ndef и NdefFormatable для записи)
            val techListsArray = arrayOf(
                arrayOf(Ndef::class.java.name),
                arrayOf(NdefFormatable::class.java.name)
            )

            nfcAdapter?.enableForegroundDispatch(activity, pendingIntent, intentFiltersArray, techListsArray)
            Log.d("Foreground dispatch включен.")
        } catch (e: Exception) {
            Log.e("Ошибка при включении foreground dispatch: ${e.message}", e)
        }
    }

    /**
     * Выключает Foreground Dispatch System.
     * Вызывайте этот метод в onPause() вашей Activity.
     */
    fun disableForegroundDispatch() {
        try {
            nfcAdapter?.disableForegroundDispatch(activity)
            Log.d("Foreground dispatch выключен.")
        } catch (e: Exception) {
            Log.e("Ошибка при выключении foreground dispatch: ${e.message}", e)
        }
    }

    /**
     * Подготавливает данные для записи на NFC-метку.
     * Этот метод должен быть вызван перед тем, как метка будет поднесена к устройству для записи.
     *
     */
    fun setDataToWrite(ndefMessage: NdefMessage): Boolean {
        return if (ndefMessage.records.isEmpty()) {
            nfcWriteListener?.onNfcWriteError(NfcError.EMPTY_TEXT)
            messageToWrite = null
            false
        } else {
            messageToWrite = ndefMessage
            true
        }
    }

    /**
     * Обрабатывает полученный NFC-интент. Вызывайте этот метод из onNewIntent() вашей Activity.
     * @param intent Интент, полученный от NFC-системы.
     */
    fun handleIntent(intent: Intent) {
        val action = intent.action
        Log.d("Получен Intent: $action")

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
                Log.w("Tag не найден в интенте.")
                val errorMsg = NfcError.TAG_NOT_FOUND
                nfcScanListener?.onNfcScanError(errorMsg)
                nfcWriteListener?.onNfcWriteError(errorMsg)
                return
            }

            // Попытка записи, если есть данные для записи
            if (messageToWrite != null) {
                writeNdefMessageToTag(messageToWrite!!, tagFromIntent)
                messageToWrite = null // Сбрасываем сообщение после попытки записи
            } else {
                // Если нет данных для записи, пытаемся прочитать
                readNdefMessageFromTag(tagFromIntent)
            }
        } else {
            Log.w("Неизвестное NFC действие: $action")
            nfcScanListener?.onNfcScanError(NfcError.UNKNOWN_NFC_ACTION)
        }
    }

    private fun readNdefMessageFromTag(tag: Tag) {
        val ndef = Ndef.get(tag)
        if (ndef == null) {
            // Попробовать прочитать как NDEF Formatable, если есть слушатель scan
            val ndefFormatable = NdefFormatable.get(tag)
            if (ndefFormatable != null) {
                nfcScanListener?.onNfcScanError(NfcError.NDEF_FORMATTABLE_BUT_EMPTY)
            } else {
                nfcScanListener?.onNfcScanError(NfcError.NDEF_NOT_SUPPORTED)
            }
            return
        }

        try {
            ndef.connect()
            val ndefMessage = ndef.ndefMessage ?: ndef.cachedNdefMessage // Попробуем также кешированное
            if (ndefMessage == null) {
                nfcScanListener?.onNfcScanError(NfcError.NO_NDEF_MESSAGE)
                ndef.close()
                return
            }

            val records = ndefMessage.records
            if (records.isEmpty()) {
                nfcScanListener?.onNfcScanError(NfcError.NO_NDEF_RECORDS)
                ndef.close()
                return
            }
            ndef.close()
            Log.list(records.asList(), "Прочитано с NFC")
            nfcScanListener?.onNfcTagScanned(records)
        } catch (e: IOException) {
            Log.e("${NfcError.NFC_IO_ERROR.message}: ${e.message}", e)
            nfcScanListener?.onNfcScanError(NfcError.NFC_IO_ERROR)
        } catch (e: FormatException) {
            Log.e("${NfcError.NFC_FORMAT_ERROR.message}: ${e.message}", e)
            nfcScanListener?.onNfcScanError(NfcError.NFC_FORMAT_ERROR)
        } catch (e: Exception) {
            Log.e("${NfcError.UNKNOWN_READ_ERROR.message}: ${e.message}", e)
            nfcScanListener?.onNfcScanError(NfcError.UNKNOWN_READ_ERROR)
        } finally {
            try {
                if (ndef.isConnected) {
                    ndef.close()
                }
            } catch (e: IOException) {
                Log.e("Ошибка при закрытии Ndef соединения: ${e.message}", e)
            }
        }
    }

    private fun writeNdefMessageToTag(message: NdefMessage, tag: Tag) {
        val ndef = Ndef.get(tag)
        if (ndef != null) {
            try {
                ndef.connect()
                if (!ndef.isWritable) {
                    nfcWriteListener?.onNfcWriteError(NfcError.TAG_READ_ONLY)
                    ndef.close()
                    return
                }
                val maxSize = ndef.maxSize
                if (message.toByteArray().size > maxSize) {
                    nfcWriteListener?.onNfcWriteError(NfcError.DATA_TOO_LARGE)
                    ndef.close()
                    return
                }
                ndef.writeNdefMessage(message)
                nfcWriteListener?.onNfcTagWritten()
                Log.i("NDEF сообщение успешно записано на метку.")
            } catch (e: IOException) {
                Log.e("${NfcError.NFC_WRITE_IO_ERROR.message}: ${e.message}", e)
                nfcWriteListener?.onNfcWriteError(NfcError.NFC_WRITE_IO_ERROR)
            } catch (e: FormatException) {
                Log.e("${NfcError.NFC_WRITE_FORMAT_ERROR.message}: ${e.message}", e)
                nfcWriteListener?.onNfcWriteError(NfcError.NFC_WRITE_FORMAT_ERROR)
            } catch (e: Exception) {
                Log.e("${NfcError.UNKNOWN_WRITE_ERROR.message}: ${e.message}", e)
                nfcWriteListener?.onNfcWriteError(NfcError.UNKNOWN_WRITE_ERROR)
            } finally {
                try {
                    if (ndef.isConnected) {
                        ndef.close()
                    }
                } catch (e: IOException) {
                    Log.e("Ошибка при закрытии Ndef соединения после записи: ${e.message}", e)
                }
            }
        } else {
            // Попробовать отформатировать и записать, если метка поддерживает NdefFormatable
            val ndefFormatable = NdefFormatable.get(tag)
            if (ndefFormatable != null) {
                try {
                    ndefFormatable.connect()
                    ndefFormatable.format(message)
                    nfcWriteListener?.onNfcTagWritten()
                    Log.i("Метка успешно отформатирована и NDEF сообщение записано.")
                } catch (e: IOException) {
                    Log.e("${NfcError.NFC_FORMATTABLE_WRITE_IO_ERROR.message}: ${e.message}", e)
                    nfcWriteListener?.onNfcWriteError(NfcError.NFC_FORMATTABLE_WRITE_IO_ERROR)
                } catch (e: FormatException) {
                    Log.e("${NfcError.NFC_FORMATTABLE_FORMAT_ERROR.message}: ${e.message}", e)
                    nfcWriteListener?.onNfcWriteError(NfcError.NFC_FORMATTABLE_FORMAT_ERROR)
                } catch (e: Exception) {
                    Log.e("${NfcError.UNKNOWN_FORMATTABLE_ERROR.message}: ${e.message}", e)
                    nfcWriteListener?.onNfcWriteError(NfcError.UNKNOWN_FORMATTABLE_ERROR)
                } finally {
                    try {
                        if (ndefFormatable.isConnected) {
                            ndefFormatable.close()
                        }
                    } catch (e: IOException) {
                        Log.e("Ошибка при закрытии NdefFormatable соединения: ${e.message}", e)
                    }
                }
            } else {
                nfcWriteListener?.onNfcWriteTagNotSupported(NfcError.TAG_NOT_NDEF_COMPATIBLE)
                Log.w("Метка не является NDEF и не NdefFormatable.")
            }
        }
    }
}