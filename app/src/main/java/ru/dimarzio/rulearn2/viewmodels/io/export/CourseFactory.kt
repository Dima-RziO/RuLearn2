package ru.dimarzio.rulearn2.viewmodels.io.export

import ru.dimarzio.rulearn2.application.Database
import ru.dimarzio.rulearn2.utils.ImageFile
import java.io.File

class CourseFactory(private val course: String) : ExportFactory {
    override fun make(
        root: String,
        folder: File,
        database: Database
    ): ExportComponent {
        val composite = ExportComposite(root)
        val icon = ImageFile("$folder/icons/$course")

        composite.add(Directory(folder, "$folder/audio/$course/"))
        composite.add(Directory(folder, "$folder/pictures/$course/"))

        if (icon != null) {
            composite.add(Path(icon.path))
        }

        composite.add(Course(database, course))

        return composite
    }
}