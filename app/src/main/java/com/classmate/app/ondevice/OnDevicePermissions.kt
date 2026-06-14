package com.classmate.app.ondevice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * A content-free snapshot of the functional permissions ClassMate relies on. Booleans only — the VM
 * and Settings consume this without touching a [Context], which keeps the VM unit-testable.
 */
data class OnDevicePermissionSnapshot(
    val allFilesAccess: Boolean,
    val mediaImages: Boolean,
    val mediaVideo: Boolean,
    val mediaAudio: Boolean,
    val recordAudio: Boolean,
    val postNotifications: Boolean,
    val camera: Boolean,
    val bluetoothConnect: Boolean,
) {
    companion object {
        /** Conservative default before the first device query (used as the initial UI state). */
        fun unknown(): OnDevicePermissionSnapshot =
            OnDevicePermissionSnapshot(false, false, false, false, false, false, false, false)
    }
}

/**
 * Android-facing permission helper (Task A–E). Reads the live grant state and builds the intents the
 * Settings "权限与能力诊断" card needs. All-files access is what unblocks the official on-device model
 * directory (/sdcard/1225) on Android 11+; the media permissions back the learning-material import.
 */
class OnDevicePermissions(private val context: Context) {

    fun snapshot(): OnDevicePermissionSnapshot = OnDevicePermissionSnapshot(
        allFilesAccess = allFilesAccessGranted(),
        mediaImages = mediaGranted(Manifest.permission.READ_MEDIA_IMAGES),
        mediaVideo = mediaGranted(Manifest.permission.READ_MEDIA_VIDEO),
        mediaAudio = mediaGranted(Manifest.permission.READ_MEDIA_AUDIO),
        recordAudio = granted(Manifest.permission.RECORD_AUDIO),
        postNotifications = postNotificationsGranted(),
        camera = granted(Manifest.permission.CAMERA),
        bluetoothConnect = bluetoothConnectGranted(),
    )

    fun cameraGranted(): Boolean = granted(Manifest.permission.CAMERA)

    /** BLUETOOTH_CONNECT on Android 12+; legacy BLUETOOTH is an install-time permission below that. */
    fun bluetoothConnectGranted(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            granted(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            true
        }

    /** All-files access (MANAGE_EXTERNAL_STORAGE) on R+; legacy READ_EXTERNAL_STORAGE below. */
    fun allFilesAccessGranted(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching { Environment.isExternalStorageManager() }.getOrDefault(false)
        } else {
            granted(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    private fun mediaGranted(permission: String): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            granted(permission)
        } else {
            granted(Manifest.permission.READ_EXTERNAL_STORAGE) // pre-13 routes through storage read
        }

    private fun postNotificationsGranted(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            granted(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            runCatching { NotificationManagerCompat.from(context).areNotificationsEnabled() }.getOrDefault(true)
        }

    private fun granted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    /**
     * Intents to open the all-files-access settings screen: the app-scoped screen first, then the
     * global list as a fallback. Empty below R (legacy runtime permission is used instead).
     */
    fun allFilesAccessIntents(): List<Intent> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            listOf(
                Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:" + context.packageName),
                ),
                Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION),
            )
        } else {
            emptyList()
        }

    /** Runtime media-import permissions to request: READ_MEDIA_* on 13+, storage read below. */
    fun mediaRequestPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO,
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    /** The runtime camera permission (capture study material). */
    fun cameraPermission(): String = Manifest.permission.CAMERA

    /** Bluetooth audio-device runtime permission on Android 12+ (null below — legacy is install-time). */
    fun bluetoothRequestPermission(): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Manifest.permission.BLUETOOTH_CONNECT else null
}
