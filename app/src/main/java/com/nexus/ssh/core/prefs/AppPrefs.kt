package com.nexus.ssh.core.prefs

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class Snippet(val id: Long, val title: String, val command: String)

data class WebBookmark(val serverId: Long, val label: String, val input: String)

/** Small SharedPreferences wrapper that exposes every setting as a StateFlow. */
class AppPrefs(context: Context) {
    private val sp = context.getSharedPreferences("nexus_prefs", Context.MODE_PRIVATE)

    private val _displayName = MutableStateFlow(sp.getString("display_name", "") ?: "")
    val displayName: StateFlow<String> = _displayName

    private val _showWallpaper = MutableStateFlow(sp.getBoolean("wallpaper", true))
    val showWallpaper: StateFlow<Boolean> = _showWallpaper

    private val _accent = MutableStateFlow(sp.getString("accent", "green") ?: "green")
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

    private val _tileOrder = MutableStateFlow(
        (sp.getString("tile_order", "") ?: "").split(',').filter { it.isNotBlank() }
    )
    /** User-defined order of the Home quick-access tiles (route keys). */
    val homeTileOrder: StateFlow<List<String>> = _tileOrder

    private val _snippets = MutableStateFlow(loadSnippets())
    val snippets: StateFlow<List<Snippet>> = _snippets

    private val _bookmarks = MutableStateFlow(loadBookmarks())
    val webBookmarks: StateFlow<List<WebBookmark>> = _bookmarks

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

    fun setHomeTileOrder(keys: List<String>) {
        sp.edit().putString("tile_order", keys.joinToString(",")).apply()
        _tileOrder.value = keys
    }

    fun resetHomeTileOrder() = setHomeTileOrder(emptyList())

    fun saveSnippet(id: Long?, title: String, command: String) {
        val clean = Snippet(id ?: System.currentTimeMillis(), title.trim().replace('\n', ' '), command.trim().replace('\n', ' '))
        val list = if (id == null) _snippets.value + clean else _snippets.value.map { if (it.id == id) clean else it }
        persistSnippets(list)
    }

    fun removeSnippet(id: Long) = persistSnippets(_snippets.value.filterNot { it.id == id })

    private fun persistSnippets(list: List<Snippet>) {
        val raw = list.joinToString("\n") { "${it.id}\u001f${it.title}\u001f${it.command}" }
        sp.edit().putString("snippets", raw).apply()
        _snippets.value = list
    }

    private fun loadSnippets(): List<Snippet> =
        (sp.getString("snippets", "") ?: "").split('\n').mapNotNull { line ->
            val p = line.split('\u001f')
            val id = p.getOrNull(0)?.toLongOrNull()
            if (p.size == 3 && id != null) Snippet(id, p[1], p[2]) else null
        }

    fun addBookmark(b: WebBookmark) {
        if (_bookmarks.value.any { it.serverId == b.serverId && it.input == b.input }) return
        saveBookmarks(_bookmarks.value + b)
    }

    fun removeBookmark(b: WebBookmark) {
        saveBookmarks(_bookmarks.value.filterNot { it.serverId == b.serverId && it.input == b.input })
    }

    private fun saveBookmarks(list: List<WebBookmark>) {
        val raw = list.joinToString("\n") { "${it.serverId}\u001f${it.label.replace('\n', ' ')}\u001f${it.input.replace('\n', ' ')}" }
        sp.edit().putString("web_bookmarks", raw).apply()
        _bookmarks.value = list
    }

    private fun loadBookmarks(): List<WebBookmark> =
        (sp.getString("web_bookmarks", "") ?: "").split('\n').mapNotNull { line ->
            val p = line.split('\u001f')
            val id = p.getOrNull(0)?.toLongOrNull()
            if (p.size == 3 && id != null) WebBookmark(id, p[1], p[2]) else null
        }

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
