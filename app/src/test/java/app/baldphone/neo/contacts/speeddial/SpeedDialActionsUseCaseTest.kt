package app.baldphone.neo.contacts.speeddial

import com.bald.uriah.baldphone.testutil.InMemorySharedPreferences
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedDialActionsUseCaseTest {
    private val repository = SpeedDialRepository(InMemorySharedPreferences())
    private var validLookupKeys = setOf<String>()
    private var contactsPermissionGranted = true
    private val useCase = SpeedDialActionsUseCase(
        repository = repository,
        contactExists = { it in validLookupKeys },
        hasContactsPermissionProvider = { contactsPermissionGranted }
    )

    @Test
    fun addToSpeedDialStoresEntryAndReportsSuccess() = runBlocking {
        val entry = entry("lookup-anna", "+48111111111")

        assertEquals(AddToSpeedDialResult.Success, useCase.addToSpeedDial(entry))

        assertTrue(useCase.isInSpeedDial("lookup-anna"))
        assertEquals(listOf(entry), repository.getAll())
    }

    @Test
    fun addToSpeedDialRejectsSecondNumberForSameContact() = runBlocking {
        val firstNumber = entry("lookup-anna", "+48111111111")
        val secondNumber = entry("lookup-anna", "+48222222222")

        assertEquals(AddToSpeedDialResult.Success, useCase.addToSpeedDial(firstNumber))
        assertEquals(AddToSpeedDialResult.AlreadyExists, useCase.addToSpeedDial(secondNumber))

        assertEquals(listOf(firstNumber), repository.getAll())
    }

    @Test
    fun addToSpeedDialReportsMaxReachedBeforeWriting() = runBlocking {
        repeat(MAX_SPEED_DIAL_ENTRIES) { index ->
            assertEquals(
                AddToSpeedDialResult.Success,
                useCase.addToSpeedDial(entry("lookup-$index", "+48$index"))
            )
        }

        assertEquals(
            AddToSpeedDialResult.MaxReached,
            useCase.addToSpeedDial(entry("lookup-overflow", "+48999"))
        )

        assertEquals(MAX_SPEED_DIAL_ENTRIES, repository.getAll().size)
    }

    @Test
    fun removeFromSpeedDialRemovesEntryForContact() = runBlocking {
        useCase.addToSpeedDial(entry("lookup-anna", "+48111111111"))

        assertTrue(useCase.removeFromSpeedDial("lookup-anna"))

        assertFalse(useCase.isInSpeedDial("lookup-anna"))
    }

    @Test
    fun getAllReturnsStoredEntriesWithoutPruningWhenContactsPermissionIsMissing() {
        contactsPermissionGranted = false
        val stale = entry("lookup-stale", "+48111111111")
        repository.add(stale)

        assertEquals(listOf(stale), useCase.getAll())
        assertEquals(listOf(stale), repository.getAll())
    }

    @Test
    fun getAllPrunesStaleEntriesWhenContactsPermissionIsAvailable() {
        val valid = entry("lookup-valid", "+48111111111")
        val stale = entry("lookup-stale", "+48222222222")
        repository.add(valid)
        repository.add(stale)
        validLookupKeys = setOf("lookup-valid")

        assertEquals(listOf(valid), useCase.getAll())
        assertEquals(listOf(valid), repository.getAll())
    }

    private fun entry(lookupKey: String, phoneNumber: String) = SpeedDialEntry(
        lookupKey = lookupKey,
        phoneNumber = phoneNumber,
        phoneType = 0,
        phoneLabel = null,
        displayNameSnapshot = lookupKey,
        photoUriSnapshot = null
    )
}
