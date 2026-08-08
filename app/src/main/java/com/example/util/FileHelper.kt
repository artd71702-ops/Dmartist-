package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.InputStream

data class FileAttachmentData(
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val size: Long,
    val textContent: String? = null,
    val bitmap: Bitmap? = null,
    val base64Data: String? = null
)

object FileHelper {

    fun processUri(context: Context, uri: Uri): FileAttachmentData? {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
        var name = "attachment"
        var size: Long = 0

        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex != -1) name = cursor.getString(nameIndex) ?: "attachment"
                if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
            }
        }

        return try {
            if (mimeType.startsWith("image/")) {
                val inputStream: InputStream? = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                val base64 = bitmap?.let { bitmapToBase64(it) }
                FileAttachmentData(
                    uri = uri,
                    name = name,
                    mimeType = mimeType,
                    size = size,
                    bitmap = bitmap,
                    base64Data = base64
                )
            } else if (isTextBasedMime(mimeType) || name.endsWith(".txt") || name.endsWith(".csv") || name.endsWith(".json") || name.endsWith(".md") || name.endsWith(".html") || name.endsWith(".xml")) {
                val inputStream: InputStream? = contentResolver.openInputStream(uri)
                val text = inputStream?.bufferedReader()?.use { it.readText() }
                FileAttachmentData(
                    uri = uri,
                    name = name,
                    mimeType = mimeType,
                    size = size,
                    textContent = text
                )
            } else {
                val inputStream: InputStream? = contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()
                val base64 = bytes?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
                FileAttachmentData(
                    uri = uri,
                    name = name,
                    mimeType = mimeType,
                    size = size,
                    base64Data = base64,
                    textContent = "Binary file: $name (${bytes?.size ?: 0} bytes)"
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun isTextBasedMime(mimeType: String): Boolean {
        return mimeType.startsWith("text/") ||
                mimeType.contains("json") ||
                mimeType.contains("csv") ||
                mimeType.contains("xml") ||
                mimeType.contains("javascript")
    }

    fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }
}
