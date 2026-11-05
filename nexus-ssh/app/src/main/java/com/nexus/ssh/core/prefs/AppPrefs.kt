package com.nexus.ssh.core.prefs

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Small SharedPreferences wrapper that exposes every setting as a StateFlow. */
class AppPrefs(context: Context) {
    private val sp = context.getSharedPreferences("nexus_prefs", Context.MODE_PRIVATE)

    private val _displayName = MutableStateFlow(sp.getString("display_name", "") ?: "")
    val displayName: StateFlow<String> = _displayName

    private val _showWallpaper = MutableStateFlow(sp.getBoolean("wallpaper", true))
    val showWallpaper: StateFlow<Boolean> = _showWallpaper

    private val _accent = MutableStateFlow(sp.getString("accent", "blue") ?: "blue")
    val accent: StateFlow<String> = _accent

    private val _appLock = MutableStateFlow(sp.getBoolean("app_lock", false))
    val appLock: StateFlow<Boolean> = _appLock

    private val _secureScreen = MutableStateFlow(sp.getBoolean("secure_screen", false))
    val secureScreen: StateFlow<Boolean> = _secureScreen

    private val _terminalFontSize = MutableStateFlow(sp.getInt("term_font", 12))
    val terminalFontSize: StateFlow<Int> = _terminalFontSize

    private val _refreshSeconds = MutableStateFlow(sp.getInt("refresh_s", 5))
    val refreshSeconds: StateFlow<Int> = _refreshSeconds

    private val _showHidden = MutableStateFlow(sp.getBoolean("show_hidden", false))
    val showHiddenFiles: StateFlow<Boolean> = _showHidden

    private val _favorites = MutableStateFlow(loadFavorites())
    /** Custom favourite folders added by the user (absolute remote paths). */
    val favorites: StateFlow<List<String>> = _favorites

    private val _serverIcons = MutableStateFlow(loadIcons())
    val serverIcons: StateFlow<Map<Long, String>> = _serverIcons

    fun setDisplayName(v: String) { sp.edit().putString("display_name", v.trim()).apply(); _displayName.value = v.trim() }
    fun setShowWallpaper(v: Boolean) { sp.edit().putBoolean("wallpaper", v).apply(); _showWallpaper.value = v }
    fun setAccent(v: String) { sp.edit().putString("accent", v).apply(); _accent.value = v }
    fun setAppLock(v: Boolean) { sp.edit().putBoolean("app_lock", v).apply(); _appLock.value = v }
    fun setSecureScreen(v: Boolean) { sp.edit().putBoolean("secure_screen", v).apply(); _secureScreen.value = v }
    fun setTerminalFontSize(v: Int) { sp.edit().putInt("term_font", v).apply(); _terminalFontSize.value = v }
    fun setRefreshSeconds(v: Int) { sp.edit().putInt("refresh_s", v).apply(); _refreshSeconds.value = v }
    fun setShowHiddenFiles(v: Boolean) { sp.edit().putBoolean("show_hidden", v).apply(); _showHidden.value = v }

    fun addFavorite(path: String) {
        if (path.isBlank() || path in _favorites.value) return
        val list = _favorites.value + path
        sp.edit().putString("favorites", list.joinToString("\n")).apply()
        _favorites.value = list
    }

    fun removeFavorite(path: String) {
        val list = _favorites.value - path
        sp.edit().putString("favorites", list.joinToString("\n")).apply()
        _favorites.value = list
    }

    fun setServerIcon(serverId: Long, key: String) {
        sp.edit().putString("icon_$serverId", key).apply()
        _serverIcons.value = _serverIcons.value + (serverId to key)
    }

    private fun loadFavorites(): List<String> =
        (sp.getString("favorites", "") ?: "").split('\n').filter { it.isNotBlank() }

    private fun loadIcons(): Map<Long, String> {
        val out = HashMap<Long, String>()
        for ((k, v) in sp.all) {
            if (k.startsWith("icon_") && v is String) {
                k.removePrefix("icon_").toLongOrNull()?.let { out[it] = v }
            }
        }
        return out
    }
}
