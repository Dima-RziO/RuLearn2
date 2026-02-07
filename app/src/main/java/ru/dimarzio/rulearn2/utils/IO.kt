package ru.dimarzio.rulearn2.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

fun ImageFile(path: String): File? { // 'class'
    val png = File("$path.png")
    val jpg = File("$path.jpg")
    val jpeg = File("$path.jpeg")

    return when {
        png.canRead() -> png
        jpg.canRead() -> jpg
        jpeg.canRead() -> jpeg
        else -> null
    }
}

fun File.deleteDirectory(onChildDeleted: (Float) -> Unit) {
    val total: Int
    listFiles().also { total = it?.size ?: 0 }?.forEachIndexed { i, file ->
        if (file.isDirectory) {
            file.deleteDirectory(onChildDeleted)
        } else {
            file.delete()
            onChildDeleted(i.inc() percentageFrom total)
        }
    }
    delete()
}

fun File.countSubFiles(): Int {
    var result = 0
    listFiles()?.forEach { file ->
        result += if (file.isDirectory) file.countSubFiles() else 1
    }
    return result
}

inline fun ZipInputStream.read(block: (ZipEntry) -> Unit) {
    var zipEntry = ZipEntry("")
    while (nextEntry?.let { zipEntry = it } != null) {
        block(zipEntry)
    }
}

inline fun ZipOutputStream.zipDirectory(
    toZip: File,
    getEntryName: (File) -> String,
    onProgressUpdate: (Float) -> Unit
) {
    val subFilesCount = toZip.countSubFiles()

    toZip.walkTopDown().forEachIndexed { i, file ->
        if (file.isFile) {
            putNextEntry(ZipEntry(getEntryName(file)))
            file.inputStream().use { `is` -> `is`.copyTo(this) }
        }

        onProgressUpdate(i.inc() percentageFrom subFilesCount)
    }
}

fun Uri.getName(context: Context): String? {
    return context.contentResolver.query(this, null, null, null, null)?.use { cursor ->
        cursor.moveToFirst()
        cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
    }
}