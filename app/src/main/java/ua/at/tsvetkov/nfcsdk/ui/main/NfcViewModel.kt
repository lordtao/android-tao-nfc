package ua.at.tsvetkov.nfcsdk.ui.main

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ua.at.tsvetkov.nfcsdk.NfcAdminError
import ua.at.tsvetkov.nfcsdk.NfcAdminState
import ua.at.tsvetkov.nfcsdk.NfcError
import ua.at.tsvetkov.nfcsdk.NfcScanListener
import ua.at.tsvetkov.nfcsdk.NfcStateListener
import ua.at.tsvetkov.nfcsdk.NfcWriteListener
import ua.at.tsvetkov.nfcsdk.handler.NfcNdefTextHandler
import ua.at.tsvetkov.nfcsdk.handler.NfcNdefUriHandler
import ua.at.tsvetkov.util.logger.Log

class NfcViewModel : ViewModel() {

    private val errorHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(throwable)
    }

    sealed class NfcState {
        object EmptyState : NfcState()
        data class NfcStateEvent(val state: NfcAdminState) : NfcState()
    }

    private val _nfcState = MutableStateFlow<NfcAdminState>(NfcAdminState.NfcUndefined)
    val nfcState: SharedFlow<NfcAdminState> = _nfcState.asStateFlow()

    private val _nfcError = MutableStateFlow<NfcAdminError?>(null)
    val nfcError: SharedFlow<NfcAdminError?> = _nfcError.asStateFlow()

    val nfcAdminStateListener =
        object : NfcStateListener {
            override fun onNfcStateChanged(state: NfcAdminState) {
                viewModelScope.launch(Dispatchers.Main + errorHandler) {
                    _nfcState.emit(state)
                }
                Log.i("NFC State: ${state.message}")
//                when (state) {
//                    NfcAdminState.NfcOn -> {
//                        // Here you can add logic for actions when NFC is enabled
//                        // For example, automatically call nfcManager.enableForegroundDispatch()
//                        // if this matches the logic of your application.
//                        Log.i("NFC State: STATE_ON")
//                    }
//
//                    NfcAdminState.NfcOff -> {
//                        // Here you can add logic for actions when NFC is turned off
//                        // For example, inform the user or disable related functions.
//                        Log.e("NFC State: STATE_OFF")
//                    }
//
//                    NfcAdminState.NfcTurningOff -> {
//                        // NFC is turned on, you can show the loading indicator}
//                    }
//
//                    NfcAdminState.NfcTurningOn -> {
//                        // NFC turns off}
//                    }
//
//                    NfcAdminState.NfcUndefined -> Unit
//                }
            }

            override fun onError(error: NfcAdminError) {
                viewModelScope.launch(Dispatchers.Main + errorHandler) {
                    _nfcError.emit(error)
                }
                Log.e("NFC Error: ${error.message}")
            }
        }

    val nfcUriHandler = NfcNdefUriHandler(
        nfcScanListener = object : NfcScanListener<Uri> {
            override fun onNfcTagScanned(result: List<Uri>) {
                Log.i("Not yet implemented")
            }

            override fun onNfcScanError(error: NfcError, throwable: Throwable?) {
                if (throwable == null) {
                    Log.w("NFC Admin Error: ${error.message}")
                } else {
                    Log.w("NFC Admin Error: ${error.message}", throwable)
                }
            }
        },
        nfcWriteListener = object : NfcWriteListener {
            override fun onNfcWriteSuccess() {
                Log.i("Not yet implemented")
            }

            override fun onNfcWriteError(error: NfcError, throwable: Throwable?) {
                if (throwable == null) {
                    Log.w("NFC Admin Error: ${error.message}")
                } else {
                    Log.w("NFC Admin Error: ${error.message}", throwable)
                }
            }
        }
    )

    val nfcTextHandler = NfcNdefTextHandler(
        nfcScanListener = object : NfcScanListener<String> {
            override fun onNfcTagScanned(result: List<String>) {
                Log.i("Not yet implemented")
            }

            override fun onNfcScanError(error: NfcError, throwable: Throwable?) {
                if (throwable == null) {
                    Log.w("NFC Admin Error: ${error.message}")
                } else {
                    Log.w("NFC Admin Error: ${error.message}", throwable)
                }
            }
        },
        nfcWriteListener = object : NfcWriteListener {
            override fun onNfcWriteSuccess() {
                Log.i("Not yet implemented")
            }

            override fun onNfcWriteError(error: NfcError, throwable: Throwable?) {
                if (throwable == null) {
                    Log.w("NFC Admin Error: ${error.message}")
                } else {
                    Log.w("NFC Admin Error: ${error.message}", throwable)
                }
            }
        }
    )
}
