package ua.at.tsvetkov.nfcsdk.ui.main

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ua.at.tsvetkov.nfcsdk.NfcAdminError
import ua.at.tsvetkov.nfcsdk.NfcAdminState
import ua.at.tsvetkov.nfcsdk.NfcMessage
import ua.at.tsvetkov.nfcsdk.NfcReadListener
import ua.at.tsvetkov.nfcsdk.NfcStateListener
import ua.at.tsvetkov.nfcsdk.NfcWriteListener
import ua.at.tsvetkov.nfcsdk.handler.NfcNdefHandler.Companion.isPossibleEmptyTag
import ua.at.tsvetkov.nfcsdk.handler.NfcNdefTextHandler
import ua.at.tsvetkov.nfcsdk.handler.NfcNdefUriHandler
import ua.at.tsvetkov.util.logger.Log

private const val MAX_DEMO_RESULTS = 5

private const val EMPTY_TAG_MESSAGE = "Empty tag scanned"

class NfcViewModel : ViewModel() {

    val nfcSupported = MutableLiveData("NFC Supported: Checking...")

    val nfcEnabled = MutableLiveData("NFC Enabled: Checking...")

    val nfcTeach = MutableLiveData("NFC Teach: Checking...")

    val nfcReadStatus = MutableLiveData("No results yet.")

    val nfcWriteStatus = MutableLiveData("Status: Waiting to write...")

    val nfcTextToWrite = MutableLiveData("")

    private val results = mutableListOf<String>()

    private var isReadyToWrite = false

    val stateListener = object : NfcStateListener {
        override fun onNfcStateChanged(state: NfcAdminState) {
            if (state == NfcAdminState.NfcNotAvailable) {
                nfcSupported.postValue("NFC Supported: NO")
            } else {
                nfcSupported.postValue("NFC Supported: YES")
            }
            when (state) {
                NfcAdminState.NfcOff -> nfcEnabled.postValue("NFC Enabled: NO")
                NfcAdminState.NfcOn -> nfcEnabled.postValue("NFC Enabled: YES")
                NfcAdminState.NfcTurningOff -> nfcEnabled.postValue("NFC Enabled: turning off...")
                NfcAdminState.NfcTurningOn -> nfcEnabled.postValue("NFC Enabled: turning on...")
                else -> Unit
            }
            Log.i("NFC State: ${state.getName()}. ${state.message}")
        }

        override fun onError(error: NfcAdminError) {
            Log.e("NFC Admin Error: ${error.message}")
        }
    }

    val uriHandler = NfcNdefUriHandler(
        nfcReadListener = object : NfcReadListener<Uri> {
            override fun onRead(result: List<Uri>) {
                createResultsList(result.map { it.toString() })
                fillTeach()
            }

            override fun onReadEvent(message: NfcMessage, throwable: Throwable?) {
                checkEmptyTag(message)
                fillTeach()
                Log.w("NFC NDEF: ${message.name} (${message.message})")
            }
        },
        nfcWriteListener = object : NfcWriteListener {
            override fun onWritten() {
                nfcWriteStatus.postValue("Write success")
            }

            override fun onWriteEvent(message: NfcMessage, throwable: Throwable?) {
                nfcWriteStatus.postValue(message.message)
                Log.w("NFC NDEF: ${message.name} (${message.message})")
            }
        }
    )

    val textHandler = NfcNdefTextHandler(
        nfcReadListener = object : NfcReadListener<String> {
            override fun onRead(result: List<String>) {
                createResultsList(result)
            }

            override fun onReadEvent(message: NfcMessage, throwable: Throwable?) {
                checkEmptyTag(message)
                Log.w("NFC NDEF: ${message.name} (${message.message})")
            }
        },
        nfcWriteListener = object : NfcWriteListener {
            override fun onWritten() {
                nfcWriteStatus.postValue("Write success")
            }

            override fun onWriteEvent(message: NfcMessage, throwable: Throwable?) {
                nfcWriteStatus.postValue(message.message)
                Log.w("NFC NDEF: ${message.name} (${message.message})")
            }
        }
    )

    private fun fillTeach() {
        val teach = textHandler
            .getCurrentTechs()
            .joinToString(separator = ", ") {
                it.removePrefix("android.nfc.tech.")
            }
        nfcTeach.postValue("Teach: $teach")
    }

//    val nfcMifareUltralightHandler = NfcMifareUltralightTextHandler(
//        nfcReadListener = object : NfcReadListener<String> {
//            override fun onNfcTagScanned(result: List<String>) {
//                createResultsList(result)
//            }
//
//            override fun onNfcScanEvent(message: NfcMessage, throwable: Throwable?) {
//                checkEmptyTag(message)
//                Log.w("NFC: ${message.name} (${message.message})")
//            }
//        },
//        nfcWriteListener = object : NfcWriteListener {
//            override fun onNfcWriteSuccess() {
//                nfcWriteStatus.postValue("Write success")
//            }
//
//            override fun onNfcWriteEvent(message: NfcMessage, throwable: Throwable?) {
//                nfcWriteStatus.postValue(message.message)
//                if (throwable != null) {
//                    Log.w("NFC: ${message.name} (${message.message})", throwable)
//                } else {
//                    Log.w("NFC: ${message.name} (${message.message})")
//                }
//            }
//        }
//    )

    fun onClearTagClick() {
        textHandler.prepareCleaningData()
        uriHandler.prepareCleaningData()
//        nfcMifareUltralightHandler.prepareCleaningData()
        nfcWriteStatus.postValue("Ready to clearing. Bring to NFC device")
    }

    fun onSetForWriteClick() {
        isReadyToWrite = false
        val string = nfcTextToWrite.value ?: ""
        if (string.isEmpty()) {
            textHandler.clearPreparedData()
            uriHandler.clearPreparedData()
//            nfcMifareUltralightHandler.clearPreparedData()
            nfcWriteStatus.postValue("There is nothing to write. Write some text first.")
            return
        }
        @Suppress("SwallowedException")
        try {
            uriHandler.prepareToWrite(listOf(string.toUri()))
        } catch (e: Exception) {
            textHandler.prepareToWrite(listOf(string))
        }
//        nfcMifareUltralightHandler.prepareToWrite(listOf(string))
        nfcWriteStatus.postValue("Ready to write. Bring to NFC device")
        isReadyToWrite = true
    }

    fun onClearStatusClick() {
        textHandler.clearPreparedData()
        uriHandler.clearPreparedData()
//        nfcMifareUltralightHandler.clearPreparedData()

        nfcWriteStatus.postValue("Status: Waiting to write...")
    }

    fun onClearOutputClick() {
        nfcReadStatus.postValue("No results yet.")
    }

    private fun checkEmptyTag(message: NfcMessage) {
        if (isPossibleEmptyTag(message)) {
            if (nfcReadStatus.value != EMPTY_TAG_MESSAGE) {
                nfcReadStatus.postValue(EMPTY_TAG_MESSAGE)
            }
        } else {
            nfcReadStatus.postValue(message.message)
        }
    }

    fun createResultsList(list: List<String>) {
        if (list.isEmpty() || (list.size == 1 && list[0].isEmpty())) {
            nfcReadStatus.postValue("No data (empty tag)")
            return
        }

        list.forEach {
            if (results.size > MAX_DEMO_RESULTS) {
                results.add("Demo limit reached\nRestart the app to scan more tags\n")
                return@forEach
            } else {
                results.add(it)
            }
        }
        val result = results.joinToString(separator = "\n")
        nfcReadStatus.postValue(result)
    }
}
