package ru.dimarzio.rulearn2.viewmodels.io.export

import okio.use
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class Directory(private val folder: File, name: String) : ExportComponent(name) { // Leaf
    private var _progress = super.getProgress()

    override fun export(zos: ZipOutputStream) {
        val directory = File(name)
        val files = directory.walk().filter { file -> !file.isDirectory }.toList()
        val total = files.size

        files.forEachIndexed { i, file ->
            val entry = ZipEntry(file.path.removePrefix(folder.path + "/"))

            zos.putNextEntry(entry)
            file.inputStream().use { `is` -> `is`.copyTo(zos) }

            _progress = (i + 1f) / total
            gofnotify()
        }
    }

    override fun getProgress(): Float {
        return _progress
    }
}