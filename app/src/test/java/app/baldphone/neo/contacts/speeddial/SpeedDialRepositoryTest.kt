package app.baldphone.neo.contacts.speeddial

import android.provider.ContactsContract.CommonDataKinds.Phone
import com.bald.uriah.baldphone.testutil.InMemorySharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedDialRepositoryTest {
    private val prefs = InMemorySharedPreferences()
    private val repository = SpeedDialRepository(prefs)

    @Test
    fun getAllReturnsEmptyListWhenNothingIsStored() {
        assertTrue(repository.getAll().isEmpty())
    }

    @Test
    fun addPersistsAllEntryFieldsInOrder() {
        val anna = entry(
            lookupKey = "lookup-anna",
            phoneNumber = "+48111111111",
            phoneType = Phone.TYPE_MOBILE,
            phoneLabel = "Personal",
            displayName = "Anna",
            photoUri = "content://photo/anna"
        )
        val bob = entry(
            lookupKey = "lookup-bob",
            phoneNumber = "+48222222222",
            phoneType = Phone.TYPE_HOME,
            phoneLabel = null,
            displayName = "Bob",
            photoUri = null
        )

        assertTrue(repository.add(anna))
        assertTrue(repository.add(bob))

        assertEquals(listOf(anna, bob), repository.getAll())
    }

    @Test
    fun addRejectsDuplicateLookupKeyAndPhoneNumber() {
        val original = entry("lookup-anna", "+48111111111")
        val duplicate = original.copy(displayNameSnapshot = "Anna Renamed")

        assertTrue(repository.add(original))
        assertFalse(repository.add(duplicate))

        assertEquals(listOf(original), repository.getAll())
    }

    @Test
    fun addAllowsSameContactWithDifferentNumberAtRepositoryLevel() {
        val mobile = entry("lookup-anna", "+48111111111")
        val home = entry("lookup-anna", "+48222222222", phoneType = Phone.TYPE_HOME)

        assertTrue(repository.add(mobile))
        assertTrue(repository.add(home))

        assertEquals(listOf(mobile, home), repository.getAll())
    }

    @Test
    fun addRejectsEntriesAfterMaximumLimit() {
        repeat(MAX_SPEED_DIAL_ENTRIES) { index ->
            assertTrue(repository.add(entry("lookup-$index", "+48$index")))
        }

        assertFalse(repository.add(entry("lookup-overflow", "+48999")))
        assertEquals(MAX_SPEED_DIAL_ENTRIES, repository.getAll().size)
    }

    @Test
    fun removeDeletesEveryEntryForLookupKey() {
        val annaMobile = entry("lookup-anna", "+48111111111")
        val annaHome = entry("lookup-anna", "+48222222222")
        val bob = entry("lookup-bob", "+48333333333")
        listOf(annaMobile, annaHome, bob).forEach { repository.add(it) }

        assertTrue(repository.remove("lookup-anna"))

        assertEquals(listOf(bob), repository.getAll())
    }

    @Test
    fun removeReturnsFalseWhenLookupKeyIsAbsent() {
        repository.add(entry("lookup-anna", "+48111111111"))

        assertFalse(repository.remove("lookup-missing"))
        assertEquals(1, repository.getAll().size)
    }

    @Test
    fun keepOnlyPrunesStaleLookupKeysAndReportsChange() {
        val anna = entry("lookup-anna", "+48111111111")
        val stale = entry("lookup-stale", "+48222222222")
        listOf(anna, stale).forEach { repository.add(it) }

        assertTrue(repository.keepOnly(setOf("lookup-anna")))

        assertEquals(listOf(anna), repository.getAll())
    }

    @Test
    fun keepOnlyReturnsFalseWhenNothingWasPruned() {
        repository.add(entry("lookup-anna", "+48111111111"))

        assertFalse(repository.keepOnly(setOf("lookup-anna")))
    }

    @Test
    fun invalidStoredJsonIsTreatedAsEmpty() {
        prefs.edit().putString("speed_dial_entries", "not-json").apply()

        assertTrue(repository.getAll().isEmpty())
        assertFalse(repository.isFull())
    }

    private fun entry(
        lookupKey: String,
        phoneNumber: String,
        phoneType: Int = Phone.TYPE_MOBILE,
        phoneLabel: String? = null,
        displayName: String = lookupKey,
        photoUri: String? = null
    ) = SpeedDialEntry(
        lookupKey = lookupKey,
        phoneNumber = phoneNumber,
        phoneType = phoneType,
        phoneLabel = phoneLabel,
        displayNameSnapshot = displayName,
        photoUriSnapshot = photoUri
    )
}
