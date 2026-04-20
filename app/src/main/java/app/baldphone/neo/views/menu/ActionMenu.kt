package app.baldphone.neo.views.menu

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.PopupWindow

import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager

import com.bald.uriah.baldphone.databinding.ViewActionPopupBinding

/**
 * A custom popup window used to display a list of menu actions: options, toggles, and separators.
 */
class ActionMenu(
    context: Context,
    actionMenuItems: List<ActionMenuItem>,
    private val listener: ActionMenuListener
) {

    companion object {
        private const val ID_CANCEL = -2
    }

    private val binding = ViewActionPopupBinding.inflate(LayoutInflater.from(context))
    private val items = actionMenuItems + listOf(
        ActionMenuItem.Separator, ActionMenuItem.Option(
            id = ID_CANCEL,
            iconRes = android.R.drawable.ic_menu_close_clear_cancel,
            labelRes = android.R.string.cancel
        )
    )
    private val adapter = ActionMenuAdapter(items) { item -> onMenuItemClicked(item) }

    private val popupWindow = PopupWindow(
        binding.root,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        true
    ).apply {
        elevation = 8f
        setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        isOutsideTouchable = true

        binding.recycler.apply {
            adapter = this@ActionMenu.adapter
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }

    /** Displays the action menu popup anchored to the specified view. */
    fun show(anchor: View) {
        if (!anchor.isAttachedToWindow) return
        popupWindow.showAsDropDown(anchor)
    }

    /** Dismisses the popup window if it is currently showing. */
    fun dismiss() {
        if (popupWindow.isShowing) popupWindow.dismiss()
    }

    private fun onMenuItemClicked(actionMenuItem: ActionMenuItem) {
        when {
            actionMenuItem.id == ID_CANCEL -> dismiss()

            actionMenuItem is ActionMenuItem.Option -> {
                listener.onActionClicked(actionMenuItem)
                dismiss()
            }

            actionMenuItem is ActionMenuItem.Toggle -> {
                actionMenuItem.checked = !actionMenuItem.checked
                listener.onActionClicked(actionMenuItem)
                dismiss()
            }
        }
    }
}
