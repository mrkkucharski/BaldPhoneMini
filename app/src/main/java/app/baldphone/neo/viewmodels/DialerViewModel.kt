package app.baldphone.neo.viewmodels

import android.app.Application

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import app.baldphone.neo.contacts.ContactItemType
import app.baldphone.neo.contacts.ContactProvider
import app.baldphone.neo.utils.PhoneUtils

import com.google.i18n.phonenumbers.AsYouTypeFormatter
import com.google.i18n.phonenumbers.PhoneNumberUtil

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for the DialerActivity:
 * - Manages the dialer number state (add/remove digits)
 * - Formats phone numbers using AsYouTypeFormatter
 * - Provides formatted number as StateFlow for UI observation
 * - Handles contact searching via ContactProvider
 */
class DialerViewModel(application: Application) : AndroidViewModel(application) {

    // Raw number input (unformatted)
    private val _rawNumber = MutableStateFlow("")
    val rawNumber: StateFlow<String> = _rawNumber.asStateFlow()

    // Formatted number for display
    private val _formattedNumber = MutableStateFlow("")
    val formattedNumber: StateFlow<String> = _formattedNumber.asStateFlow()

    private var formatter: AsYouTypeFormatter? = null
    private val deviceRegion: String = PhoneUtils.getDeviceRegion(application)

    private val contactProvider = ContactProvider(application)

    val searchResults: StateFlow<List<ContactItemType>> = contactProvider.search(
        queryFlow = rawNumber,
        enableT9 = true,
        showAllWhenEmpty = false
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        resetFormatter()
    }

    /** Add a digit to the number. */
    fun addDigit(digit: Char) {
        val newRawNumber = _rawNumber.value + digit
        _rawNumber.value = newRawNumber

        _formattedNumber.value = formatter?.inputDigit(digit) ?: newRawNumber
    }

    /** Remove the last digit from the number. */
    fun removeLastDigit() {
        if (_rawNumber.value.isEmpty()) return
        setNumber(_rawNumber.value.dropLast(1))
    }

    /** Clear all digits. */
    fun clearNumber() {
        _rawNumber.value = ""
        _formattedNumber.value = ""
        resetFormatter()
    }

    /** Set the number directly */
    fun setNumber(number: String) {
        _rawNumber.value = number
        reformatNumber(number)
    }

    private fun reformatNumber(number: String) {
        resetFormatter()
        var formatted = ""
        number.forEach { digit ->
            formatted = formatter?.inputDigit(digit) ?: number
        }
        _formattedNumber.value = formatted
    }

    private fun resetFormatter() {
        formatter = PhoneNumberUtil.getInstance().getAsYouTypeFormatter(deviceRegion)
    }
}
