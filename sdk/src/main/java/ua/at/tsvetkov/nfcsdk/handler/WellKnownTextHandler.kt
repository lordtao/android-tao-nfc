package ua.at.tsvetkov.nfcsdk.handler

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import ua.at.tsvetkov.util.logger.Log
import java.nio.charset.Charset

/**
 * Created by Alexandr Tsvetkov on 22.08.2025.
 */
class WellKnownTextHandler(
    var languageCode: String = "en",
) : NfcHandler<String>() {

    override fun parse(records: Array<out NdefRecord>): String {
        val builder = StringBuilder()
        for (record in records) {
            if (record.tnf == NdefRecord.TNF_WELL_KNOWN && record.type.contentEquals(NdefRecord.RTD_TEXT)) {
                try {
                    val payload = record.payload
                    val status = payload[0].toInt()
                    val languageCodeLength = status and 0x3F // Первые 6 бит
                    val encoding = if ((status and 0x80) == 0) "UTF-8" else "UTF-16" // 7-й бит
                    val text = String(
                        payload,
                        languageCodeLength + 1,
                        payload.size - languageCodeLength - 1,
                        Charset.forName(encoding)
                    )
                    builder.append(text).append("\n")
                } catch (e: Exception) {
                    Log.e("Ошибка парсинга текстовой записи NDEF: ${e.message}", e)
                    builder.append("[Ошибка чтения записи]\n")
                }
            } else {
                // Можно добавить обработку других типов записей (URI, Smart Poster и т.д.)
                builder.append("[Неподдерживаемый тип записи: ${record.toUri()}]\n")
            }
        }
        return builder.toString().trim()
    }

    /**
     * Подготавливает данные для записи на NFC-метку.
     * Этот метод должен быть вызван перед тем, как метка будет поднесена к устройству для записи.
     *
     * @param data Текст для записи на метку.
     */
    override fun prepareToWrite(data: String): NdefMessage {
        val langBytes = languageCode.toByteArray(Charset.forName("US-ASCII"))
        val textBytes = data.toByteArray(Charset.forName("UTF-8"))
        val payload = ByteArray(1 + langBytes.size + textBytes.size)
        payload[0] = (langBytes.size).toByte() // Status byte: UTF-8 encoding, language code length
        System.arraycopy(langBytes, 0, payload, 1, langBytes.size)
        System.arraycopy(textBytes, 0, payload, 1 + langBytes.size, textBytes.size)

        val record = NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, ByteArray(0), payload)
        Log.d("Data for writing has been prepared.: '$data'")
        return NdefMessage(arrayOf(record))
    }

}