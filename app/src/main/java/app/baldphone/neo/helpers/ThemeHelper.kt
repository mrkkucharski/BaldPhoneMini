package app.baldphone.neo.helpers

import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import app.baldphone.neo.data.Prefs

object ThemeHelper {
    private const val TAG = "ThemeHelper"

    enum class Theme(val value: Int) {
        SYSTEM(0), LIGHT(1), DARK(2);

        companion object {
            fun fromValue(value: Int): Theme? {
                return entries.find { it.value == value }
            }
        }
    }

    /**
     * Applies the theme that is currently saved in SharedPreferences.
     * This is used to set the theme at the start of an activity.
     */
    fun applySavedTheme() {
        applyTheme(getSavedTheme())
    }

    /**
     * Applies the given theme and saves it to SharedPreferences.
     */
    fun setTheme(theme: Theme) {
        Prefs.setTheme(theme.value)
        applyTheme(theme)
    }

    /**
     * Retrieves the currently persisted theme from SharedPreferences.
     */
    fun getSavedTheme(): Theme {
        val savedValue = Prefs.getTheme()
        return savedValue?.let { Theme.fromValue(it) } ?: getDefaultThemeForDevice()
    }

    private fun getDefaultThemeForDevice(): Theme {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Theme.SYSTEM // Default for modern Android (API 29+)
        } else {
            Theme.LIGHT  // Legacy default
        }
    }

    private fun applyTheme(theme: Theme) {
        @AppCompatDelegate.NightMode val mode = when (theme) {
            Theme.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            Theme.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            Theme.DARK -> AppCompatDelegate.MODE_NIGHT_YES
        }
        if (AppCompatDelegate.getDefaultNightMode() != mode) {
            Log.d(TAG, "Applying theme mode: $mode")
            AppCompatDelegate.setDefaultNightMode(mode)
        }
    }
}
