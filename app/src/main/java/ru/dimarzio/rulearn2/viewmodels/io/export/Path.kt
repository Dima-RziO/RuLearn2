package ru.dimarzio.rulearn2.viewmodels.io.export

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class Path(private val folder: File, name: String) : ExportComponent(name) { // Leaf
    override fun export(zos: ZipOutputStream) {
        val file = File(name)

        if (file.exists()) {
            val entry = ZipEntry(name.removePrefix(folder.path + "/"))

            zos.putNextEntry(entry)
            file.inputStream().use { `is` -> `is`.copyTo(zos) }
        }
    }
}