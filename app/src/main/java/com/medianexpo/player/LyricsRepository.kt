package com.medianexpo.player

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Drop-in LRC lyrics loader.
 * Looks for a .lrc file with the same base name next to the audio
 * (file:// path or same folder via MediaStore DATA path).
 */
object LyricsRepository {

    data class Line(val timeMs: Long, val text: String)

    fun loadForUri(context: Context, audioUri: Uri): List<Line> {
        // 1) Same path with .lrc extension (file URIs / paths)
        resolveSiblingLrcPath(context, audioUri)?.let { path ->
            val f = File(path)
            if (f.exists() && f.canRead()) {
                return parseLrc(f.readText())
            }
        }
        // 2) Try content resolver open on sibling is not always possible;
        //    scan parent folder if we have a filesystem path
        val path = queryDisplayPath(context, audioUri)
        if (path != null) {
            val parent = File(path).parentFile
            val base = File(path).nameWithoutExtension
            if (parent != null) {
                val candidates = listOf(
                    File(parent, "$base.lrc"),
                    File(parent, "$base.LRC"),
                    File(parent, "${base}.txt")
                )
                for (c in candidates) {
                    if (c.exists() && c.canRead()) {
                        val text = c.readText()
                        if (c.extension.equals("txt", true) && !text.contains("[")) continue
                        return parseLrc(text)
                    }
                }
            }
        }
        return emptyList()
    }

    private fun resolveSiblingLrcPath(context: Context, uri: Uri): String? {
        if (uri.scheme == "file") {
            val path = uri.path ?: return null
            return path.substringBeforeLast('.') + ".lrc"
        }
        val data = queryDisplayPath(context, uri) ?: return null
        return data.substringBeforeLast('.') + ".lrc"
    }

    private fun queryDisplayPath(context: Context, uri: Uri): String? {
        return try {
            when (uri.scheme) {
                "file" -> uri.path
                "content" -> {
                    context.contentResolver.query(uri, arrayOf("_data"), null, null, null)?.use { c ->
                        if (c.moveToFirst()) {
                            val idx = c.getColumnIndex("_data")
                            if (idx >= 0) c.getString(idx) else null
                        } else null
                    }
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun parseLrc(raw: String): List<Line> {
        val lines = mutableListOf<Line>()
        // [mm:ss.xx] text  or  [mm:ss.xxx]
        val re = Regex("""\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?\]\s*(.*)""")
        raw.lineSequence().forEach { line ->
            val m = re.find(line.trim()) ?: return@forEach
            val min = m.groupValues[1].toLongOrNull() ?: return@forEach
            val sec = m.groupValues[2].toLongOrNull() ?: return@forEach
            val frac = m.groupValues[3]
            val ms = when {
                frac.isEmpty() -> 0L
                frac.length == 1 -> frac.toLong() * 100
                frac.length == 2 -> frac.toLong() * 10
                else -> frac.take(3).toLong()
            }
            val text = m.groupValues[4].trim()
            if (text.isNotEmpty()) {
                lines.add(Line(min * 60_000 + sec * 1_000 + ms, text))
            }
        }
        return lines.sortedBy { it.timeMs }
    }

    fun lineAt(lines: List<Line>, positionMs: Long): Int {
        if (lines.isEmpty()) return -1
        var idx = -1
        for (i in lines.indices) {
            if (lines[i].timeMs <= positionMs) idx = i else break
        }
        return idx
    }
}
