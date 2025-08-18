package app.baldphone.neo.contacts

data class SimpleContact(
    val id: Long,
    val lookupKey: String,
    val name: String,
    val phoneNumber: String,
    val photoUri: String? = null,
    val isPrimary: Boolean = false,
    val isStarred: Boolean = false,
    val phoneType: Int = 0,
    val phoneLabel: String? = null
)

sealed class ContactItemType {
    /** Section header showing the first letter of grouped contacts */
    data class Header(val letter: String) : ContactItemType()

    /** Actual contact item */
    data class ContactItem(val contact: SimpleContact) : ContactItemType()
}
