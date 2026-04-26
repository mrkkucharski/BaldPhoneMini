package app.baldphone.neo.sms

import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Telephony

import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration

import app.baldphone.neo.activities.ContactsActivity
import app.baldphone.neo.permissions.PermissionManager
import app.baldphone.neo.permissions.RuntimePermission

import com.bald.uriah.baldphone.activities.BaldActivity
import com.bald.uriah.baldphone.databinding.ActivityMessagesBinding

import kotlinx.coroutines.launch

class MessagesActivity : BaldActivity() {

    private lateinit var binding: ActivityMessagesBinding
    private val viewModel: MessagesViewModel by viewModels()
    private var hasSmsPermission = false

    private val adapter by lazy {
        ThreadAdapter { thread ->
            ConversationActivity.open(this, thread.threadId, thread.address, thread.displayName)
        }
    }

    /** Launches ContactsActivity in picker mode and opens a new conversation with the result. */
    private val contactPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val lookupKey = result.data
            ?.getStringExtra(ContactsActivity.EXTRA_CONTACT_LOOKUP_KEY) ?: return@registerForActivityResult
        openNewConversation(lookupKey)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMessagesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.threadsRecyclerView.apply {
            adapter = this@MessagesActivity.adapter
            itemAnimator = null
            setHasFixedSize(false)
            addItemDecoration(DividerItemDecoration(this@MessagesActivity, DividerItemDecoration.VERTICAL))
        }

        binding.buttonNewMessage.setOnClickListener {
            contactPickerLauncher.launch(
                Intent(this, ContactsActivity::class.java)
                    .putExtra(ContactsActivity.EXTRA_PICK_CONTACT, true)
                    .putExtra(ContactsActivity.EXTRA_DIRECT_PICK, true)
            )
        }

        PermissionManager.checkOrRequest(this, RuntimePermission.ReadSendSms) {
            onGranted {
                hasSmsPermission = true
                observeViewModel()
                viewModel.refresh()
            }
            onDenied {
                hasSmsPermission = false
                fallbackToSystemSmsApp()
            }
            onPermanentlyDenied {
                hasSmsPermission = false
                fallbackToSystemSmsApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasSmsPermission || RuntimePermission.ReadSendSms.isGranted(this)) {
            hasSmsPermission = true
            viewModel.refresh()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.threads.collect { threads ->
                    if (threads != null) {
                        adapter.submitList(threads)
                        binding.emptyStateText.isVisible = threads.isEmpty()
                        binding.threadsRecyclerView.isVisible = threads.isNotEmpty()
                    }
                }
            }
        }
    }

    /** Opens the device's default SMS app, then closes this screen. */
    private fun fallbackToSystemSmsApp() {
        val pkg = Telephony.Sms.getDefaultSmsPackage(this)
        if (pkg != null) {
            packageManager.getLaunchIntentForPackage(pkg)?.let { startActivity(it) }
        }
        finish()
    }

    private fun openNewConversation(lookupKey: String) {
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ),
            "${ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY} = ?",
            arrayOf(lookupKey),
            null
        ) ?: return

        val phone = cursor.use {
            if (it.moveToFirst()) {
                val number = it.getString(0) ?: return
                val name = it.getString(1) ?: number
                Pair(number, name)
            } else return
        }

        ConversationActivity.open(this, threadId = -1, address = phone.first, displayName = phone.second)
    }
}
