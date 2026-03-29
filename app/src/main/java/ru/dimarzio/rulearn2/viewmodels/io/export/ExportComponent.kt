package ru.dimarzio.rulearn2.viewmodels.io.export

import ru.dimarzio.rulearn2.viewmodels.Subject
import java.util.zip.ZipOutputStream

open class ExportComponent(val name: String = "root") : Subject() { // GoF Composite
    open fun export(zos: ZipOutputStream) {
        println("Skipped $name")
    }

    open fun add(component: ExportComponent) {
        error("Calling add through a leaf object")
    }

    open fun remove(component: ExportComponent) {
        error("Calling remove through a leaf object")
    }

    open fun getProgress(): Float { // 0 is 0% and 1 is 100%
        return 0f
    }
}