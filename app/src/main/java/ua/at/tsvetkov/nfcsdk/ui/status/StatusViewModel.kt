package ua.at.tsvetkov.nfcsdk.ui.status

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class StatusViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is NFC Status Fragment"
    }
    val text: LiveData<String> = _text
}