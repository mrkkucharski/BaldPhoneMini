package app.baldphone.neo.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

import androidx.core.content.edit

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

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

    /**
     * Controls whether audible feedback (DTMF tones) is played when interacting with the dialer.
     */
    var areDialerSoundsEnabled: Boolean by BooleanPreference(
        PrefKeys.KEY_DIALER_SOUNDS, PrefKeys.DEFAULT_DIALER_SOUNDS
    )

    /**
     * If true, the dialog for choosing a SIM will be shown when calling.
     */
    var isDualSimActive: Boolean by BooleanPreference(
        PrefKeys.KEY_DUAL_SIM_MODE, PrefKeys.DEFAULT_DUAL_SIM_MODE
    )

    /**
     * Controls whether call logs are expanded by default
     * in the [app.baldphone.neo.contacts.ui.details.ContactDetailsActivity].
     */
    var isCallLogVisible: Boolean by BooleanPreference(
        PrefKeys.KEY_CALL_LOG_VISIBLE, false
    )

    private fun putInt(key: String, value: Int) {
        prefs.edit { putInt(key, value) }
    }

    // Delegate for a Boolean preference
    private class BooleanPreference(
        private val key: String, private val defaultValue: Boolean
    ) : ReadWriteProperty<Prefs, Boolean> {

        override fun getValue(thisRef: Prefs, property: KProperty<*>): Boolean {
            val value = thisRef.prefs.getBoolean(key, defaultValue)
            Log.d(TAG, "Read $key: $value (default $defaultValue)")
            return value
        }

        override fun setValue(thisRef: Prefs, property: KProperty<*>, value: Boolean) {
            Log.d(TAG, "Setting $key to $value")
            thisRef.prefs.edit { putBoolean(key, value) }
        }
    }
}
