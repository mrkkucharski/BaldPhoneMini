package com.bald.uriah.baldphone.utils

import com.bald.uriah.baldphone.testutil.InMemorySharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class BaldPrefsUtilsTest {
    @Test
    fun newInstanceUsesDefaultsConsistently() {
        val first = BaldPrefsUtils.newInstance(InMemorySharedPreferences())
        val second = BaldPrefsUtils.newInstance(InMemorySharedPreferences())

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
    }

    @Test
    fun snapshotDetectsRecentHomePageVisibilityPreferenceChanges() {
        assertSnapshotChangesWhen(BPrefs.PILLS_VISIBLE_KEY, false)
        assertSnapshotChangesWhen(BPrefs.INTERNET_VISIBLE_KEY, false)
        assertSnapshotChangesWhen(BPrefs.MAPS_VISIBLE_KEY, false)
        assertSnapshotChangesWhen(BPrefs.ALARMS_VISIBLE_KEY, false)
        assertSnapshotChangesWhen(BPrefs.PHOTOS_VISIBLE_KEY, false)
    }

    @Test
    fun snapshotDetectsTopBarControlsPreferenceChanges() {
        assertSnapshotChangesWhen(
            BPrefs.HOME_TOP_BAR_CONTROLS_KEY,
            BPrefs.HOME_TOP_BAR_CONTROLS_SIMPLE
        )
    }

    @Test
    fun snapshotDetectsNewCustomButtonPreferenceChanges() {
        assertSnapshotChangesWhen(BPrefs.CUSTOM_PILLS_KEY, "com.example.pills")
        assertSnapshotChangesWhen(BPrefs.CUSTOM_APPS_KEY, "com.example.apps")
        assertSnapshotChangesWhen(BPrefs.CUSTOM_ALARMS_KEY, "com.example.alarms")
    }

    private fun assertSnapshotChangesWhen(key: String, value: Boolean) {
        val before = BaldPrefsUtils.newInstance(InMemorySharedPreferences())
        val afterPrefs = InMemorySharedPreferences(mapOf(key to value))
        val after = BaldPrefsUtils.newInstance(afterPrefs)

        assertNotEquals("Expected $key to participate in BaldPrefsUtils equality", before, after)
    }

    private fun assertSnapshotChangesWhen(key: String, value: Int) {
        val before = BaldPrefsUtils.newInstance(InMemorySharedPreferences())
        val afterPrefs = InMemorySharedPreferences(mapOf(key to value))
        val after = BaldPrefsUtils.newInstance(afterPrefs)

        assertNotEquals("Expected $key to participate in BaldPrefsUtils equality", before, after)
    }

    private fun assertSnapshotChangesWhen(key: String, value: String) {
        val before = BaldPrefsUtils.newInstance(InMemorySharedPreferences())
        val afterPrefs = InMemorySharedPreferences(mapOf(key to value))
        val after = BaldPrefsUtils.newInstance(afterPrefs)

        assertNotEquals("Expected $key to participate in BaldPrefsUtils equality", before, after)
    }
}
