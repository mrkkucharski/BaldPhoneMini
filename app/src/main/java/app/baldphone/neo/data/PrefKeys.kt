package app.baldphone.neo.data

/**
 * A centralized object for storing all SharedPreferences keys and their default values.
 */
object PrefKeys {
    // General Prefs
    const val PREFS_NAME = "baldPrefs"
    const val PREFS_VERSION_KEY = "prefs_version"
    const val CURRENT_PREFS_VERSION = 1

    // Theme
    const val THEME_KEY = "theme"

    // Dialer
    const val KEY_DIALER_SOUNDS = "DIALER_SOUNDS_KEY"
    const val DEFAULT_DIALER_SOUNDS = true

    const val KEY_DUAL_SIM_MODE = "DUAL_SIM_KEY"
    const val DEFAULT_DUAL_SIM_MODE = false

}
