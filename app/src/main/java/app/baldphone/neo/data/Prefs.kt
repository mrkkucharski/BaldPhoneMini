package app.baldphone.neo.data

import android.content.Context
import android.content.SharedPreferences

import androidx.core.content.edit

object Prefs {
    private const val TAG = "Prefs"

    private lateinit var prefs: SharedPreferences

    @JvmStatic
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PrefKeys.PREFS_NAME, Context.MODE_PRIVATE)

        val version = prefs.getInt(PrefKeys.PREFS_VERSION_KEY, 0)
        if (version < PrefKeys.CURRENT_PREFS_VERSION) {
            prefs.edit { putInt(PrefKeys.PREFS_VERSION_KEY, PrefKeys.CURRENT_PREFS_VERSION) }
        }
    }

    fun setTheme(theme: Int) {
        putInt(PrefKeys.THEME_KEY, theme)
    }

    fun getTheme(): Int? {
        return if (prefs.contains(PrefKeys.THEME_KEY)) {
            prefs.getInt(PrefKeys.THEME_KEY, 0) // default is not used but required
        } else {
            null
        }
    }

    private fun putInt(key: String, value: Int) {
        prefs.edit { putInt(key, value) }
    }
}
