package ru.dimarzio.rulearn2.utils

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Environment
import android.widget.Toast

val Context.deviceVolume: Int
    get() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    }

val Context.notifyPermissionGranted
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permission = checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
        permission == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

val Context.storagePermissionGranted
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        val write = checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        val read = checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        read == PackageManager.PERMISSION_GRANTED && write == PackageManager.PERMISSION_GRANTED
    }

fun Context.toast(text: String, length: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, text, length).show()
}