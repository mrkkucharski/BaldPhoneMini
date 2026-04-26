package app.baldphone.neo.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsMessageMatcherTest {
    @Test
    fun pendingNotYetConfirmedRemovesOnlyOneMatchingOptimisticMessage() {
        val provider = listOf(
            SmsMessage(id = 1, body = "OK", date = 1_000L, isSent = true)
        )
        val optimistic = listOf(
            SmsMessage(id = -1, body = "OK", date = 1_010L, isSent = true),
            SmsMessage(id = -2, body = "OK", date = 1_020L, isSent = true)
        )

        val pending = SmsMessageMatcher.pendingNotYetConfirmed(provider, optimistic)

        assertEquals(listOf(optimistic[1]), pending)
    }

    @Test
    fun pendingNotYetConfirmedIgnoresReceivedProviderMessages() {
        val provider = listOf(
            SmsMessage(id = 1, body = "OK", date = 1_000L, isSent = false)
        )
        val optimistic = listOf(
            SmsMessage(id = -1, body = "OK", date = 1_010L, isSent = true)
        )

        val pending = SmsMessageMatcher.pendingNotYetConfirmed(provider, optimistic)

        assertEquals(optimistic, pending)
    }

    @Test
    fun pendingNotYetConfirmedKeepsOldOptimisticMessageOutsideMatchWindow() {
        val provider = listOf(
            SmsMessage(id = 1, body = "OK", date = 1_000L, isSent = true)
        )
        val optimistic = listOf(
            SmsMessage(id = -1, body = "OK", date = 122_000L, isSent = true)
        )

        val pending = SmsMessageMatcher.pendingNotYetConfirmed(provider, optimistic)

        assertEquals(optimistic, pending)
    }

    @Test
    fun pendingNotYetConfirmedReturnsEmptyWhenAllOptimisticMessagesAreConfirmed() {
        val provider = listOf(
            SmsMessage(id = 1, body = "First", date = 1_000L, isSent = true),
            SmsMessage(id = 2, body = "Second", date = 2_000L, isSent = true)
        )
        val optimistic = listOf(
            SmsMessage(id = -1, body = "First", date = 1_010L, isSent = true),
            SmsMessage(id = -2, body = "Second", date = 2_010L, isSent = true)
        )

        val pending = SmsMessageMatcher.pendingNotYetConfirmed(provider, optimistic)

        assertTrue(pending.isEmpty())
    }
}
