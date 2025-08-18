package app.baldphone.neo.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts

import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration

import app.baldphone.neo.calls.CallManager
import app.baldphone.neo.contacts.ContactAdapter
import app.baldphone.neo.contacts.openDetails
import app.baldphone.neo.utils.getTextFromClipboard
import app.baldphone.neo.viewmodels.DialerViewModel

import com.bald.uriah.baldphone.R
import com.bald.uriah.baldphone.activities.BaldActivity
import com.bald.uriah.baldphone.activities.contacts.AddContactActivity
import com.bald.uriah.baldphone.databinding.DialerBinding
import com.bald.uriah.baldphone.databinding.DialpadButtonBinding

import kotlinx.coroutines.launch

class DialerActivity : BaldActivity() {

    companion object {
        private const val TONE_LENGTH_MS = 300
        private const val TONE_VOLUME = 50
    }

    private val viewModel: DialerViewModel by viewModels()
    private lateinit var binding: DialerBinding
    private var dtmfManager: DtmfManager? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.READ_CONTACTS] == true) {
            observeSearchResults()
        }
    }

    private val adapter by lazy {
        ContactAdapter(
            showPhoneNumbers = true
        ) { contact -> contact.openDetails(this) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DialerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize DTMF tone generator
        dtmfManager = try {
            DtmfManager(TONE_VOLUME, TONE_LENGTH_MS)
        } catch (e: Exception) {
            Log.e("DialerActivity", "Failed to initialize DTMF tone generator", e)
            null
        }

        setupRecyclerView()
        setupDialPad()
        setupEmptyState()

        lifecycleScope.launch {
            viewModel.formattedNumber.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                .collect { formatted ->
                    binding.dialPad.tvNumber.text = formatted
                    updateEmptyState(adapter.itemCount > 0, formatted)
                }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            observeSearchResults()
        }

        val permissions = arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_CONTACTS)
        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (missingPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(missingPermissions)
        }
    }

    override fun onDestroy() {
        dtmfManager?.release()
        dtmfManager = null
        super.onDestroy()
    }

    private fun observeSearchResults() {
        lifecycleScope.launch {
            viewModel.searchResults.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                .collect { contacts ->
                    adapter.submitList(contacts)
                    updateEmptyState(
                        hasContacts = contacts.isNotEmpty(),
                        currentNumber = viewModel.rawNumber.value
                    )
                }
        }
    }

    private fun setupRecyclerView() {
        binding.contactsRecyclerView.apply {
            adapter = this@DialerActivity.adapter
            setHasFixedSize(true) // For performance
            addItemDecoration(createDivider())
        }
    }

    private fun createDivider() =
        DividerItemDecoration(this, DividerItemDecoration.VERTICAL).apply {
            ContextCompat.getDrawable(this@DialerActivity, R.drawable.ll_divider)?.let {
                setDrawable(it)
            }
        }

    private fun setupDialPad() = with(binding.dialPad) {

        val buttonList = listOf(
            DialKey(b0, '0', "+", ToneGenerator.TONE_DTMF_0),
            DialKey(b1, '1', null, ToneGenerator.TONE_DTMF_1),
            DialKey(b2, '2', getString(R.string.t9_key_2), ToneGenerator.TONE_DTMF_2),
            DialKey(b3, '3', getString(R.string.t9_key_3), ToneGenerator.TONE_DTMF_3),
            DialKey(b4, '4', getString(R.string.t9_key_4), ToneGenerator.TONE_DTMF_4),
            DialKey(b5, '5', getString(R.string.t9_key_5), ToneGenerator.TONE_DTMF_5),
            DialKey(b6, '6', getString(R.string.t9_key_6), ToneGenerator.TONE_DTMF_6),
            DialKey(b7, '7', getString(R.string.t9_key_7), ToneGenerator.TONE_DTMF_7),
            DialKey(b8, '8', getString(R.string.t9_key_8), ToneGenerator.TONE_DTMF_8),
            DialKey(b9, '9', getString(R.string.t9_key_9), ToneGenerator.TONE_DTMF_9),
            DialKey(bStar, '*', null, ToneGenerator.TONE_DTMF_S),
            DialKey(bHash, '#', null, ToneGenerator.TONE_DTMF_P),
        )

        buttonList.forEach { key ->
            key.view.tvDigit.text = key.digit.toString()
            key.view.tvLetters.text = key.letters
            key.view.root.contentDescription = key.digit.toString()
            key.view.root.setOnClickListener {
                handleDialClick(key.digit, key.tone)
            }
        }

        b0.root.setOnLongClickListener {
            handleDialClick('+', ToneGenerator.TONE_DTMF_0)
            true
        }

        bBackspace.setOnClickListener {
            viewModel.removeLastDigit()
        }

        bBackspace.setOnLongClickListener {
            viewModel.clearNumber()
            true
        }

        bCall.setOnClickListener {
            CallManager.call(this@DialerActivity, viewModel.rawNumber.value)
        }

        tvNumber.setOnLongClickListener {
            pasteNumberFromClipboard()
        }
    }

    private fun handleDialClick(char: Char, tone: Int) {
        binding.dialPad.root.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        dtmfManager?.playTone(tone)
        viewModel.addDigit(char)
    }

    private fun setupEmptyState() {
        binding.emptyStateContainer.btAddContact.setOnClickListener {
            val intent = Intent(this, AddContactActivity::class.java).apply {
                putExtra(AddContactActivity.CONTACT_NUMBER, viewModel.rawNumber.value)
            }
            startActivity(intent)
        }
    }

    private fun updateEmptyState(hasContacts: Boolean, currentNumber: String) {
        if (hasContacts) {
            binding.emptyStateContainer.root.visibility = View.GONE
            binding.scrollingHelper.visibility = View.VISIBLE
        } else {
            binding.emptyStateContainer.root.visibility = View.VISIBLE
            binding.scrollingHelper.visibility = View.INVISIBLE
        }
        binding.dialPad.bBackspace.apply {
            val isEmpty = currentNumber.isEmpty()
            alpha = if (isEmpty) 0.5f else 1.0f
            isEnabled = !isEmpty
        }
    }

    private fun pasteNumberFromClipboard(): Boolean {
        val pasteData = getTextFromClipboard()
        if (!pasteData.isNullOrEmpty()) {
            val filtered = pasteData.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
            if (filtered.isNotEmpty()) {
                viewModel.setNumber(filtered.toString())
                return true
            }
        }
        return false
    }

    private data class DialKey(
        val view: DialpadButtonBinding, val digit: Char, val letters: String?, val tone: Int
    )

    private class DtmfManager(volume: Int, private val duration: Int) {
        private val toneGenerator = ToneGenerator(AudioManager.STREAM_DTMF, volume)

        fun playTone(tone: Int) {
            toneGenerator.startTone(tone, duration)
        }

        fun release() {
            toneGenerator.release()
        }
    }
}
