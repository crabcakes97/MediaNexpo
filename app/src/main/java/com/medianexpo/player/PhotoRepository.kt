package com.medianexpo.player

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

data class PhotoItem(
    val id: Long,
    val title: String,
    val path: String,
    val contentUri: Uri,
    val sizeBytes: Long,
    val dateAdded: Long
)

object PhotoRepository {
    fun scanLocalPhotos(context: Context): List<PhotoItem> {
        val list = mutableListOf<PhotoItem>()
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        try {
            context.contentResolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(nameCol) ?: "Photo"
                    val path = cursor.getString(dataCol) ?: ""
                    val size = cursor.getLong(sizeCol)
                    val date = cursor.getLong(dateCol)
                    val uri = ContentUris.withAppendedId(collection, id)
                    list.add(PhotoItem(id, title, path, uri, size, date))
                }
            }
        } catch (_: SecurityException) {
        } catch (_: Exception) {
        }
        return list
    }
}
