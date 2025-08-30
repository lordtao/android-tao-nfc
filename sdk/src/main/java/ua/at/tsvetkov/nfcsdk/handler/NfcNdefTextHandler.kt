package ua.at.tsvetkov.nfcsdk.handler

import android.nfc.NdefMessage
import ua.at.tsvetkov.nfcsdk.NfcListener
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
    nfcListener: NfcListener<String>
) : NfcNdefHandler<String>(parser, preparer, nfcListener)
