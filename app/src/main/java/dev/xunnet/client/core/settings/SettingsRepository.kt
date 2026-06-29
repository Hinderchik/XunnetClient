package dev.xunnet.client.core.settings

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistent app settings backed by SharedPreferences.
 * Each setting has a getter and a setter that writes through immediately.
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("xunnet_settings", Context.MODE_PRIVATE)

    enum class ThemeMode { SYSTEM, LIGHT, DARK }
    enum class Language { SYSTEM, RUSSIAN, ENGLISH }

    var autoConnect: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CONNECT, false)
        set(value) { prefs.edit().putBoolean(KEY_AUTO_CONNECT, value).apply() }

    var killSwitch: Boolean
        get() = prefs.getBoolean(KEY_KILL_SWITCH, true)
        set(value) { prefs.edit().putBoolean(KEY_KILL_SWITCH, value).apply() }

    var blockQuic: Boolean
        get() = prefs.getBoolean(KEY_BLOCK_QUIC, false)
        set(value) { prefs.edit().putBoolean(KEY_BLOCK_QUIC, value).apply() }

    var disconnectOnSleep: Boolean
        get() = prefs.getBoolean(KEY_DISCONNECT_SLEEP, false)
        set(value) { prefs.edit().putBoolean(KEY_DISCONNECT_SLEEP, value).apply() }

    var splitTunnelingPreset: String
        get() = prefs.getString(KEY_SPLIT_PRESET, "default") ?: "default"
        set(value) { prefs.edit().putString(KEY_SPLIT_PRESET, value).apply() }

    var themeMode: ThemeMode
        get() = ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
        set(value) { prefs.edit().putString(KEY_THEME, value.name).apply() }

    var language: Language
        get() = Language.valueOf(prefs.getString(KEY_LANGUAGE, Language.SYSTEM.name) ?: Language.SYSTEM.name)
        set(value) { prefs.edit().putString(KEY_LANGUAGE, value.name).apply() }

    /**
     * Selected launcher icon alias index. -1 = use default (MainActivity).
     * Each alias in the manifest gets a sequential index.
     */
    var selectedIconAlias: Int
        get() = prefs.getInt(KEY_ICON_ALIAS, -1)
        set(value) { prefs.edit().putInt(KEY_ICON_ALIAS, value).apply() }

    companion object {
        private const val KEY_AUTO_CONNECT = "auto_connect"
        private const val KEY_KILL_SWITCH = "kill_switch"
        private const val KEY_BLOCK_QUIC = "block_quic"
        private const val KEY_DISCONNECT_SLEEP = "disconnect_on_sleep"
        private const val KEY_SPLIT_PRESET = "split_preset"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_ICON_ALIAS = "icon_alias"
    }
}
