package ua.at.tsvetkov.nfcsdk.parser

/**
 * Created by Alexandr Tsvetkov on 26.08.2025.
 */

import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord

/**
 * Parses an [NdefMessage] to extract [Uri] objects from NDEF URI Records (RTD_URI).
 *
 * It iterates through records, identifies well-known URI records,
 * and converts them to [Uri] objects using `NdefRecord.toUri()`.
 */
class NfcNdefUriDataParser : NfcDataParser<NdefMessage, Uri> {
    override fun parse(data: NdefMessage): List<Uri> {
        val list = mutableListOf<Uri>()
        for (record in data.records) {
            if (record.tnf == NdefRecord.TNF_WELL_KNOWN && record.type.contentEquals(NdefRecord.RTD_URI)) {
                list.add(record.toUri())
            }
        }
        return list
    }
}
