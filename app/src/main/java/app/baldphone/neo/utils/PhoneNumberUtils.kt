package app.baldphone.neo.utils

import com.google.i18n.phonenumbers.PhoneNumberUtil

object PhoneNumberUtils {
    private val phoneNumberUtil = PhoneNumberUtil.getInstance()

    /**
     * Formats a phone number using AsYouTypeFormatter for the given region.
     * Useful for dialer input display.
     */
    fun formatAsYouType(number: String, region: String): String {
        if (number.isEmpty()) return ""
        val formatter = phoneNumberUtil.getAsYouTypeFormatter(region)
        var formatted = ""
        number.forEach { digit ->
            formatted = formatter.inputDigit(digit)
        }
        return formatted.ifEmpty { number }
    }
}
