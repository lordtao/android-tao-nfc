package ua.at.tsvetkov.nfcsdk.preparer

/**
 * Created by Alexandr Tsvetkov on 26.08.2025.
 */

import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import ua.at.tsvetkov.util.logger.Log

/**
 * Converts a list of [Uri] objects into an [NdefMessage] containing NDEF URI Records.
 *
 * Each [Uri] is transformed into an [NdefRecord] using [NdefRecord.createUri].
 */
class NfcNdefUriDataPreparer : NfcDataPreparer<Uri, NdefMessage> {
    override fun prepare(data: List<Uri>): NdefMessage {
        val list = mutableListOf<NdefRecord>()
        data.forEach { uri ->
            try {
                val record = NdefRecord.createUri(uri)
                list.add(record)
            } catch (e: Exception) {
                Log.e("Error creating NdefRecord for URI: $uri", e)
            }
        }
        return NdefMessage(list.toTypedArray())
    }
}
