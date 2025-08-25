package ua.at.tsvetkov.nfcsdk

import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.NfcA
import ua.at.tsvetkov.nfcsdk.handler.NfcHandler
import ua.at.tsvetkov.util.logger.Log

/**
 * Created by Alexandr Tsvetkov on 24.08.2025.
 */
class CardTestHandler : NfcHandler<String, String>() {
    override val techList: List<String> = listOf(IsoDep::class.java.name, NfcA::class.java.name)

    override fun isHavePreparedMessageToWrite(): Boolean = false

    override fun readMessageFromTag(tag: Tag) {
        Log.i("We got a Card Tag for future processing: $tag")
    }

    override fun writeMessageToTag(tag: Tag) {
        Log.i("Cannot write to this card")
    }

    override fun prepareToWrite(data: String) {
        Log.i("Not yet implemented")
    }
}
