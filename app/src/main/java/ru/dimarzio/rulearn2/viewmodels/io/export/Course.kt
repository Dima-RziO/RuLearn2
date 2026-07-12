package ru.dimarzio.rulearn2.viewmodels.io.export

import ru.dimarzio.rulearn2.application.Database

class Course(database: Database, name: String, onlyLearned: Boolean = false) :
    ExportComposite(name) { // Composite
    init {
        add(Table(database, name, onlyLearned))
        add(Table(database, name + "_stat", onlyLearned))
        add(Table(database, name + "_ml", onlyLearned))
    }
}