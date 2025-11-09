package app.baldphone.neo.activities

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle

import androidx.core.net.toUri

import app.baldphone.neo.Constants
import app.baldphone.neo.utils.getDeviceInfoFull

import com.bald.uriah.baldphone.R
import com.bald.uriah.baldphone.activities.BaldActivity
import com.bald.uriah.baldphone.databinding.ActivityFeedbackBinding
import com.bald.uriah.baldphone.utils.BaldToast

class FeedbackActivity : BaldActivity() {

    private lateinit var binding: ActivityFeedbackBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedbackBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.feedbackSendButton.setOnClickListener {
            sendFeedback()
        }
    }

    private fun sendFeedback() {
        val message = binding.feedbackInput.text.toString().trim()
        if (message.isEmpty()) {
            BaldToast.from(this).setType(BaldToast.TYPE_INFORMATIVE)
                .setText(R.string.feedback_cannot_be_empty).show()
            return
        }
        val body = buildString {
            append(message)
            if (binding.feedbackIncludeInfoCheckbox.isChecked) {
                append("\n\n---\n")
                append(getDeviceInfoFull())
            }
        }
        launchEmailApp(body)
    }

    private fun launchEmailApp(body: String) {
        val subject = getString(
            R.string.feedback_email_subject, getString(R.string.app_display_name)
        )

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = "mailto:${Constants.APP_CONTACT_EMAIL}".toUri()
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }

        launchChooserSafely(intent, getString(R.string.feedback_choose_email_app))
    }

    private fun launchChooserSafely(intent: Intent, chooserTitle: String) {
        try {
            val finalIntent = Intent.createChooser(intent, chooserTitle)
            startActivity(finalIntent)
        } catch (_: ActivityNotFoundException) {
            BaldToast.error(this, R.string.feedback_no_email_app_found)
        }
    }

    override fun requiredPermissions(): Int {
        return PERMISSION_NONE
    }
}
