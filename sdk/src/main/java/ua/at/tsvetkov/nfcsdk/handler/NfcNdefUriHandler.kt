package ua.at.tsvetkov.nfcsdk.handler

import android.net.Uri
import android.nfc.NdefMessage
import ua.at.tsvetkov.nfcsdk.NfcScanListener
import ua.at.tsvetkov.nfcsdk.NfcWriteListener
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
    nfcScanListener: NfcScanListener<Uri>? = null,
    nfcWriteListener: NfcWriteListener? = null
) : NfcNdefHandler<Uri>(reader, preparer, nfcScanListener, nfcWriteListener)
