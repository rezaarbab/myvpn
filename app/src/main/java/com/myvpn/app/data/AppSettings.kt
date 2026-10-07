package com.myvpn.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppSettings {

    private const val PREFS = "settings"
    private const val KEY_THEME = "theme_mode"
    private const val KEY_AUTO_CONNECT = "auto_connect"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _themeMode.value = prefs.getString(KEY_THEME, "system") ?: "system"
        _autoConnect.value = prefs.getBoolean(KEY_AUTO_CONNECT, false)
    }

    // system | light | dark
    private val _themeMode = MutableStateFlow("system")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        prefs.edit().putString(KEY_THEME, mode).apply()
    }

    private val _autoConnect = MutableStateFlow(false)
    val autoConnect: StateFlow<Boolean> = _autoConnect.asStateFlow()

    fun setAutoConnect(value: Boolean) {
        _autoConnect.value = value
        prefs.edit().putBoolean(KEY_AUTO_CONNECT, value).apply()
    }
}
