package ua.at.tsvetkov.nfcsdk

import android.content.IntentFilter
import android.nfc.NfcAdapter
import ua.at.tsvetkov.util.logger.Log

/**
 * Created by Alexandr Tsvetkov on 24.08.2025.
 */
class NfcIntentFilterBuilder {

    // Приватный список для хранения фильтров
    private val filters = mutableListOf<IntentFilter>()

    // Метод для добавления фильтра NDEF_DISCOVERED
    fun addNdefDiscovered(): NfcIntentFilterBuilder {
        val ndef = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
            try {
                addDataType("*/*")
            } catch (e: IntentFilter.MalformedMimeTypeException) {
                Log.e("${NfcAdminError.MALFORMED_MIME_TYPE.message}. Failed to add NfcAdapter.ACTION_NDEF_DISCOVERED data type: ${e.message}", e)
            }
        }
        filters.add(ndef)
        return this
    }

    // Метод для добавления фильтра TECH_DISCOVERED
    fun addTechDiscovered(): NfcIntentFilterBuilder {
        filters.add(IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED))
        return this
    }

    // Метод для добавления фильтра TAG_DISCOVERED
    fun addTagDiscovered(): NfcIntentFilterBuilder {
        filters.add(IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED))
        return this
    }

    // Метод для добавления пользовательских фильтров
    fun addCustomFilter(intentFilter: IntentFilter): NfcIntentFilterBuilder {
        filters.add(intentFilter)
        return this
    }

    // Метод, который "строит" итоговый массив
    fun build(): Array<IntentFilter> {
        return filters.toTypedArray()
    }

}