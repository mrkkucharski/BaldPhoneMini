package app.baldphone.neo.utils

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager

import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.core.graphics.drawable.toDrawable

import com.bald.uriah.baldphone.activities.BaldActivity
import com.bald.uriah.baldphone.databinding.AppDialogBinding
import com.bald.uriah.baldphone.views.BaldButton

/**
 * A custom replacement for BDialog, implemented in Kotlin.
 * Features a Builder API similar to MaterialAlertDialog and support for custom background colors.
 */
class AppDialog private constructor(
    context: Context, private val builder: Builder
) : Dialog(context) {

    private val binding: AppDialogBinding by lazy {
        AppDialogBinding.inflate(LayoutInflater.from(context))
    }

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)

        window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.6f)
        }

        setupContent()
        setupButtons()
        setupBackground()
    }

    fun getInputText(): String = binding.editText.text.toString()

    private fun setupContent() = with(binding) {
        builder.title?.let {
            dialogTitle.text = it
        } ?: run {
            dialogTitle.visibility = View.GONE
            titleDivider.visibility = View.GONE
        }

        builder.message?.let {
            dialogMessage.text = it
        } ?: run {
            dialogMessage.visibility = View.GONE
        }

        if (builder.showInput) {
            editText.visibility = View.VISIBLE
            editText.setText(builder.inputText)
            editText.hint = builder.inputHint
            editText.requestFocus()
        }

        builder.customView?.let { customView ->
            (customView.parent as? ViewGroup)?.removeView(customView)
            customViewContainer.addView(customView)
            customViewContainer.visibility = View.VISIBLE
        }
    }

    private fun setupButtons() = with(binding) {
        val visibleButtons = listOf(
            configureButton(
                buttonPositive, builder.positiveButtonText, builder.positiveButtonListener
            ),
            configureButton(
                buttonNegative, builder.negativeButtonText, builder.negativeButtonListener
            ),
            configureButton(buttonNeutral, builder.neutralButtonText, builder.neutralButtonListener)
        ).count { it }

        buttonSpacer.visibility = if (visibleButtons >= 2) View.VISIBLE else View.GONE
        buttonContainer.visibility = if (visibleButtons == 0) View.GONE else View.VISIBLE
    }

    private fun configureButton(
        button: BaldButton, text: CharSequence?, listener: ((AppDialog) -> Unit)?
    ): Boolean {
        if (text == null) {
            button.visibility = View.GONE
            return false
        }
        button.apply {
            this.text = text
            visibility = View.VISIBLE
            setOnClickListener {
                listener?.invoke(this@AppDialog)
                if (builder.autoDismiss) dismiss()
            }
        }
        return true
    }

    private fun setupBackground() {
        builder.backgroundColor?.let {
            binding.dialogBackground.setBackgroundColor(it)
        }
        setCancelable(builder.cancelable)
    }

    class Builder(private val context: Context) {
        internal var title: CharSequence? = null
        internal var message: CharSequence? = null
        internal var customView: View? = null
        internal var positiveButtonText: CharSequence? = null
        internal var negativeButtonText: CharSequence? = null
        internal var neutralButtonText: CharSequence? = null
        internal var positiveButtonListener: ((AppDialog) -> Unit)? = null
        internal var negativeButtonListener: ((AppDialog) -> Unit)? = null
        internal var neutralButtonListener: ((AppDialog) -> Unit)? = null
        internal var cancelable: Boolean = true
        internal var autoDismiss: Boolean = true
        internal var showInput: Boolean = false
        internal var inputText: CharSequence? = null
        internal var inputHint: CharSequence? = null

        @ColorInt
        internal var backgroundColor: Int? = null

        fun setTitle(title: CharSequence) = apply { this.title = title }
        fun setTitle(@StringRes resId: Int) = setTitle(context.getText(resId))

        fun setMessage(message: CharSequence) = apply { this.message = message }
        fun setMessage(@StringRes resId: Int) = setMessage(context.getText(resId))

        fun setView(view: View) = apply { this.customView = view }

        fun setInput(show: Boolean = true, text: CharSequence? = null, hint: CharSequence? = null) =
            apply {
                this.showInput = show
                this.inputText = text
                this.inputHint = hint
            }

        fun setPositiveButton(text: CharSequence, listener: ((AppDialog) -> Unit)? = null) = apply {
            this.positiveButtonText = text
            this.positiveButtonListener = listener
        }

        fun setPositiveButton(@StringRes resId: Int, listener: ((AppDialog) -> Unit)? = null) =
            setPositiveButton(context.getText(resId), listener)

        fun setNegativeButton(text: CharSequence, listener: ((AppDialog) -> Unit)? = null) = apply {
            this.negativeButtonText = text
            this.negativeButtonListener = listener
        }

        fun setNegativeButton(@StringRes resId: Int, listener: ((AppDialog) -> Unit)? = null) =
            setNegativeButton(context.getText(resId), listener)

        fun setNeutralButton(text: CharSequence, listener: ((AppDialog) -> Unit)? = null) = apply {
            this.neutralButtonText = text
            this.neutralButtonListener = listener
        }

        fun setNeutralButton(@StringRes resId: Int, listener: ((AppDialog) -> Unit)? = null) =
            setNeutralButton(context.getText(resId), listener)

        fun setCancelable(cancelable: Boolean) = apply { this.cancelable = cancelable }
        fun setBackgroundColor(@ColorInt color: Int) = apply { this.backgroundColor = color }

        fun create(): AppDialog {
            val dialog = AppDialog(context, this)
            (context as? BaldActivity)?.autoDismiss(dialog)
            return dialog
        }

        fun show(): AppDialog = create().apply { show() }
    }
}

/** DSL wrapper for [AppDialog.Builder] */
fun Context.baldAlertDialog(init: AppDialog.Builder.() -> Unit): AppDialog {
    return AppDialog.Builder(this).apply(init).create()
}
