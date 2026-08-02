package com.medianexpo.player

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
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
    }

    fun scanAudiobookFolders(context: Context): List<BookItem> {
        val list = mutableListOf<BookItem>()
        val roots = listOf(
            File(Environment.getExternalStorageDirectory(), "Audiobooks"),
            File(Environment.getExternalStorageDirectory(), "AudioBooks"),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "Audiobooks"),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Audiobooks")
        )
        roots.forEach { root ->
            if (!root.exists() || !root.isDirectory) return@forEach
            root.listFiles()?.filter { it.isDirectory }?.forEach { folder ->
                val hasAudio = folder.listFiles()?.any { isAudioFile(it) } == true
                if (hasAudio) {
                    list.add(
                        BookItem(
                            id = folder.absolutePath.hashCode().toLong(),
                            title = folder.name,
                            path = folder.absolutePath,
                            contentUri = Uri.fromFile(folder),
                            isAudiobook = true,
                            sizeBytes = folder.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                        )
                    )
                }
            }
            // Also treat root itself if it contains audio files directly
            val directAudio = root.listFiles()?.filter { isAudioFile(it) } ?: emptyList()
            if (directAudio.isNotEmpty()) {
                list.add(
                    BookItem(
                        id = root.absolutePath.hashCode().toLong(),
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
        val files = folder.listFiles()?.filter { isAudioFile(it) }?.sortedBy { it.name.lowercase() } ?: return emptyList()
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
