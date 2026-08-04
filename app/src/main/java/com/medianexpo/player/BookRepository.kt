package com.medianexpo.player

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
<<<<<<< HEAD
=======
import android.os.Build
>>>>>>> 2c4ab1d (Initial commit after project recovery)
import android.os.Environment
import android.provider.MediaStore
import java.io.File

data class BookItem(
    val id: Long,
    val title: String,
    val path: String,
    val contentUri: Uri,
    val isAudiobook: Boolean,
    val sizeBytes: Long
)

data class AudiobookChapter(
    val title: String,
    val uri: Uri,
    val durationMs: Long
)

object BookRepository {

    private val audioExtensions = setOf("mp3", "m4a", "m4b", "aac", "flac", "ogg", "wav", "opus")

    fun isAudioFile(file: File): Boolean {
        val ext = file.extension.lowercase()
        return ext in audioExtensions
    }

<<<<<<< HEAD
    fun scanEpubFiles(context: Context): List<BookItem> {
        val list = mutableListOf<BookItem>()
        // MediaStore documents / downloads that look like epub
        try {
            val collection = MediaStore.Files.getContentUri("external")
            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.MIME_TYPE
            )
            val selection = "${MediaStore.Files.FileColumns.MIME_TYPE}=? OR ${MediaStore.Files.FileColumns.DATA} LIKE ?"
            val args = arrayOf("application/epub+zip", "%.epub")
            context.contentResolver.query(collection, projection, selection, args, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(nameCol) ?: "EPUB"
                    val path = cursor.getString(dataCol) ?: continue
                    if (!path.lowercase().endsWith(".epub")) continue
                    val size = cursor.getLong(sizeCol)
                    val uri = ContentUris.withAppendedId(collection, id)
                    list.add(BookItem(id, title.removeSuffix(".epub"), path, uri, false, size))
                }
            }
        } catch (_: Exception) { }

        // Manual scan of common folders
        val roots = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            File(Environment.getExternalStorageDirectory(), "Books"),
            File(Environment.getExternalStorageDirectory(), "eBooks")
        )
        roots.forEach { root ->
            if (root.exists() && root.isDirectory) {
                root.walkTopDown().maxDepth(3).forEach { file ->
                    if (file.isFile && file.extension.lowercase() == "epub") {
                        val already = list.any { it.path == file.absolutePath }
                        if (!already) {
                            list.add(
                                BookItem(
                                    id = file.absolutePath.hashCode().toLong(),
                                    title = file.nameWithoutExtension,
                                    path = file.absolutePath,
                                    contentUri = Uri.fromFile(file),
                                    isAudiobook = false,
                                    sizeBytes = file.length()
                                )
                            )
                        }
                    }
                }
            }
        }
        return list
=======
    /**
     * Find EPUB files via MediaStore + filesystem walk.
     * Does not require the DATA column (often null on Android 10+).
     */
    fun scanEpubFiles(context: Context): List<BookItem> {
        val list = mutableListOf<BookItem>()
        val seen = mutableSetOf<String>()

        fun add(id: Long, title: String, path: String, uri: Uri, size: Long) {
            val key = uri.toString()
            if (!seen.add(key)) return
            val cleanTitle = title
                .removeSuffix(".epub")
                .removeSuffix(".EPUB")
                .ifBlank { "EPUB" }
            list.add(BookItem(id, cleanTitle, path.ifBlank { cleanTitle }, uri, false, size))
        }

        // 1) MediaStore.Files — match mime OR display name
        queryEpubs(
            context,
            MediaStore.Files.getContentUri("external"),
            ::add
        )

        // 2) Downloads collection (API 29+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                queryEpubs(context, MediaStore.Downloads.EXTERNAL_CONTENT_URI, ::add)
            } catch (_: Exception) { }
        }

        // 3) Filesystem roots (works when storage permission / legacy path access is available)
        val roots = mutableListOf<File>()
        try {
            roots += Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            roots += Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            roots += File(Environment.getExternalStorageDirectory(), "Books")
            roots += File(Environment.getExternalStorageDirectory(), "books")
            roots += File(Environment.getExternalStorageDirectory(), "eBooks")
            roots += File(Environment.getExternalStorageDirectory(), "Ebook")
            roots += File(Environment.getExternalStorageDirectory(), "Ebooks")
            roots += File(Environment.getExternalStorageDirectory(), "Novels")
            context.getExternalFilesDir(null)?.let { roots += it }
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)?.let { roots += it }
            // App-specific + shared storage tree
            Environment.getExternalStorageDirectory()?.let { roots += it }
        } catch (_: Exception) { }

        roots.distinct().forEach { root ->
            try {
                if (!root.exists() || !root.isDirectory) return@forEach
                // Cap depth so full-storage walk stays reasonable
                val depth = if (root.absolutePath == Environment.getExternalStorageDirectory()?.absolutePath) 4 else 6
                root.walkTopDown().maxDepth(depth).forEach { file ->
                    if (!file.isFile) return@forEach
                    if (file.extension.lowercase() != "epub") return@forEach
                    try {
                        // Prefer a content-style file URI; reader opens via contentResolver
                        val uri = Uri.fromFile(file)
                        add(
                            id = file.absolutePath.hashCode().toLong() and 0x7FFFFFFFFFFFFFFFL,
                            title = file.nameWithoutExtension,
                            path = file.absolutePath,
                            uri = uri,
                            size = file.length()
                        )
                    } catch (_: Exception) { }
                }
            } catch (_: Exception) { }
        }

        return list
    }

    private fun queryEpubs(
        context: Context,
        collection: Uri,
        add: (Long, String, String, Uri, Long) -> Unit
    ) {
        try {
            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.DATA
            )
            // epub mime, generic octet-stream named .epub, or name ends with .epub
            val selection = (
                "${MediaStore.MediaColumns.MIME_TYPE}=? OR " +
                    "${MediaStore.MediaColumns.MIME_TYPE}=? OR " +
                    "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"
                )
            val args = arrayOf(
                "application/epub+zip",
                "application/octet-stream",
                "%.epub"
            )
            context.contentResolver.query(
                collection,
                projection,
                selection,
                args,
                "${MediaStore.MediaColumns.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                val mimeCol = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                val dataCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                if (idCol < 0 || nameCol < 0) return
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameCol) ?: continue
                    val mime = if (mimeCol >= 0) cursor.getString(mimeCol) ?: "" else ""
                    val lower = name.lowercase()
                    val isEpub = lower.endsWith(".epub") ||
                        mime.equals("application/epub+zip", ignoreCase = true)
                    if (!isEpub) continue
                    // Skip random octet-stream files that aren't epub-named
                    if (!lower.endsWith(".epub") && mime.equals("application/octet-stream", ignoreCase = true)) {
                        continue
                    }
                    val id = cursor.getLong(idCol)
                    val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                    val path = if (dataCol >= 0) cursor.getString(dataCol) ?: "" else ""
                    val uri = ContentUris.withAppendedId(collection, id)
                    add(id, name, path, uri, size)
                }
            }
        } catch (_: Exception) { }
>>>>>>> 2c4ab1d (Initial commit after project recovery)
    }

    fun scanAudiobookFolders(context: Context): List<BookItem> {
        val list = mutableListOf<BookItem>()
        val roots = listOf(
            File(Environment.getExternalStorageDirectory(), "Audiobooks"),
            File(Environment.getExternalStorageDirectory(), "AudioBooks"),
<<<<<<< HEAD
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "Audiobooks"),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Audiobooks")
=======
            File(Environment.getExternalStorageDirectory(), "Audiobook"),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "Audiobooks"),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Audiobooks"),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Audiobooks")
>>>>>>> 2c4ab1d (Initial commit after project recovery)
        )
        roots.forEach { root ->
            if (!root.exists() || !root.isDirectory) return@forEach
            root.listFiles()?.filter { it.isDirectory }?.forEach { folder ->
                val hasAudio = folder.listFiles()?.any { isAudioFile(it) } == true
                if (hasAudio) {
                    list.add(
                        BookItem(
<<<<<<< HEAD
                            id = folder.absolutePath.hashCode().toLong(),
=======
                            id = folder.absolutePath.hashCode().toLong() and 0x7FFFFFFFFFFFFFFFL,
>>>>>>> 2c4ab1d (Initial commit after project recovery)
                            title = folder.name,
                            path = folder.absolutePath,
                            contentUri = Uri.fromFile(folder),
                            isAudiobook = true,
                            sizeBytes = folder.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                        )
                    )
                }
            }
<<<<<<< HEAD
            // Also treat root itself if it contains audio files directly
=======
>>>>>>> 2c4ab1d (Initial commit after project recovery)
            val directAudio = root.listFiles()?.filter { isAudioFile(it) } ?: emptyList()
            if (directAudio.isNotEmpty()) {
                list.add(
                    BookItem(
<<<<<<< HEAD
                        id = root.absolutePath.hashCode().toLong(),
=======
                        id = root.absolutePath.hashCode().toLong() and 0x7FFFFFFFFFFFFFFFL,
>>>>>>> 2c4ab1d (Initial commit after project recovery)
                        title = root.name,
                        path = root.absolutePath,
                        contentUri = Uri.fromFile(root),
                        isAudiobook = true,
                        sizeBytes = directAudio.sumOf { it.length() }
                    )
                )
            }
        }
        return list
    }

    fun getAudiobookChapters(context: Context, bookPath: String): List<AudiobookChapter> {
        val folder = File(bookPath)
        if (!folder.exists()) return emptyList()
<<<<<<< HEAD
        val files = folder.listFiles()?.filter { isAudioFile(it) }?.sortedBy { it.name.lowercase() } ?: return emptyList()
=======
        val files = folder.listFiles()?.filter { isAudioFile(it) }?.sortedBy { it.name.lowercase() }
            ?: return emptyList()
>>>>>>> 2c4ab1d (Initial commit after project recovery)
        return files.map { file ->
            var duration = 0L
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(file.absolutePath)
                duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                retriever.release()
            } catch (_: Exception) { }
            AudiobookChapter(
                title = file.nameWithoutExtension,
                uri = Uri.fromFile(file),
                durationMs = duration
            )
        }
    }
}
