package app.baldphone.neo.sms

data class SmsThread(
    val threadId: Long,
    val address: String,
    val contactName: String?,
    val snippet: String,
    val date: Long,
    val isRead: Boolean,
) {
    val displayName: String get() = contactName ?: address
}
