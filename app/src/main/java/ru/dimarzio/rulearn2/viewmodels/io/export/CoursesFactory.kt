package ru.dimarzio.rulearn2.viewmodels.io.export

import ru.dimarzio.rulearn2.application.Database
import java.io.File

class CoursesFactory(private val courses: Set<String>) : ExportFactory {
    override fun make(root: String, folder: File, database: Database): ExportComponent {
        val composite = ExportComposite(root)

        composite.add(Directory(folder, "$folder/audio/"))
        composite.add(Directory(folder, "$folder/pictures/"))
        composite.add(Directory(folder, "$folder/icons/"))
        composite.add(Directory(folder, "$folder/tflite/"))

        courses.forEach { course ->
            composite.add(Course(database, course))
        }

        return composite
    }
}