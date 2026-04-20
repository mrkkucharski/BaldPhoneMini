package app.baldphone.neo.sms

data class SmsMessage(
    val id: Long,
    val body: String,
    val date: Long,
    val isSent: Boolean,
)
