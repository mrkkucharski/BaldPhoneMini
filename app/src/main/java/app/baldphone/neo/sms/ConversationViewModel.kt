package app.baldphone.neo.sms

import android.app.Application

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ConversationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SmsRepository(application)

    // Messages confirmed to be in the SMS provider
    private val _providerMessages = MutableStateFlow<List<SmsMessage>>(emptyList())

    // Messages sent by the user that may not yet be written to the provider
    // (non-default SMS apps cannot write to Telephony.Sms on Android 4.4+)
    private val _optimisticMessages = MutableStateFlow<List<SmsMessage>>(emptyList())

    private val _sendFailures = MutableSharedFlow<Unit>()
    val sendFailures: SharedFlow<Unit> = _sendFailures.asSharedFlow()

    private val _sendSuccesses = MutableSharedFlow<String>()
    val sendSuccesses: SharedFlow<String> = _sendSuccesses.asSharedFlow()

    /**
     * Merged view: provider messages + any optimistic messages not yet reflected in the provider.
     * Optimistic messages are matched one-to-one with provider rows so repeated short replies
     * like "OK" do not collapse into a single visible message.
     */
    val messages: StateFlow<List<SmsMessage>> = combine(
        _providerMessages, _optimisticMessages
    ) { provider, optimistic ->
        val stillPending = pendingNotYetConfirmed(provider, optimistic)
        (provider + stillPending).sortedBy { it.date }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    var threadId: Long = -1L
        private set
    var address: String = ""
        private set

    fun load(threadId: Long, address: String) {
        this.threadId = threadId
        this.address = address
        if (threadId != -1L) refresh()
    }

    fun refresh() {
        if (threadId == -1L) return
        viewModelScope.launch {
            val fresh = repository.getMessages(threadId)
            _providerMessages.value = fresh
            _optimisticMessages.update { pending -> pendingNotYetConfirmed(fresh, pending) }
        }
    }

    fun sendMessage(body: String) {
        viewModelScope.launch {
            runCatching {
                repository.sendMessage(address, body)
            }.onSuccess {
                val optimistic = SmsMessage(
                    id = -System.currentTimeMillis(),
                    body = body,
                    date = System.currentTimeMillis(),
                    isSent = true,
                )
                _optimisticMessages.update { it + optimistic }
                _sendSuccesses.emit(body)

                // Give the SMS subsystem a moment to write the sent message to the DB
                delay(600)
                // For new conversations, try to discover the thread ID created by the send
                if (threadId == -1L) {
                    threadId = repository.findThreadId(address) ?: -1L
                }
                refresh()
            }.onFailure {
                _sendFailures.emit(Unit)
            }
        }
    }

    private fun pendingNotYetConfirmed(
        provider: List<SmsMessage>,
        optimistic: List<SmsMessage>,
    ): List<SmsMessage> {
        val unmatchedPending = optimistic.toMutableList()
        provider
            .filter { it.isSent }
            .forEach { sent ->
                val matchIndex = unmatchedPending.indexOfFirst { pending ->
                    pending.body == sent.body && kotlin.math.abs(pending.date - sent.date) < 120_000
                }
                if (matchIndex != -1) unmatchedPending.removeAt(matchIndex)
            }
        return unmatchedPending
    }
}
