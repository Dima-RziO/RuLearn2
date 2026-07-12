package ru.dimarzio.rulearn2.viewmodels.io.export

import ru.dimarzio.rulearn2.application.Database

class Course(database: Database, name: String) : ExportComposite(name) { // Composite
    init {
        add(Table(database, name))
        add(Table(database, name + "_stat"))
        add(Table(database, name + "_ml"))
    }
}