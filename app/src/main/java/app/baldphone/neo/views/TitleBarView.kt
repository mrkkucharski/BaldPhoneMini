package app.baldphone.neo.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.core.content.res.use
import androidx.core.view.isVisible

import com.bald.uriah.baldphone.R
import com.bald.uriah.baldphone.databinding.ViewTitleBarBinding

/**
 * A custom view that displays a title, an exit button, and an optional 'more' button.
 *
 * This view can be customized via XML attributes:
 * - `titleBarTitle`: Sets the text for the title.
 * - `titleBarBackgroundColor`: Sets the background color of the title bar.
 */
class TitleBarView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : RelativeLayout(context, attrs) {

    private val binding: ViewTitleBarBinding =
        ViewTitleBarBinding.inflate(LayoutInflater.from(context), this)

    init {
        binding.btnMore.isVisible = false

        // The default exit behavior is to simulate a back press.
        // This can be overridden by setOnExitClickListener.
        binding.btnExit.setOnClickListener {
            (context as? ComponentActivity)?.onBackPressedDispatcher?.onBackPressed()
        }

        attrs?.let {
            context.obtainStyledAttributes(it, R.styleable.TitleBarView).use { a ->
                a.getString(R.styleable.TitleBarView_titleBarTitle)?.let { title ->
                    setTitle(title)
                }

                val bgColor = a.getColor(R.styleable.TitleBarView_titleBarBackgroundColor, -1)
                if (bgColor != -1) binding.titleBarRoot.setBackgroundColor(bgColor)
            }
        }
    }

    /** Sets the title text from a string resource. */
    fun setTitle(@StringRes resId: Int) {
        binding.txtTitle.setText(resId)
    }

    /** Sets the title text from a string. */
    fun setTitle(title: String) {
        binding.txtTitle.text = title
    }

    /** Makes the 'More settings' button visible, as it is not visible by default. */
    fun showMoreButton() {
        binding.btnMore.isVisible = true
    }

    /**
     * Registers a callback to be invoked when the 'More' options button is clicked.
     * Call {@link #showMoreButton()} to make the button visible, as it is not visible by default.
     */
    fun setOnMoreClickListener(listener: OnClickListener?) {
        binding.btnMore.setOnClickListener(listener)
    }

    /**
     * Overrides the default exit behavior with a custom click listener.
     * The default action is to trigger a back press.
     */
    fun setOnExitClickListener(listener: OnClickListener?) {
        binding.btnExit.setOnClickListener(listener)
    }
}
