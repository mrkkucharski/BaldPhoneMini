package app.baldphone.neo.utils

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

import androidx.core.net.toUri

import com.bald.uriah.baldphone.R
import com.bald.uriah.baldphone.utils.BaldToast

private const val TAG = "ContextUtils"

/**
 * Attempts to open a given URL in an appropriate application (usually a web browser).
 *
 * If no application is found on the system that can handle the URL, it displays an error toast
 * to the user.
 *
 * @param url The URL string to be opened.
 */
fun Context.openUrl(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Log.e(TAG, "Cannot open URL: " + e.message)
        BaldToast.from(this).setText("No app found to open URL").setType(BaldToast.TYPE_ERROR)
            .show()
    }
}

/**
 * Copies a given text to the system clipboard.
 *
 * After copying, it displays a toast message "Copied to clipboard" to confirm the action,
 * but only on Android versions older than SDK 31 (Android S), as newer versions show a
 * system-provided confirmation.
 *
 * @param label A user-visible label for the clip data.
 * @param text The actual text to be copied to the clipboard.
 */
fun Context.copyToClipboard(label: CharSequence, text: CharSequence) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        BaldToast.from(this).setType(BaldToast.TYPE_INFORMATIVE)
            .setText(R.string.copied_to_clipboard).show()
    }
}
