package ua.at.tsvetkov.nfcsdk.parser

/**
 * Created by Alexandr Tsvetkov on 26.08.2025.
 */

import android.nfc.NdefMessage
import android.nfc.NdefRecord

/**
 * Parses an [NdefMessage] to extract text content from NDEF Text Records (RTD_TEXT).
 *
 * It iterates through the records in an [NdefMessage], identifies NDEF Text Records,
 * and extracts their textual payload.
 */
class NfcNdefTextDataParser : NfcDataParser<NdefMessage, String> {

    companion object {
        const val BIT_7 = 0x80
        const val LENGTH_OF_THE_LANGUAGE_CODE = 0x3F
    }

    override fun parse(data: NdefMessage): List<String> {
        val list = mutableListOf<String>()
        data
            .records
            .filter {
                it.tnf == NdefRecord.TNF_WELL_KNOWN && it.type.contentEquals(NdefRecord.RTD_TEXT)
            }
            .forEach { record ->
                try {
                    val payload = record.payload
                    // The first byte of the payload is the status byte
                    val statusByte = payload[0].toInt()

                    // Bit 7 (MSB) specifies the encoding: 0 for UTF-8, 1 for UTF-16
                    val textEncoding = if ((statusByte and BIT_7) == 0) {
                        Charsets.UTF_8
                    } else {
                        Charsets.UTF_16BE // Or UTF_16LE, depends on endianness
                    }

                    // Bits 5-0 define the length of the language code (0 to 63 bytes)
                    val languageCodeLength = statusByte and LENGTH_OF_THE_LANGUAGE_CODE // 00111111 в бинарном виде

                    // Language code (e.g. "en", "ru-RU")
                    // It starts from the 1st byte (after the status byte) and has a length of languageCodeLength
                    val languageCode = String(payload, 1, languageCodeLength, Charsets.US_ASCII)

                    // The text itself starts after the language code and goes until the end of the payload
                    val text = String(
                        payload,
                        1 + languageCodeLength,
                        payload.size - 1 - languageCodeLength,
                        textEncoding
                    )
                    list.add(text)
                } catch (e: Exception) {
                    // Handling possible parsing errors, such as incorrect length,
                    // unknown encoding (although the standard defines UTF-8/UTF-16)
                    // Log.e("NfcParseError", "Error parsing RTD_TEXT record", e)
                    // You can add some error message to the list or skip the record
                    list.add("[Parsing error for RTD_TEXT: ${e.message}]")
                }
            }
        return list
    }
}
