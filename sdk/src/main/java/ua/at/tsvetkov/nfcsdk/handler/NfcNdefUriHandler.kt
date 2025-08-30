package ua.at.tsvetkov.nfcsdk.handler

import android.net.Uri
import android.nfc.NdefMessage
import ua.at.tsvetkov.nfcsdk.NfcListener
import ua.at.tsvetkov.nfcsdk.parser.NfcDataParser
import ua.at.tsvetkov.nfcsdk.parser.NfcNdefUriDataParser
import ua.at.tsvetkov.nfcsdk.preparer.NfcDataPreparer
import ua.at.tsvetkov.nfcsdk.preparer.NfcNdefUriDataPreparer

/**
 * Handles reading and writing URI data (NDEF RTD_URI) to NFC tags.
 */
class NfcNdefUriHandler(
    reader: NfcDataParser<NdefMessage, Uri> = NfcNdefUriDataParser(),
    preparer: NfcDataPreparer<Uri, NdefMessage> = NfcNdefUriDataPreparer(),
    nfcListener: NfcListener<Uri>
) : NfcNdefHandler<Uri>(reader, preparer, nfcListener)
