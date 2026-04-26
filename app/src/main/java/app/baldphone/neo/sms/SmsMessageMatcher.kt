package app.baldphone.neo.sms

object SmsMessageMatcher {
    fun pendingNotYetConfirmed(
        provider: List<SmsMessage>,
        optimistic: List<SmsMessage>,
    ): List<SmsMessage> {
        val unmatchedPending = optimistic.toMutableList()
        provider
            .filter { it.isSent }
            .forEach { sent ->
                val matchIndex = unmatchedPending.indexOfFirst { pending ->
                    pending.body == sent.body && kotlin.math.abs(pending.date - sent.date) < 120_000
                }
                if (matchIndex != -1) unmatchedPending.removeAt(matchIndex)
            }
        return unmatchedPending
    }
}
