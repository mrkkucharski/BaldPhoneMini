package app.baldphone.neo.contacts.speeddial

const val MAX_SPEED_DIAL_ENTRIES = 8

data class SpeedDialEntry(
    val lookupKey: String,
    val phoneNumber: String,
    val phoneType: Int,
    val phoneLabel: String?,
    val displayNameSnapshot: String,
    val photoUriSnapshot: String?
)
