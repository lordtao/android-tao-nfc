package ua.at.tsvetkov.nfcsdk.ui.read

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ReadViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is NFC Read Fragment"
    }
    val text: LiveData<String> = _text
}