package ru.dimarzio.rulearn2.viewmodels.io.import

import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class ZIP(private val factory: ImportFactory, name: String) : ImportComposite(name) { // Composite
    private var progress = super.getProgress()

    override fun import(`is`: InputStream) {
        ZipInputStream(`is`).use { zis ->
            val total = `is`.available() // zis.available returns something different

            lateinit var entry: ZipEntry
            while (zis.nextEntry?.let { entry = it } != null) {
                if (!entry.isDirectory) {
                    val child = factory.create(entry.name)
                    if (child is ZIP || child is CSV) {
                        copy(child)
                    }
                    add(child)
                    child.import(zis)
                }
                zis.closeEntry()

                progress = `is`.available() / total.toFloat()
                gofnotify()
            }
        }
    }

    override fun getProgress(): Float {
        return progress
    }
}