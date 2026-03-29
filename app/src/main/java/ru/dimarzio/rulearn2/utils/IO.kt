package ru.dimarzio.rulearn2.utils

import java.io.File

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