package app.baldphone.neo.utils

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.ContactsContract
import android.telephony.TelephonyManager
import android.util.Log

import androidx.annotation.RequiresPermission
import androidx.annotation.WorkerThread
import androidx.core.content.ContextCompat

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil

object PhoneNumberUtils {
    private val phoneNumberUtil = PhoneNumberUtil.getInstance()

    /**
     * Checks if the given phone number is an emergency number.
     */
    fun isEmergency(context: Context, number: String): Boolean {
        val isEmergency = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            tm.isEmergencyNumber(number)
        } else {
            @Suppress("DEPRECATION")
            android.telephony.PhoneNumberUtils.isEmergencyNumber(number)
        }
        Log.v("PhoneNumberUtils", "isEmergency: $isEmergency")
        return isEmergency
    }

    /**
     * Retrieves the primary emergency number for the current country/network.
     */
    fun getPrimaryEmergencyNumber(context: Context): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED
            ) {

                val emergencyNumbers = telephony.emergencyNumberList
                // Get the first number
                val primary = emergencyNumbers.values.flatten().firstOrNull()
                if (primary != null) return primary.number
            }
        }

        // Universal fallback
        return "112"
    }

    /**
     * Resolves the best available phone number for the contact identified by [lookupKey].
     */
    @RequiresPermission(Manifest.permission.READ_CONTACTS)
    @WorkerThread
    fun resolvePhoneNumber(lookupKey: String, resolver: ContentResolver): String? {
        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val selection = "${ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY} = ?"
        val selectionArgs = arrayOf(lookupKey)
        val sortOrder = "${ContactsContract.CommonDataKinds.Phone.IS_PRIMARY} DESC, " +
                "${ContactsContract.CommonDataKinds.Phone.TYPE} ASC"

        try {
            resolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection, selection, selectionArgs, sortOrder
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return cursor.getString(0)
                }
            }
        } catch (e: Exception) {
            Log.e("PhoneNumberUtils", "Error resolving phone number for lookupKey: $lookupKey", e)
        }
        return null
    }

    /**
     * Formats a phone number to the E.164 standard.
     *
     * E.164 format includes a country code prefixed with '+' and no separators (e.g., +12125552368).
     */
    fun formatToE164(number: String, region: String): String? {
        return try {
            val parsed = phoneNumberUtil.parse(number, region)
            if (phoneNumberUtil.isValidNumber(parsed)) {
                phoneNumberUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164)
            } else null
        } catch (e: NumberParseException) {
            Log.w("PhoneNumberUtils", "Could not parse number for region '$region'", e)
            null
        }
    }

    /**
     * Formats a phone number for display (INTERNATIONAL format).
     * Returns the raw number if parsing fails or if the number is invalid.
     */
    fun formatForDisplay(number: String, region: String): String {
        return try {
            val parsed = phoneNumberUtil.parse(number, region)
            if (phoneNumberUtil.isValidNumber(parsed)) {
                phoneNumberUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL)
                    .replace('-', ' ')
            } else {
                number
            }
        } catch (e: Exception) {
            Log.w("PhoneNumberUtils", "Could not parse number for display: $number", e)
            number
        }
    }

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
