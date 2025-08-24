package ua.at.tsvetkov.nfcsdk.handler

import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import ua.at.tsvetkov.nfcsdk.NfcError
import ua.at.tsvetkov.util.logger.Log
import java.nio.charset.Charset

/**
 * Created by Alexandr Tsvetkov on 22.08.2025.
 */
class WellKnownUriHandler(
    var languageCode: String = "en",
) : NfcHandler<Uri>() {

    private val TAG = this::class.java.simpleName

    override fun parse(records: Array<out NdefRecord>): Uri? {
        for (record in records) {
            if (record.tnf == NdefRecord.TNF_WELL_KNOWN && record.type.contentEquals(NdefRecord.RTD_URI)) {
                try {
                    return record.toUri()
                } catch (e: Exception) {
                    Log.e("${NfcError.ERROR_PARSING_NDEF_TEXT_RECORD.message}: ${e.message}", e)
                    nfcScanListener?.onNfcScanError(NfcError.ERROR_PARSING_NDEF_TEXT_RECORD)
                }
            } else {
                // It is possible to add processing of other types of records (URI, Smart Poster, etc.)
                Log.e("${NfcError.UNSUPPORTED_NDFE_RECORD_TYPE.message}: tnf=${record.tnf}, type=${record.type}")
            }
        }
        return null
    }

    /**
     * Подготавливает данные для записи на NFC-метку.
     * Этот метод должен быть вызван перед тем, как метка будет поднесена к устройству для записи.
     *
     * @param data Текст для записи на метку.
     */
    override fun prepareToWrite(data: Uri): NdefMessage {
        val preparedData = data.toString()
        val langBytes = languageCode.toByteArray(Charset.forName("US-ASCII"))
        val textBytes = preparedData.toByteArray(Charset.forName("UTF-8"))
        val payload = ByteArray(1 + langBytes.size + textBytes.size)
        payload[0] = (langBytes.size).toByte() // Status byte: UTF-8 encoding, language code length
        System.arraycopy(langBytes, 0, payload, 1, langBytes.size)
        System.arraycopy(textBytes, 0, payload, 1 + langBytes.size, textBytes.size)

        val record = NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, ByteArray(0), payload)
        Log.d("Data for writing has been prepared.: '$preparedData'")
        return NdefMessage(arrayOf(record))
    }

}