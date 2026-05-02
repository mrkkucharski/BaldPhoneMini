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
            photoUri = "content://photo/anna",
            createdAt = 1L
        )
        val bob = entry(
            lookupKey = "lookup-bob",
            phoneNumber = "+48222222222",
            phoneType = Phone.TYPE_HOME,
            phoneLabel = null,
            displayName = "Bob",
            photoUri = null,
            createdAt = 2L
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

    @Test
    fun getAllReturnsSortedByCreatedAt() {
        val c = entry("lookup-c", "+48333", createdAt = 3L)
        val a = entry("lookup-a", "+48111", createdAt = 1L)
        val b = entry("lookup-b", "+48222", createdAt = 2L)
        listOf(c, a, b).forEach { repository.add(it) }

        assertEquals(listOf(a, b, c), repository.getAll())
    }

    @Test
    fun removeDoesNotAffectSurvivingCreatedAt() {
        val a = entry("lookup-a", "+48111", createdAt = 1L)
        val b = entry("lookup-b", "+48222", createdAt = 2L)
        val c = entry("lookup-c", "+48333", createdAt = 3L)
        listOf(a, b, c).forEach { repository.add(it) }

        repository.remove("lookup-b")

        val result = repository.getAll()
        assertEquals(listOf(a, c), result)
        assertEquals(1L, result[0].createdAt)
        assertEquals(3L, result[1].createdAt)
    }

    @Test
    fun legacyEntriesWithoutCreatedAtAreMigratedToArrayOrder() {
        prefs.edit().putString(
            "speed_dial_entries",
            """[
                {"lookupKey":"k1","phoneNumber":"+1","phoneType":1,"displayName":"One"},
                {"lookupKey":"k2","phoneNumber":"+2","phoneType":1,"displayName":"Two"},
                {"lookupKey":"k3","phoneNumber":"+3","phoneType":1,"displayName":"Three"}
            ]""".trimIndent()
        ).apply()

        val result = repository.getAll()

        assertEquals(3, result.size)
        assertEquals("k1", result[0].lookupKey)
        assertEquals("k2", result[1].lookupKey)
        assertEquals("k3", result[2].lookupKey)
        assertEquals(1L, result[0].createdAt)
        assertEquals(2L, result[1].createdAt)
        assertEquals(3L, result[2].createdAt)
    }

    @Test
    fun migrationIsPersisted() {
        prefs.edit().putString(
            "speed_dial_entries",
            """[
                {"lookupKey":"k1","phoneNumber":"+1","phoneType":1,"displayName":"One"},
                {"lookupKey":"k2","phoneNumber":"+2","phoneType":1,"displayName":"Two"}
            ]""".trimIndent()
        ).apply()

        repository.getAll()

        val storedJson = prefs.getString("speed_dial_entries", null)!!
        assertTrue("createdAt should be persisted after migration", storedJson.contains("createdAt"))

        val second = repository.getAll()
        assertEquals(1L, second[0].createdAt)
        assertEquals(2L, second[1].createdAt)
    }

    @Test
    fun newEntryAfterRemovalSortsLast() {
        val a = entry("lookup-a", "+48111", createdAt = 1L)
        val b = entry("lookup-b", "+48222", createdAt = 2L)
        repository.add(a)
        repository.add(b)
        repository.remove("lookup-a")

        val c = entry("lookup-c", "+48333", createdAt = 3L)
        repository.add(c)

        assertEquals(listOf(b, c), repository.getAll())
    }

    @Test
    fun migrationViaAdd() {
        prefs.edit().putString(
            "speed_dial_entries",
            """[
                {"lookupKey":"k1","phoneNumber":"+1","phoneType":1,"displayName":"One"},
                {"lookupKey":"k2","phoneNumber":"+2","phoneType":1,"displayName":"Two"}
            ]""".trimIndent()
        ).apply()

        val newEntry = entry("lookup-new", "+48999", createdAt = 1_700_000_000_000L)
        repository.add(newEntry)

        val result = repository.getAll()
        assertEquals(3, result.size)
        assertEquals("k1", result[0].lookupKey)
        assertEquals("k2", result[1].lookupKey)
        assertEquals("lookup-new", result[2].lookupKey)
    }

    private fun entry(
        lookupKey: String,
        phoneNumber: String,
        phoneType: Int = Phone.TYPE_MOBILE,
        phoneLabel: String? = null,
        displayName: String = lookupKey,
        photoUri: String? = null,
        createdAt: Long = 1L
    ) = SpeedDialEntry(
        lookupKey = lookupKey,
        phoneNumber = phoneNumber,
        phoneType = phoneType,
        phoneLabel = phoneLabel,
        displayNameSnapshot = displayName,
        photoUriSnapshot = photoUri,
        createdAt = createdAt
    )
}
