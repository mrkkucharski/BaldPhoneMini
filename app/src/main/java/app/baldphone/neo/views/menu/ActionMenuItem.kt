package app.baldphone.neo.views.menu

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/**
 * Represents an item within an action menu:
 * buttons with icons ([Option]), checkboxes ([Toggle]), or visual separators ([Separator]).
 *
 * @property id A unique identifier for the action, used to handle clicks.
 * @property enabled Whether the action is currently interactable.
 */
sealed interface ActionMenuItem {
    val id: Int
    var enabled: Boolean

    data class Option(
        override val id: Int,
        @param:DrawableRes val iconRes: Int,
        @param:StringRes val labelRes: Int,
        val destructive: Boolean = false,
        override var enabled: Boolean = true
    ) : ActionMenuItem

    data class Toggle(
        override val id: Int,
        @param:DrawableRes val iconOnRes: Int,
        @param:DrawableRes val iconOffRes: Int,
        @param:StringRes val labelOnRes: Int,
        @param:StringRes val labelOffRes: Int,
        var checked: Boolean,
        override var enabled: Boolean = true
    ) : ActionMenuItem {
        val currentIconRes: Int
            get() = if (checked) iconOnRes else iconOffRes

        val currentLabelRes: Int
            get() = if (checked) labelOnRes else labelOffRes
    }

    data object Separator : ActionMenuItem {
        override val id = -1
        override var enabled = false
    }
}
