package com.myvpn.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * مصرف تجمعی کل (جمع همه‌ی نشست‌ها) با قابلیت ریست.
 */
object TrafficStore {

    private const val PREFS = "traffic"
    private const val KEY_DOWN = "total_down"
    private const val KEY_UP = "total_up"

    private lateinit var prefs: SharedPreferences

    private val _totalDown = MutableStateFlow(0L)
    val totalDown: StateFlow<Long> = _totalDown.asStateFlow()

    private val _totalUp = MutableStateFlow(0L)
    val totalUp: StateFlow<Long> = _totalUp.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _totalDown.value = prefs.getLong(KEY_DOWN, 0L)
        _totalUp.value = prefs.getLong(KEY_UP, 0L)
    }

    fun addSession(down: Long, up: Long) {
        _totalDown.value += down
        _totalUp.value += up
        prefs.edit()
            .putLong(KEY_DOWN, _totalDown.value)
            .putLong(KEY_UP, _totalUp.value)
            .apply()
    }

    fun reset() {
        _totalDown.value = 0L
        _totalUp.value = 0L
        prefs.edit().clear().apply()
    }
}
