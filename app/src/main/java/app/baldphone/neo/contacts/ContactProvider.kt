package app.baldphone.neo.contacts

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract

import androidx.core.content.ContextCompat

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContactProvider(private val context: Context) {
    private val resolver: ContentResolver = context.contentResolver

    companion object {

        private val PHONE_PROJECTION = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
            ContactsContract.CommonDataKinds.Phone.IS_PRIMARY,
            ContactsContract.CommonDataKinds.Phone.STARRED,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.LABEL
        )
    }

    /**
     * Retrieves a list of all contacts from the database.
     * Filtering and grouping is handled upstream in memory.
     */
    suspend fun getAllContacts(): List<SimpleContact> = withContext(Dispatchers.IO) {
        if (!hasContactsPermission()) return@withContext emptyList()

        val contacts = mutableListOf<SimpleContact>()

        resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            PHONE_PROJECTION,
            null,
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val lookupIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY)
            val nameIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val photoIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
            val primaryIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.IS_PRIMARY)
            val starredIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.STARRED)
            val typeIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE)
            val labelIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LABEL)

            while (cursor.moveToNext()) {
                val name = cursor.getString(nameIdx) ?: android.R.string.unknownName.toString()
                val phoneNumber = cursor.getString(numberIdx) ?: continue

                contacts.add(
                    SimpleContact(
                        id = cursor.getLong(idIdx),
                        lookupKey = cursor.getString(lookupIdx) ?: "",
                        name = name,
                        phoneNumber = phoneNumber,
                        photoUri = cursor.getString(photoIdx),
                        isPrimary = cursor.getInt(primaryIdx) != 0,
                        isStarred = cursor.getInt(starredIdx) != 0,
                        phoneType = cursor.getInt(typeIdx),
                        phoneLabel = cursor.getString(labelIdx)
                    )
                )
            }
        }
        deduplicateContacts(contacts)
    }

    private fun deduplicateContacts(contacts: List<SimpleContact>): List<SimpleContact> =
        contacts.groupBy { it.id }.map { (_, entries) ->
            entries.firstOrNull { it.isPrimary }
                ?: entries.firstOrNull { it.phoneType == ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE }
                ?: entries.first()
        }

    private fun hasContactsPermission(): Boolean = ContextCompat.checkSelfPermission(
        context, Manifest.permission.READ_CONTACTS
    ) == PackageManager.PERMISSION_GRANTED
}
