package app.baldphone.neo.sms

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.telephony.SmsManager

class RespondViaMessageService : Service() {

    override fun onBind(intent: Intent): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val recipients = intent.getStringArrayExtra(Intent.EXTRA_PHONE_NUMBER)
            val body = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!recipients.isNullOrEmpty() && !body.isNullOrEmpty()) {
                @Suppress("DEPRECATION")
            val smsManager = SmsManager.getDefault()
                recipients.forEach { smsManager.sendTextMessage(it, null, body, null, null) }
            }
        }
        stopSelf(startId)
        return START_NOT_STICKY
    }
}
