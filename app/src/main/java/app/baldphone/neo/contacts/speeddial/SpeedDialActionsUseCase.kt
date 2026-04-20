package app.baldphone.neo.contacts.speeddial

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import app.baldphone.neo.contacts.data.ContactRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class AddToSpeedDialResult {
    object Success : AddToSpeedDialResult()
    object AlreadyExists : AddToSpeedDialResult()
    object MaxReached : AddToSpeedDialResult()
    object Failed : AddToSpeedDialResult()
}

class SpeedDialActionsUseCase(
    private val repository: SpeedDialRepository,
    private val contactExists: (String) -> Boolean,
    private val hasContactsPermissionProvider: () -> Boolean
) {
    constructor(context: Context) : this(
        SpeedDialRepository(context),
        createContactExists(context.applicationContext),
        { hasContactsPermission(context.applicationContext) }
    )

    fun isInSpeedDial(lookupKey: String): Boolean = repository.contains(lookupKey)

    fun isFull(): Boolean = repository.isFull()

    fun getAll(): List<SpeedDialEntry> {
        val entries = repository.getAll()
        if (!hasContactsPermissionProvider()) return entries

        val validKeys = entries.mapNotNull { entry ->
            if (contactExists(entry.lookupKey)) entry.lookupKey else null
        }.toSet()

        repository.keepOnly(validKeys)
        return entries.filter { it.lookupKey in validKeys }
    }

    suspend fun addToSpeedDial(entry: SpeedDialEntry): AddToSpeedDialResult = withContext(Dispatchers.IO) {
        when {
            repository.contains(entry.lookupKey) -> AddToSpeedDialResult.AlreadyExists
            repository.isFull() -> AddToSpeedDialResult.MaxReached
            repository.add(entry) -> AddToSpeedDialResult.Success
            else -> AddToSpeedDialResult.Failed
        }
    }

    suspend fun removeFromSpeedDial(lookupKey: String): Boolean = withContext(Dispatchers.IO) {
        repository.remove(lookupKey)
    }

    companion object {
        private fun createContactExists(context: Context): (String) -> Boolean {
            val contactRepository = ContactRepositoryImpl.getInstance(context)
            return { lookupKey ->
                runCatching {
                    contactRepository.getContactByLookupKeyJava(lookupKey)
                }.getOrNull() != null
            }
        }

        private fun hasContactsPermission(context: Context): Boolean =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
    }
}
