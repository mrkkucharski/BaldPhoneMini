package com.bald.uriah.baldphone.testutil

import android.content.SharedPreferences

class InMemorySharedPreferences(
    initialValues: Map<String, Any?> = emptyMap()
) : SharedPreferences {
    private val values = initialValues.toMutableMap()

    override fun getAll(): MutableMap<String, *> = values.toMutableMap()

    override fun getString(key: String, defValue: String?): String? =
        values[key] as? String ?: defValue

    override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? =
        (values[key] as? Set<*>)?.filterIsInstance<String>()?.toMutableSet() ?: defValues

    override fun getInt(key: String, defValue: Int): Int = values[key] as? Int ?: defValue

    override fun getLong(key: String, defValue: Long): Long = values[key] as? Long ?: defValue

    override fun getFloat(key: String, defValue: Float): Float = values[key] as? Float ?: defValue

    override fun getBoolean(key: String, defValue: Boolean): Boolean =
        values[key] as? Boolean ?: defValue

    override fun contains(key: String): Boolean = values.containsKey(key)

    override fun edit(): SharedPreferences.Editor = Editor()

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?
    ) = Unit

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?
    ) = Unit

    inner class Editor : SharedPreferences.Editor {
        private val staged = mutableMapOf<String, Any?>()
        private val removals = mutableSetOf<String>()
        private var clearRequested = false

        override fun putString(key: String, value: String?): SharedPreferences.Editor =
            apply { staged[key] = value }

        override fun putStringSet(
            key: String,
            values: MutableSet<String>?
        ): SharedPreferences.Editor = apply {
            staged[key] = values?.toMutableSet()
        }

        override fun putInt(key: String, value: Int): SharedPreferences.Editor =
            apply { staged[key] = value }

        override fun putLong(key: String, value: Long): SharedPreferences.Editor =
            apply { staged[key] = value }

        override fun putFloat(key: String, value: Float): SharedPreferences.Editor =
            apply { staged[key] = value }

        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor =
            apply { staged[key] = value }

        override fun remove(key: String): SharedPreferences.Editor =
            apply { removals += key }

        override fun clear(): SharedPreferences.Editor = apply {
            clearRequested = true
            staged.clear()
            removals.clear()
        }

        override fun commit(): Boolean {
            if (clearRequested) values.clear()
            removals.forEach(values::remove)
            staged.forEach { (key, value) ->
                if (value == null) values.remove(key) else values[key] = value
            }
            return true
        }

        override fun apply() {
            commit()
        }
    }
}
