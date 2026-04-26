package app.baldphone.neo.sms

import android.provider.Telephony
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WapPushReceiverTest {
    @Test
    fun shouldNotifyUnsupportedMmsForWapPushDeliverAction() {
        assertTrue(
            WapPushReceiver.shouldNotifyUnsupportedMms(
                Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION
            )
        )
    }

    @Test
    fun shouldNotNotifyUnsupportedMmsForOtherActions() {
        assertFalse(WapPushReceiver.shouldNotifyUnsupportedMms(null))
        assertFalse(WapPushReceiver.shouldNotifyUnsupportedMms("android.provider.Telephony.SMS_DELIVER"))
    }
}
