package ru.dimarzio.rulearn2.viewmodels.io.import

import java.io.File
import java.io.InputStream

class Media(private val folder: File, name: String) : ImportComponent(name) {
    override fun import(`is`: InputStream) {
        val file = File(folder, name)

        file.parentFile?.mkdirs()
        file.createNewFile()

        file.outputStream().use { os -> `is`.copyTo(os) }
    }
}