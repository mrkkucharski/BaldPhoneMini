package app.baldphone.neo.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class WapPushReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Required to be eligible as default SMS app. MMS handling not implemented.
    }
}
