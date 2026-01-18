package app.baldphone.neo.contacts

import android.content.Context
import android.content.Intent
import android.widget.Toast

import androidx.core.net.toUri

import com.bald.uriah.baldphone.activities.contacts.SingleContactActivity

/**
 * Extension functions for Contact operations.
 */

/**
 * Open the BaldPhone contact details activity for this contact.
 *
 * Usage:
 * ```
 * contact.openDetails(context)
 * ```
 *
 * @param context Context to launch the activity
 */
fun SimpleContact.openDetails(context: Context) {
    try {
        val intent = Intent(context, SingleContactActivity::class.java).apply {
            putExtra(SingleContactActivity.CONTACT_ID, id.toString())
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        // Fallback to toast if error occurs
        Toast.makeText(
            context,
            "Contact: $name\nPhone: $phoneNumber\nID: $id",
            Toast.LENGTH_LONG
        ).show()
    }
}

/**
 * Send SMS to this contact's phone number.
 *
 * This launches the SMS app with the number pre-filled.
 * User can then type and send the message.
 *
 * Usage:
 * ```
 * contact.sendSms(context, "Hello!")
 * ```
 *
 * @param context Context to launch the SMS app
 * @param message Optional pre-filled message text
 */
fun SimpleContact.sendSms(context: Context, message: String = "") {
    try {
        val smsUri = "sms:$phoneNumber".toUri()
        val intent = Intent(Intent.ACTION_VIEW, smsUri).apply {
            if (message.isNotEmpty()) {
                putExtra("sms_body", message)
            }
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(
            context,
            "Unable to open SMS app",
            Toast.LENGTH_SHORT
        ).show()
    }
}

/**
 * Share this contact's information.
 *
 * Opens the system share sheet to share contact details.
 *
 * Usage:
 * ```
 * contact.share(context)
 * ```
 *
 * @param context Context to launch the share sheet
 */
fun SimpleContact.share(context: Context) {
    try {
        val shareText = "Contact: $name\nPhone: $phoneNumber"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "Contact: $name")
        }
        context.startActivity(Intent.createChooser(intent, "Share Contact"))
    } catch (_: Exception) {
        Toast.makeText(
            context,
            "Unable to share contact",
            Toast.LENGTH_SHORT
        ).show()
    }
}
