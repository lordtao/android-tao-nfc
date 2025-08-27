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

    override fun parse(data: NdefMessage): List<String> {
        val list = mutableListOf<String>()
        for (record in data.records) {
            if (record.tnf == NdefRecord.TNF_WELL_KNOWN && record.type.contentEquals(NdefRecord.RTD_TEXT)) {
                list.add(record.toString())
            }
        }
        return list
    }
}
