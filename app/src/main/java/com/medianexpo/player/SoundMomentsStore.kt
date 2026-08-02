package com.medianexpo.player

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Sound Moments — a feature most players don't have.
 * Capture a timestamp + optional note while a song plays ("this drop", "favorite verse"),
 * then jump back to that exact moment later from the Moments list.
 */
object SoundMomentsStore {
    private const val PREFS = "sound_moments"
    private const val KEY = "moments"

    data class Moment(
        val id: String,
        val uri: String,
        val trackTitle: String,
        val trackArtist: String,
        val positionMs: Long,
        val note: String,
        val createdAt: Long
    )

    fun add(
        context: Context,
        uri: Uri,
        title: String,
        artist: String,
        positionMs: Long,
        note: String
    ) {
        val list = load(context).toMutableList()
        list.add(
            0,
            Moment(
                id = UUID.randomUUID().toString(),
                uri = uri.toString(),
                trackTitle = title,
                trackArtist = artist,
                positionMs = positionMs.coerceAtLeast(0L),
                note = note.trim().ifEmpty { "Moment @ ${formatTime(positionMs)}" },
                createdAt = System.currentTimeMillis()
            )
        )
        // Keep last 200 moments
        while (list.size > 200) list.removeAt(list.lastIndex)
        save(context, list)
    }

    fun remove(context: Context, id: String) {
        val list = load(context).filterNot { it.id == id }
        save(context, list)
    }

    fun load(context: Context): List<Moment> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        Moment(
                            id = o.getString("id"),
                            uri = o.getString("uri"),
                            trackTitle = o.optString("trackTitle", "Unknown"),
                            trackArtist = o.optString("trackArtist", ""),
                            positionMs = o.optLong("positionMs", 0L),
                            note = o.optString("note", ""),
                            createdAt = o.optLong("createdAt", 0L)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun save(context: Context, list: List<Moment>) {
        val arr = JSONArray()
        list.forEach { m ->
            arr.put(JSONObject().apply {
                put("id", m.id)
                put("uri", m.uri)
                put("trackTitle", m.trackTitle)
                put("trackArtist", m.trackArtist)
                put("positionMs", m.positionMs)
                put("note", m.note)
                put("createdAt", m.createdAt)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, arr.toString())
            .apply()
    }

    private fun formatTime(ms: Long): String {
        val totalSec = (ms / 1000).toInt().coerceAtLeast(0)
        val m = totalSec / 60
        val s = totalSec % 60
        return "%d:%02d".format(m, s)
    }
}
