package com.myvpn.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * ذخیره‌ی سرورها و سرور انتخاب‌شده در SharedPreferences.
 * در فاز ۲ به‌جای دستی، از سابسکریپشن کلادفلر پر می‌شود.
 */
object ProfileStore {

    private const val PREFS = "profiles"
    private const val KEY_LIST = "list"
    private const val KEY_SELECTED = "selected"

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private lateinit var prefs: SharedPreferences

    private val _profiles = MutableStateFlow<List<ServerProfile>>(emptyList())
    val profiles: StateFlow<List<ServerProfile>> = _profiles.asStateFlow()

    private val _selectedId = MutableStateFlow<String?>(null)
    val selectedId: StateFlow<String?> = _selectedId.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_LIST, null)
        val list = if (raw.isNullOrBlank()) {
            emptyList()
        } else {
            runCatching { json.decodeFromString(ListSerializer(ServerProfile.serializer()), raw) }
                .getOrDefault(emptyList())
        }
        _profiles.value = list
        _selectedId.value = prefs.getString(KEY_SELECTED, null)
            ?.takeIf { id -> list.any { it.id == id } }
            ?: list.firstOrNull()?.id
    }

    fun save(profile: ServerProfile) {
        val current = _profiles.value.toMutableList()
        val index = current.indexOfFirst { it.id == profile.id }
        if (index >= 0) current[index] = profile else current.add(profile)
        persist(current)
        if (_selectedId.value == null) select(profile.id)
    }

    fun delete(id: String) {
        val current = _profiles.value.filterNot { it.id == id }
        persist(current)
        if (_selectedId.value == id) {
            _selectedId.value = current.firstOrNull()?.id
            prefs.edit().putString(KEY_SELECTED, _selectedId.value).apply()
        }
    }

    fun select(id: String) {
        _selectedId.value = id
        prefs.edit().putString(KEY_SELECTED, id).apply()
    }

    fun selected(): ServerProfile? {
        val id = _selectedId.value ?: return _profiles.value.firstOrNull()
        return _profiles.value.firstOrNull { it.id == id } ?: _profiles.value.firstOrNull()
    }

    private fun persist(list: List<ServerProfile>) {
        _profiles.value = list
        prefs.edit().putString(KEY_LIST, json.encodeToString(ListSerializer(ServerProfile.serializer()), list)).apply()
    }
}
