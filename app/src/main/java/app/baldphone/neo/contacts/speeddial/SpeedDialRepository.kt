package app.baldphone.neo.contacts.speeddial

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class SpeedDialRepository {
    private val prefs: SharedPreferences

    constructor(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    constructor(prefs: SharedPreferences) {
        this.prefs = prefs
    }

    fun getAll(): List<SpeedDialEntry> {
        val json = prefs.getString(KEY_ENTRIES, null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i -> fromJson(array.getJSONObject(i)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun add(entry: SpeedDialEntry): Boolean {
        val current = getAll().toMutableList()
        if (current.size >= MAX_SPEED_DIAL_ENTRIES) return false
        if (current.any { it.lookupKey == entry.lookupKey && it.phoneNumber == entry.phoneNumber }) return false
        current.add(entry)
        save(current)
        return true
    }

    fun remove(lookupKey: String): Boolean {
        val current = getAll().toMutableList()
        val removed = current.removeAll { it.lookupKey == lookupKey }
        if (removed) save(current)
        return removed
    }

    fun keepOnly(lookupKeys: Set<String>): Boolean {
        val current = getAll()
        val pruned = current.filter { it.lookupKey in lookupKeys }
        if (pruned.size == current.size) return false
        save(pruned)
        return true
    }

    fun contains(lookupKey: String): Boolean = getAll().any { it.lookupKey == lookupKey }

    fun isFull(): Boolean = getAll().size >= MAX_SPEED_DIAL_ENTRIES

    private fun save(entries: List<SpeedDialEntry>) {
        val array = JSONArray()
        entries.forEach { array.put(toJson(it)) }
        prefs.edit().putString(KEY_ENTRIES, array.toString()).apply()
    }

    private fun toJson(e: SpeedDialEntry): JSONObject = JSONObject().apply {
        put(F_LOOKUP_KEY, e.lookupKey)
        put(F_PHONE_NUMBER, e.phoneNumber)
        put(F_PHONE_TYPE, e.phoneType)
        e.phoneLabel?.let { put(F_PHONE_LABEL, it) }
        put(F_DISPLAY_NAME, e.displayNameSnapshot)
        e.photoUriSnapshot?.let { put(F_PHOTO_URI, it) }
    }

    private fun fromJson(o: JSONObject) = SpeedDialEntry(
        lookupKey = o.getString(F_LOOKUP_KEY),
        phoneNumber = o.getString(F_PHONE_NUMBER),
        phoneType = o.getInt(F_PHONE_TYPE),
        phoneLabel = if (o.has(F_PHONE_LABEL)) o.getString(F_PHONE_LABEL) else null,
        displayNameSnapshot = o.getString(F_DISPLAY_NAME),
        photoUriSnapshot = if (o.has(F_PHOTO_URI)) o.getString(F_PHOTO_URI) else null
    )

    companion object {
        private const val PREFS_NAME = "speed_dial_prefs"
        private const val KEY_ENTRIES = "speed_dial_entries"
        private const val F_LOOKUP_KEY = "lookupKey"
        private const val F_PHONE_NUMBER = "phoneNumber"
        private const val F_PHONE_TYPE = "phoneType"
        private const val F_PHONE_LABEL = "phoneLabel"
        private const val F_DISPLAY_NAME = "displayName"
        private const val F_PHOTO_URI = "photoUri"
    }
}
