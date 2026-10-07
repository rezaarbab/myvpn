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
    private const val KEY_SEED = "seed_color"
    private const val KEY_PURE_BLACK = "pure_black"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _themeMode.value = prefs.getString(KEY_THEME, "system") ?: "system"
        _autoConnect.value = prefs.getBoolean(KEY_AUTO_CONNECT, false)
        _seed.value = prefs.getString(KEY_SEED, DEFAULT_SEED) ?: DEFAULT_SEED
        _pureBlack.value = prefs.getBoolean(KEY_PURE_BLACK, false)
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

    /** seed رنگ تم — همان پالت‌های انتخابی FlClash، یا "dynamic" برای رنگوالپیپر دستگاه */
    private const val DEFAULT_SEED = "0xFFD8C0C3"

    private val _seed = MutableStateFlow(DEFAULT_SEED)
    val seed: StateFlow<String> = _seed.asStateFlow()

    fun setSeed(value: String) {
        _seed.value = value
        prefs.edit().putString(KEY_SEED, value).apply()
    }

    private val _pureBlack = MutableStateFlow(false)
    val pureBlack: StateFlow<Boolean> = _pureBlack.asStateFlow()

    fun setPureBlack(value: Boolean) {
        _pureBlack.value = value
        prefs.edit().putBoolean(KEY_PURE_BLACK, value).apply()
    }
}
