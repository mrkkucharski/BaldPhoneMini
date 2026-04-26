package app.baldphone.neo.sms

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle

import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager

import app.baldphone.neo.permissions.RuntimePermission

import com.bald.uriah.baldphone.R
import com.bald.uriah.baldphone.activities.BaldActivity
import com.bald.uriah.baldphone.databinding.ActivityConversationBinding
import com.bald.uriah.baldphone.utils.BaldToast

import kotlinx.coroutines.launch

class ConversationActivity : BaldActivity() {

    companion object {
        private const val EXTRA_THREAD_ID = "extra_thread_id"
        private const val EXTRA_ADDRESS = "extra_address"
        private const val EXTRA_DISPLAY_NAME = "extra_display_name"

        fun open(context: Context, threadId: Long, address: String, displayName: String) {
            context.startActivity(
                Intent(context, ConversationActivity::class.java).apply {
                    putExtra(EXTRA_THREAD_ID, threadId)
                    putExtra(EXTRA_ADDRESS, address)
                    putExtra(EXTRA_DISPLAY_NAME, displayName)
                }
            )
        }
    }

    private lateinit var binding: ActivityConversationBinding
    private val viewModel: ConversationViewModel by viewModels()

    private val adapter by lazy { MessageAdapter() }

    private val layoutManager by lazy {
        binding.messagesRecyclerView.layoutManager as LinearLayoutManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConversationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!RuntimePermission.ReadSendSms.isGranted(this)) {
            finish()
            return
        }

        val threadId = intent.getLongExtra(EXTRA_THREAD_ID, -1)
        val address = intent.getStringExtra(EXTRA_ADDRESS) ?: ""
        val displayName = intent.getStringExtra(EXTRA_DISPLAY_NAME) ?: address

        binding.titleBar.setTitle(getString(R.string.conversation_with, displayName))

        binding.messagesRecyclerView.apply {
            adapter = this@ConversationActivity.adapter
            itemAnimator = null
            setHasFixedSize(false)
        }

        binding.sendButton.setOnClickListener {
            val text = binding.messageInput.text.toString().trim()
            if (text.isNotEmpty()) {
                viewModel.sendMessage(text)
            }
        }

        setupKeyboardInsets()

        viewModel.load(threadId, address)
        dismissNotification(address)
        observeViewModel()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.messages.collect { messages ->
                        adapter.submitList(messages) {
                            // Scroll to newest message after list update
                            if (messages.isNotEmpty()) {
                                layoutManager.scrollToPosition(messages.size - 1)
                            }
                        }
                    }
                }

                launch {
                    viewModel.sendSuccesses.collect { sentBody ->
                        if (binding.messageInput.text.toString().trim() == sentBody) {
                            binding.messageInput.text?.clear()
                        }
                    }
                }

                launch {
                    viewModel.sendFailures.collect {
                        BaldToast.error(this@ConversationActivity, R.string.an_error_has_occurred)
                    }
                }
            }
        }
    }

    private fun dismissNotification(address: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(address.hashCode())
    }

    private fun setupKeyboardInsets() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                left = systemBars.left,
                top = systemBars.top,
                right = systemBars.right,
                bottom = if (insets.isVisible(WindowInsetsCompat.Type.ime())) imeInsets.bottom else systemBars.bottom
            )
            insets
        }
    }
}
