package app.baldphone.neo.sms

import android.provider.Telephony
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsRepositoryTest {
    @Test
    fun sentMessageRecordUsesSentProviderSemantics() {
        val record = SmsRepository.buildSentMessageRecord(
            address = "+48123123123",
            body = "Hello",
            timestamp = 123_456L
        )

        assertEquals("+48123123123", record.address)
        assertEquals("Hello", record.body)
        assertEquals(123_456L, record.date)
        assertEquals(123_456L, record.dateSent)
        assertTrue(record.read)
        assertTrue(record.seen)
        assertEquals(Telephony.Sms.MESSAGE_TYPE_SENT, record.type)
    }
}
