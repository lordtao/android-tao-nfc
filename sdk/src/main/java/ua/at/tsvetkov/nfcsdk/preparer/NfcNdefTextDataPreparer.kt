package ua.at.tsvetkov.nfcsdk.preparer

/**
 * Created by Alexandr Tsvetkov on 26.08.2025.
 */

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import java.nio.charset.Charset

/**
 * Converts a list of strings into an [NdefMessage] containing NDEF Text Records (RTD_TEXT).
 *
 * Each string is paired with the provided [languageCode] to create a valid NDEF Text Record.
 * This class handles the manual construction of the NDEF record payload.
 *
 * @param languageCode
 */
class NfcNdefTextDataPreparer(val languageCode: String = "en") : NfcDataPreparer<String, NdefMessage> {
    override fun prepare(data: List<String>): NdefMessage {
        val list = mutableListOf<NdefRecord>()
        val asciiCharset = Charset.forName("US-ASCII")
        val utf8Charset = Charset.forName("UTF-8")
        val langBytes = languageCode.toByteArray(asciiCharset)

        data.forEach { str ->
            val textBytes = str.toByteArray(utf8Charset)
            val payload = ByteArray(1 + langBytes.size + textBytes.size)
            payload[0] = (langBytes.size).toByte()
            System.arraycopy(langBytes, 0, payload, 1, langBytes.size)
            System.arraycopy(textBytes, 0, payload, 1 + langBytes.size, textBytes.size)
            val record =
                NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, ByteArray(0), payload)
            list.add(record)
        }
        return NdefMessage(list.toTypedArray())
    }
}
