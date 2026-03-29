package ru.dimarzio.rulearn2.viewmodels.io.export

import ru.dimarzio.rulearn2.application.Database
import java.io.File

interface ExportFactory { // Factory, GoF Strategy
    // TODO: Switch to GoF Interpreter
    fun make(root: String, folder: File, database: Database): ExportComponent
}