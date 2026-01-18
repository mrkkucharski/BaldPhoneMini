package app.baldphone.neo.activities

import android.content.Intent
import android.os.Bundle

import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.DividerItemDecoration.VERTICAL

import app.baldphone.neo.contacts.ContactAdapter
import app.baldphone.neo.contacts.openDetails
import app.baldphone.neo.permissions.PermissionManager
import app.baldphone.neo.permissions.RuntimePermission
import app.baldphone.neo.viewmodels.ContactsViewModel

import com.bald.uriah.baldphone.activities.BaldActivity
import com.bald.uriah.baldphone.activities.contacts.AddContactActivity
import com.bald.uriah.baldphone.databinding.ActivityContactsBinding

import kotlinx.coroutines.launch

class ContactsActivity : BaldActivity() {

    private lateinit var binding: ActivityContactsBinding
    private val viewModel: ContactsViewModel by viewModels()
    private val adapter by lazy { ContactAdapter { it.openDetails(this) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupSearch()
        setupButtons()
        setupKeyboardInsets()

        checkPermissions()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun checkPermissions() {
        PermissionManager.requestPermission(this, RuntimePermission.ReadWriteContacts) {
            onGranted { observeViewModel() }
            onDenied { finish() }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isFavoritesOnly.collect { binding.buttonFavorites.isChecked = it }
                }

                launch {
                    viewModel.contactsFlow.collect { contacts ->
                        // Handle null as a loading state
                        // binding.progressBar.isVisible = contacts == null

                        contacts?.let {
                            adapter.submitList(it)
                            updateEmptyState(it.isEmpty())
                        }
                    }
                }
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyStateText.isVisible = isEmpty
        binding.contactsRecyclerView.isVisible = !isEmpty
    }

    private fun setupRecyclerView() {
        binding.contactsRecyclerView.apply {
            adapter = this@ContactsActivity.adapter
            itemAnimator = null
            setHasFixedSize(true)
            addItemDecoration(DividerItemDecoration(this@ContactsActivity, VERTICAL))
        }
    }

    private fun setupSearch() {
        binding.searchEditText.doAfterTextChanged { text ->
            val query = text?.toString().orEmpty()
            viewModel.searchQuery.value = query
        }
    }

    private fun setupButtons() {
        binding.buttonAddContact.setOnClickListener {
            startActivity(Intent(this, AddContactActivity::class.java))
        }
        binding.buttonFavorites.setOnClickListener {
            viewModel.toggleFavorites()
        }
    }

    /** Hides the bottom action buttons when the keyboard is visible to save screen space. */
    private fun setupKeyboardInsets() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val isImeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())

            binding.contactsBottomActions.isGone = isImeVisible

            view.updatePadding(
                left = systemBars.left,
                top = systemBars.top,
                right = systemBars.right,
                bottom = if (isImeVisible) imeInsets.bottom else systemBars.bottom
            )
            insets
        }
    }
}
