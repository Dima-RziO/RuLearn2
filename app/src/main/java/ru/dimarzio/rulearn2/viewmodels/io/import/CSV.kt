package ru.dimarzio.rulearn2.viewmodels.io.import

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import ru.dimarzio.rulearn2.application.Database
import java.io.FilterInputStream
import java.io.InputStream

class CSV(private val database: Database, name: String) : ImportComponent(name) { // Leaf
    override fun import(`is`: InputStream) {
        val tableName = name.removeSuffix(".csv")

        database.deleteCourse(tableName)
        database.createCourse(tableName)

        val wrapper = object : FilterInputStream(`is`) {
            override fun close() {
                // Avoid closing `is`
            }
        }

        val lines = csvReader { skipEmptyLine = true }.readAll(wrapper)
        database.importLines(tableName, lines)

        gofnotify() // TODO: Track the progress
    }
}