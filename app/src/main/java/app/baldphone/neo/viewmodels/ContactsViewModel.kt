package app.baldphone.neo.viewmodels

import android.app.Application

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import app.baldphone.neo.calls.CallsRepository
import app.baldphone.neo.contacts.ContactItemType
import app.baldphone.neo.contacts.ContactSearcher
import app.baldphone.neo.contacts.SimpleContact
import app.baldphone.neo.contacts.data.ContactRepositoryImpl

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ContactsViewModel(application: Application) : AndroidViewModel(application) {

    private val contactProvider = ContactRepositoryImpl.getInstance(application)
    private val callsRepository = CallsRepository(application)
    private val contactSearcher = ContactSearcher(application)

    private val _allContacts = MutableStateFlow<List<SimpleContact>>(emptyList())
    private val _frequentContacts = MutableStateFlow<List<SimpleContact>>(emptyList())

    val searchQuery = MutableStateFlow("")
    val isFavoritesOnly = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val contactsFlow: StateFlow<List<ContactItemType>?> = combine(
        _allContacts.flatMapLatest { contacts ->
            contactSearcher.searchContactsFlow(
                allContacts = contacts,
                searchQueryFlow = searchQuery,
                starredOnlyFlow = isFavoritesOnly,
                showAllWhenEmpty = true
            )
        },
        _frequentContacts,
        searchQuery,
        isFavoritesOnly
    ) { alphabetical, frequent, query, isFavOnly ->
        if (query.isBlank() && !isFavOnly && frequent.size >= 2) {
            listOf(ContactItemType.SectionHeader(FREQUENT_SECTION_TITLE)) +
                frequent.map { ContactItemType.ContactItem(it) } +
                alphabetical
        } else {
            alphabetical
        }
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = null
    )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val allContacts = contactProvider.getAllContacts()
            _allContacts.value = allContacts
            _frequentContacts.value = callsRepository.getFrequentContacts(allContacts)
        }
    }

    fun toggleFavorites() {
        isFavoritesOnly.value = !isFavoritesOnly.value
    }

    companion object {
        const val FREQUENT_SECTION_TITLE = "Frequently Used"
    }
}
