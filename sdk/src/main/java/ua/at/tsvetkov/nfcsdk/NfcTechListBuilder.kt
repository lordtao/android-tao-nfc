package ua.at.tsvetkov.nfcsdk

import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.nfc.tech.NfcA
import android.nfc.tech.NfcB
import android.nfc.tech.NfcF
import android.nfc.tech.NfcV

/**
 * Created by Alexandr Tsvetkov on 24.08.2025.
 */
// Класс-билдер для создания массива технологических списков
class NfcTechListBuilder {

    // Приватный список для хранения массивов технологий
    private val techLists = mutableListOf<Array<String>>()

    // Метод для добавления технологии IsoDep (для банковских карт)
    fun addIsoDep(): NfcTechListBuilder {
        techLists.add(arrayOf(IsoDep::class.java.name))
        return this
    }

    // Метод для добавления технологии Ndef
    fun addNdef(): NfcTechListBuilder {
        techLists.add(arrayOf(Ndef::class.java.name))
        return this
    }

    // Метод для добавления технологии NdefFormatable
    fun addNdefFormatable(): NfcTechListBuilder {
        techLists.add(arrayOf(NdefFormatable::class.java.name))
        return this
    }

    // Метод для добавления технологии NfcA
    fun addNfcA(): NfcTechListBuilder {
        techLists.add(arrayOf(NfcA::class.java.name))
        return this
    }

    // Метод для добавления технологии NfcB
    fun addNfcB(): NfcTechListBuilder {
        techLists.add(arrayOf(NfcB::class.java.name))
        return this
    }

    // Метод для добавления технологии NfcF
    fun addNfcF(): NfcTechListBuilder {
        techLists.add(arrayOf(NfcF::class.java.name))
        return this
    }

    // Метод для добавления технологии NfcV
    fun addNfcV(): NfcTechListBuilder {
        techLists.add(arrayOf(NfcV::class.java.name))
        return this
    }

    // Метод для добавления пользовательских технологий
    fun addCustomTech(vararg techNames: String): NfcTechListBuilder {
        techLists.add(arrayOf(*techNames))
        return this
    }

    // Метод, который "строит" итоговый массив
    fun build(): Array<Array<String>> {
        return techLists.toTypedArray()
    }
}