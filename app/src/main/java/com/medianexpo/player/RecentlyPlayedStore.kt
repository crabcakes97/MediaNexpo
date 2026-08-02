package com.medianexpo.player

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

/**
 * Drop-in recently-played history (last 100 tracks).
 * Call [record] when a track starts; read [load] for the UI list.
 */
object RecentlyPlayedStore {
    private const val PREFS = "recently_played"
    private const val KEY = "entries"
    private const val MAX = 100

    data class Entry(
        val uri: String,
        val title: String,
        val artist: String,
        val playedAt: Long
    )

    fun record(context: Context, uri: Uri, title: String, artist: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val list = load(context).toMutableList()
        list.removeAll { it.uri == uri.toString() }
        list.add(0, Entry(uri.toString(), title, artist, System.currentTimeMillis()))
        while (list.size > MAX) list.removeAt(list.lastIndex)
        val arr = JSONArray()
        list.forEach { e ->
            arr.put(JSONObject().apply {
                put("uri", e.uri)
                put("title", e.title)
                put("artist", e.artist)
                put("playedAt", e.playedAt)
            })
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    fun load(context: Context): List<Entry> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        Entry(
                            uri = o.getString("uri"),
                            title = o.optString("title", "Unknown"),
                            artist = o.optString("artist", "Unknown"),
                            playedAt = o.optLong("playedAt", 0L)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply()
    }
}
