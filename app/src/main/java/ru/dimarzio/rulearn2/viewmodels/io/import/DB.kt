package ru.dimarzio.rulearn2.viewmodels.io.import

import java.io.File
import java.io.InputStream

class DB(private val file: File, name: String) : ImportComponent(name) { // Leaf
    override fun import(`is`: InputStream) {
        file.outputStream().use { os -> `is`.copyTo(os) }
    }
}