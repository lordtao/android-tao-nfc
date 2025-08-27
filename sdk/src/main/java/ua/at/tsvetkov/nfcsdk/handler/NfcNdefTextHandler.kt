package ua.at.tsvetkov.nfcsdk.handler

import android.nfc.NdefMessage
import ua.at.tsvetkov.nfcsdk.NfcReadListener
import ua.at.tsvetkov.nfcsdk.NfcWriteListener
import ua.at.tsvetkov.nfcsdk.parser.NfcDataParser
import ua.at.tsvetkov.nfcsdk.parser.NfcNdefTextDataParser
import ua.at.tsvetkov.nfcsdk.preparer.NfcDataPreparer
import ua.at.tsvetkov.nfcsdk.preparer.NfcNdefTextDataPreparer

/**
 * Handles reading and writing plain text data (NDEF RTD_TEXT) to NFC tags.
 */
class NfcNdefTextHandler(
    parser: NfcDataParser<NdefMessage, String> = NfcNdefTextDataParser(),
    preparer: NfcDataPreparer<String, NdefMessage> = NfcNdefTextDataPreparer(),
    nfcReadListener: NfcReadListener<String>? = null,
    nfcWriteListener: NfcWriteListener? = null
) : NfcNdefHandler<String>(parser, preparer, nfcReadListener, nfcWriteListener)
