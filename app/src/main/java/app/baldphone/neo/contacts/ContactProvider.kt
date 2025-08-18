package app.baldphone.neo.contacts

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.util.Log

import androidx.core.content.ContextCompat

import com.bald.uriah.baldphone.R

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

import java.text.Normalizer

/**
 * Helper class for searching contacts by phone number as-you-type.
 */
class ContactProvider(private val context: Context) {

    companion object {
        private const val TAG = "ContactProvider"
        private const val DEFAULT_DEBOUNCE_MS = 250L

        private val PROJECTION = arrayOf(
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

        private val NON_DIGIT_REGEX = Regex("[^0-9]")
        private val NON_LETTER_REGEX = Regex("[^a-z]")
        private val NUMERIC_REGEX = Regex("[0-9]+")
    }

    private val t9Map: Map<Char, String> by lazy {
        mapOf(
            '0' to " ",
            '2' to context.getString(R.string.t9_key_2),
            '3' to context.getString(R.string.t9_key_3),
            '4' to context.getString(R.string.t9_key_4),
            '5' to context.getString(R.string.t9_key_5),
            '6' to context.getString(R.string.t9_key_6),
            '7' to context.getString(R.string.t9_key_7),
            '8' to context.getString(R.string.t9_key_8),
            '9' to context.getString(R.string.t9_key_9)
        )
    }

    /**
     * Search contacts as-you-type with auto-detection.
     *
     * Automatically detects what to search based on input:
     * - Numeric input (e.g., "555", "2542") -> Searches phone numbers + optional T9 name matching
     * - Letter input (e.g., "ali", "bob") -> Searches contact names
     * - Mixed input -> Searches both
     */
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    fun search(
        queryFlow: Flow<String>,
        debounceMs: Long = DEFAULT_DEBOUNCE_MS,
        enableT9: Boolean = true,
        groupByInitial: Boolean = true,
        showAllWhenEmpty: Boolean = false,
        filterStarredOnly: Flow<Boolean> = flowOf(false)
    ): Flow<List<ContactItemType>> {
        return combine(
            queryFlow
                .map { it.trim() }
                .distinctUntilChanged()
                .debounce(debounceMs),
            filterStarredOnly.distinctUntilChanged()
        ) { query, starredOnly ->
            query to starredOnly
        }.flatMapLatest { (query, starredOnly) ->
            searchContactsFlow(
                query, enableT9, groupByInitial, showAllWhenEmpty, starredOnly
            )
        }.flowOn(Dispatchers.IO)
    }

    /** Loads all contacts from the database. */
    fun loadAllContacts(
        groupByInitial: Boolean = true,
        filterStarredOnly: Boolean = false
    ): Flow<List<ContactItemType>> {
        return searchContactsFlow(
            query= "",
            enableT9 = false,
            groupByInitial = groupByInitial,
            showAllWhenEmpty = true,
            filterStarredOnly = filterStarredOnly,
        ).flowOn(Dispatchers.IO)
    }

    private fun searchContactsFlow(
        query: String,
        enableT9: Boolean,
        groupByInitial: Boolean,
        showAllWhenEmpty: Boolean,
        filterStarredOnly: Boolean
    ): Flow<List<ContactItemType>> = flow {
        if (query.isEmpty() && !showAllWhenEmpty) {
            emit(emptyList())
            return@flow
        }

        if (!hasContactsPermission()) {
            Log.w(TAG, "READ_CONTACTS permission not granted")
            emit(emptyList())
            return@flow
        }

        val rawResults = searchContacts(query, enableT9, filterStarredOnly)
        // If searching specifically for a name, we might want to deduplicate to one entry per contact,
        // prioritizing the primary number.
        val results = deduplicateContacts(rawResults)

        val listItems = if (groupByInitial) {
            groupContactsByInitial(results)
        } else {
            results.map { ContactItemType.ContactItem(it) }
        }
        emit(listItems)
    }

    private fun searchContacts(
        query: String, enableT9: Boolean, filterStarredOnly: Boolean
    ): List<SimpleContact> {
        val contacts = mutableListOf<SimpleContact>()
        val search = SearchParams(query, enableT9)

        // Use SQL filtering for starred contacts
        val selection = if (filterStarredOnly) {
            "${ContactsContract.CommonDataKinds.Phone.STARRED} = ?"
        } else {
            null
        }
        val selectionArgs = selection?.let { arrayOf("1") }

        try {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                PROJECTION,
                selection,
                selectionArgs,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )?.use {
                val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val lookupIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY)
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                val primaryIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.IS_PRIMARY)
                val starredIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED)
                val typeIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
                val labelIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL)

                if (idIndex < 0 || lookupIndex < 0 || nameIndex < 0 || numberIndex < 0) {
                    return emptyList()
                }

                while (it.moveToNext()) {
                    val id = it.getLong(idIndex)
                    val lookupKey = it.getString(lookupIndex) ?: ""
                    val name = it.getString(nameIndex) ?: "Unknown"
                    val phoneNumber = it.getString(numberIndex) ?: continue

                    if (shouldIncludeContact(search, phoneNumber, name)) {
                        contacts.add(
                            SimpleContact(
                                id = id,
                                lookupKey = lookupKey,
                                name = name,
                                phoneNumber = phoneNumber,
                                photoUri = if (photoIndex >= 0) it.getString(photoIndex) else null,
                                isPrimary = if (primaryIndex >= 0) it.getInt(primaryIndex) != 0 else false,
                                isStarred = if (starredIndex >= 0) it.getInt(starredIndex) != 0 else false,
                                phoneType = if (typeIndex >= 0) it.getInt(typeIndex) else 0,
                                phoneLabel = if (labelIndex >= 0) it.getString(labelIndex) else null
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching contacts", e)
        }
        return contacts
    }

    private fun shouldIncludeContact(
        searchMeta: SearchParams, phoneNumber: String, name: String
    ): Boolean {
        if (searchMeta.isQueryEmpty) {
            return true
        }

        val normalizedNumber = normalizePhoneNumber(phoneNumber)
        return when {
            // Only digits types: search phone, T9, or names (can contains numbers)
            searchMeta.isNumericOnly -> {
                val phoneMatch = normalizedNumber.contains(searchMeta.normalizedQuery)
                val t9Match =
                    if (searchMeta.enableT9) matchesT9Query(name, searchMeta.query) else false
                val numericNameMatch = matchesNameQuery(name, searchMeta.query)

                phoneMatch || t9Match || numericNameMatch
            }

            // Names only (e.g. "Anna") or mixed input (e.g. "Emily 555"): search names
            else -> {
                matchesNameQuery(name, searchMeta.query)
            }
        }
    }

    private data class SearchParams(val query: String, val enableT9: Boolean) {
        val isQueryEmpty = query.isEmpty()
        val isNumericOnly = !isQueryEmpty && query.matches(NUMERIC_REGEX)
        val normalizedQuery = if (isQueryEmpty) "" else query.replace(NON_DIGIT_REGEX, "")
    }

    private fun normalizePhoneNumber(phoneNumber: String): String =
        phoneNumber.replace(NON_DIGIT_REGEX, "")

    private fun normalizeName(input: String): String {
        val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
        val temp = normalized
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase()
        return temp
    }

    private fun matchesNameQuery(name: String, query: String): Boolean =
        normalizeName(name).contains(normalizeName(query))

    private fun matchesT9Query(name: String, query: String): Boolean {
        if (query.isEmpty() || !query.matches(NUMERIC_REGEX)) return false

        val normalizedName = name.lowercase().replace(NON_LETTER_REGEX, "")
        if (normalizedName.isEmpty()) return false

        val nameT9 = normalizedName.map { char ->
            t9Map.entries.find { it.value.contains(char) }?.key ?: '?'
        }.joinToString("")
        return nameT9.contains(query)
    }

    private fun groupContactsByInitial(contacts: List<SimpleContact>): List<ContactItemType> {
        if (contacts.isEmpty()) return emptyList()

        val result = mutableListOf<ContactItemType>()
        val grouped = contacts.groupBy {
            normalizeName(it.name).firstOrNull()?.uppercaseChar()?.toString() ?: "#"
        }

        grouped.toSortedMap().forEach { (letter, list) ->
            result.add(ContactItemType.Header(letter))
            list.sortedBy { normalizeName(it.name) }
                .forEach { result.add(ContactItemType.ContactItem(it)) }
        }

        return result
    }

    private fun deduplicateContacts(contacts: List<SimpleContact>): List<SimpleContact> =
        contacts.groupBy { it.id }.map { (_, entries) -> selectBestEntry(entries) }

    private fun selectBestEntry(entries: List<SimpleContact>): SimpleContact =
        entries.firstOrNull { it.isPrimary }
            ?: entries.firstOrNull { it.phoneType == ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE }
            ?: entries.first()

    private fun hasContactsPermission(): Boolean = ContextCompat.checkSelfPermission(
        context, Manifest.permission.READ_CONTACTS
    ) == PackageManager.PERMISSION_GRANTED
}
