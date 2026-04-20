package app.baldphone.neo.sms

import android.app.Application

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MessagesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SmsRepository(application)

    private val _threads = MutableStateFlow<List<SmsThread>?>(null)
    val threads: StateFlow<List<SmsThread>?> = _threads.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _threads.value = repository.getThreads()
        }
    }
}
