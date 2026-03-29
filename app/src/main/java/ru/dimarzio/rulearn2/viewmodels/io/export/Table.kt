package ru.dimarzio.rulearn2.viewmodels.io.export

import com.github.doyaaaaaken.kotlincsv.client.KotlinCsvExperimental
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import ru.dimarzio.rulearn2.application.Database
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

open class Table(private val database: Database, name: String) : ExportComponent(name) { // Leaf
    @OptIn(KotlinCsvExperimental::class)
    override fun export(zos: ZipOutputStream) {
        val entry = ZipEntry("$name.csv")
        zos.putNextEntry(entry)

        // TODO: Track the progress
        csvWriter().openAndGetRawWriter(zos).writeRows(database.exportTable(name))
    }
}