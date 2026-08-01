package com.gallery.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HiddenAlbumsStore @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("hidden_albums", Context.MODE_PRIVATE)
    private val _hiddenKeys = MutableStateFlow(loadKeys())
    val hiddenKeys: StateFlow<Set<String>> = _hiddenKeys.asStateFlow()

    fun setHidden(key: String, hidden: Boolean) {
        val next = _hiddenKeys.value.toMutableSet().also {
            if (hidden) it.add(key) else it.remove(key)
        }
        prefs.edit().putStringSet(PREF_KEY, next).apply()
        _hiddenKeys.value = next
    }

    private fun loadKeys(): Set<String> =
        prefs.getStringSet(PREF_KEY, emptySet())?.toSet() ?: emptySet()

    companion object {
        private const val PREF_KEY = "keys"
    }
}
