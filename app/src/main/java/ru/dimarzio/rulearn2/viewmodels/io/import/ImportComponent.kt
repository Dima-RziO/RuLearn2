package ru.dimarzio.rulearn2.viewmodels.io.import

import ru.dimarzio.rulearn2.viewmodels.Subject
import java.io.InputStream

open class ImportComponent(val name: String) : Subject() { // GoF Composite
    open fun import(`is`: InputStream) {
        println("Skipped $name")
    }

    open fun add(component: ImportComponent) {
        error("Calling add through a leaf object")
    }

    open fun remove(component: ImportComponent) {
        error("Calling remove through a leaf object")
    }

    open fun getProgress(): Float { // 0 is 0% and 1 is 100%
        return 0f
    }
}