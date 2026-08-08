package com.example.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

data class PermissionItem(
    val id: String,
    val title: String,
    val description: String,
    val permissions: List<String>,
    val iconRes: Int? = null
)

object PermissionHelper {

    val PERMISSION_CAMERA = PermissionItem(
        id = "camera",
        title = "Camera",
        description = "Take photos or scan documents directly from the chat box.",
        permissions = listOf(Manifest.permission.CAMERA)
    )

    val PERMISSION_MICROPHONE = PermissionItem(
        id = "microphone",
        title = "Microphone",
        description = "Record audio for voice-to-text input and voice messages.",
        permissions = listOf(Manifest.permission.RECORD_AUDIO)
    )

    val PERMISSION_LOCATION = PermissionItem(
        id = "location",
        title = "Location",
        description = "Provide context-aware responses like local weather and nearby places.",
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    val PERMISSION_PHOTOS = PermissionItem(
        id = "photos",
        title = "Photos & Media",
        description = "Import existing photos and images from your photo library.",
        permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    )

    val PERMISSION_DOCUMENTS = PermissionItem(
        id = "documents",
        title = "Documents & Files",
        description = "Import PDF, TXT, CSV, and code files for AI document analysis.",
        permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    )

    val ALL_PERMISSIONS = listOf(
        PERMISSION_CAMERA,
        PERMISSION_MICROPHONE,
        PERMISSION_LOCATION,
        PERMISSION_PHOTOS,
        PERMISSION_DOCUMENTS
    )

    fun isPermissionGranted(context: Context, item: PermissionItem): Boolean {
        return item.permissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
}
