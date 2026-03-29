package ru.dimarzio.rulearn2.viewmodels.io.import

import ru.dimarzio.rulearn2.application.Database
import java.io.File

class ImportFactory(private val database: Database, private val folder: File) { // Composite factory
    fun create(name: String): ImportComponent {
        return when (name.substringAfterLast('.')) { // Extension
            "csv" -> CSV(database, name)
            "db" -> DB(database.path, name)
            "png", "jpg", "mp3", "tflite" -> Media(folder, name)
            "zip" -> ZIP(this, name)
            else -> ImportComponent(name)
        }
    }
}