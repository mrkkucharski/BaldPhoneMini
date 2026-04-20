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

class SpeedDialActionsUseCase(context: Context) {
    private val appContext = context.applicationContext
    private val repository = SpeedDialRepository(context)
    private val contactRepository = ContactRepositoryImpl.getInstance(appContext)

    fun isInSpeedDial(lookupKey: String): Boolean = repository.contains(lookupKey)

    fun isFull(): Boolean = repository.isFull()

    fun getAll(): List<SpeedDialEntry> {
        val entries = repository.getAll()
        if (!hasContactsPermission()) return entries

        val validKeys = entries.mapNotNull { entry ->
            val contact = runCatching {
                contactRepository.getContactByLookupKeyJava(entry.lookupKey)
            }.getOrNull()
            if (contact == null) null else entry.lookupKey
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

    private fun hasContactsPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
}
