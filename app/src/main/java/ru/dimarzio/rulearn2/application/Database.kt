package ru.dimarzio.rulearn2.application

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import ru.dimarzio.rulearn2.models.Course
import ru.dimarzio.rulearn2.models.Word
import ru.dimarzio.rulearn2.utils.ImageFile
import ru.dimarzio.rulearn2.viewmodels.PreferencesViewModel
import java.io.File

/*
* Not singleton!
* Note: It is caller's responsibility to catch all exceptions
 */

class Database(private val folder: File) { // Not singleton!
    val database: SQLiteDatabase by lazy { // If exception is thrown, it should be handled by caller
        SQLiteDatabase.openOrCreateDatabase(File(folder, DB_NAME), null).apply {
            setForeignKeyConstraintsEnabled(true)
        }
    }

    val courses get() = getCoursesNames().associateWith(::getCourse)

    companion object {
        private const val MS = 3600000.0

        private const val MASTER = "main"
        private const val SLAVE = "slave"

        const val DB_NAME = "rulearn.db"
    }

    private inline fun <R> query(sql: String, block: Cursor.() -> R): R {
        return database.rawQuery(sql, null).use(block)
    }

    private fun updateOrInsert(table: String, values: Map<String, *>, whereClause: String) {
        if (database.update("'$table'", values.toContentValues(), whereClause, null) == 0) {
            database.insertOrThrow("'$table'", null, values.toContentValues())
        }
    }

    private fun Map<String, *>.toContentValues() = ContentValues().apply {
        forEach { (key, value) ->
            if (value != null) {
                when (value) {
                    is String -> put(key, value)
                    is Byte -> put(key, value)
                    is Short -> put(key, value)
                    is Int -> put(key, value)
                    is Long -> put(key, value)
                    is Float -> put(key, value)
                    is Double -> put(key, value)
                    is Boolean -> put(key, if (value) 1 else 0)
                    is ByteArray -> put(key, value)
                    null -> putNull(key)
                    else -> put(key, value.toString())
                }
            }
        }
    }

    private fun columnsExist(table: String, vararg columns: String?): Boolean {
        query("SELECT * FROM $table LIMIT 0") {
            columns.forEach { column ->
                if (getColumnIndex(column) == -1) {
                    return false
                }
            }
        }
        return true
    }

    private fun getRowCount(table: String, condition: String? = null): Int {
        return query("SELECT COUNT(*) FROM $table" + condition?.let { " WHERE $it" }.orEmpty()) {
            moveToFirst()
            getInt(0)
        }
    }

    private fun createTable(name: String, vararg columns: String) {
        return database.execSQL("CREATE TABLE IF NOT EXISTS'$name'(${columns.joinToString(", ")})")
    }

    fun getCoursesNames(db: String = MASTER): List<String> {
        val tables = query("SELECT name FROM $db.sqlite_master WHERE type = 'table'") {
            List(count) {
                moveToNext()
                getString(0)
            }
        }

        return tables.filter { table ->
            columnsExist("$db.'$table'", "id", "word", "translation", "audio", "level")
        }
    }

    fun getRepeatWords(course: String): Int {
        val millis = System.currentTimeMillis()
        val ratio = "IFNULL(CAST(sum_correct AS REAL) / NULLIF(CAST(n_repeat AS REAL), 0), 1.0)"

        return getRowCount(
            "'$course' LEFT JOIN '${course}_stat' ON '$course'.id = '${course}_stat'.id",
            "skip = 0 " +
                    "AND ((rating = 10 AND (($millis - accessed) / $MS) >= 1 * $ratio) OR " +
                    "(rating = 11 AND (($millis - accessed) / $MS) >= 5 * $ratio) OR " +
                    "(rating = 12 AND (($millis - accessed) / $MS) >= 24 * $ratio) OR " +
                    "(rating = 13 AND (($millis - accessed) / $MS) >= 120 * $ratio) OR " +
                    "(rating = 14 AND (($millis - accessed) / $MS) >= 600 * $ratio) OR " +
                    "(rating = 15 AND (($millis - accessed) / $MS) >= 2880 * $ratio))"
        )
    }

    fun getCourse(name: String) = Course(
        icon = ImageFile("$folder/icons/$name"),
        repeat = getRepeatWords(name),
        learned = getRowCount(
            "'$name' LEFT JOIN '${name}_stat' ON '$name'.id = '${name}_stat'.id",
            "rating >= 10 AND skip = 0"
        ),
        total = getRowCount(
            "'$name' LEFT JOIN '${name}_stat' ON '$name'.id = '${name}_stat'.id",
            "skip = 0 OR skip IS NULL"
        )
    )

    fun createCourse(name: String) { // Do NOT change the logic.
        val course = name.removeSuffix("_stat").removeSuffix("_ml")

        if (!name.endsWith("_stat")) {
            createTable(
                course + "_ml",
                "id INTEGER PRIMARY KEY REFERENCES '$course'(id) ON DELETE CASCADE",
                "n_repeat INTEGER DEFAULT 0",
                "sum_correct INTEGER DEFAULT 0",
                "cur_rating INTEGER DEFAULT 0",
                "s_lapsed INTEGER DEFAULT 0",
                "type_repeat INTEGER",
                "n_hint INTEGER DEFAULT 0"
            )
        }

        if (!name.endsWith("_ml")) {
            createTable(
                course + "_stat",
                "id INTEGER PRIMARY KEY REFERENCES '$course'(id) ON DELETE CASCADE",
                "accessed INTEGER DEFAULT 0",
                "skip INTEGER DEFAULT 0",
                "difficult INTEGER DEFAULT 0",
                "rating INTEGER DEFAULT 0",
                "n_repeat INTEGER DEFAULT 0",
                "sum_correct INTEGER DEFAULT 0"
            )
        }

        if (name == course) {
            createTable(
                name,
                "id INTEGER PRIMARY KEY AUTOINCREMENT",
                "word TEXT",
                "translation TEXT",
                "audio TEXT",
                "level TEXT"
            )
        }
    }

    fun deleteCourse(name: String) { // Do NOT change the logic.
        val course = name.removeSuffix("_stat").removeSuffix("_ml")

        if (!name.endsWith("_stat")) {
            database.execSQL("DROP TABLE IF EXISTS'${course}_ml'")
        }

        if (!name.endsWith("_ml")) {
            database.execSQL("DROP TABLE IF EXISTS'${course}_stat'")
        }

        if (name == course) {
            database.execSQL("DROP TABLE IF EXISTS'$name'")
        }
    }

    fun renameCourse(from: String, to: String) {
        database.execSQL("ALTER TABLE '$from' RENAME TO '$to'")
        database.execSQL("ALTER TABLE '${from}_stat' RENAME TO '${to}_stat'")
        database.execSQL("ALTER TABLE '${from}_ml' RENAME TO '${to}_ml'")
    }

    fun importLines(course: String, lines: List<List<String>>) = lines.forEach { line ->
        val content = when {
            course.endsWith("_stat") -> mapOf(
                "id" to line[0].toIntOrNull(),
                "accessed" to line[1].toIntOrNull(),
                "skip" to line[2].toIntOrNull(),
                "difficult" to line[3].toIntOrNull(),
                "rating" to line[4].toIntOrNull(),
                "n_repeat" to line[5].toIntOrNull(),
                "sum_correct" to line[6].toIntOrNull()
            )

            course.endsWith("_ml") -> mapOf(
                "id" to line[0].toIntOrNull(),
                "n_repeat" to line[1].toIntOrNull(),
                "sum_correct" to line[2].toIntOrNull(),
                "cur_rating" to line[3].toIntOrNull(),
                "s_lapsed" to line[4].toIntOrNull(),
                "type_repeat" to line[5].toIntOrNull(),
                "n_hint" to line[6].toIntOrNull()
            )

            else -> mapOf(
                "id" to line[0].takeIf { id -> id.isNotBlank() },
                "word" to line[1],
                "translation" to line[2],
                "audio" to line[3],
                "level" to line[4]
            )
        }

        database.insertOrThrow("'$course'", null, content.toContentValues())
    }

    fun exportTable(course: String): List<List<String?>> {
        val columns = "'$course'.id, word, translation, audio, level"
        val columnsStat = "id, accessed, skip, difficult, rating, n_repeat, sum_correct"
        val columnsMl = "id, n_repeat, sum_correct, cur_rating, s_lapsed, type_repeat, n_hint"

        return query(
            when {
                course.endsWith("_stat") -> "SELECT $columnsStat FROM '$course'"
                course.endsWith("_ml") -> "SELECT $columnsMl FROM '$course'"
                else -> "SELECT $columns FROM '$course'"
            }
        ) {
            List(count) {
                moveToNext()
                List(columnCount) { index ->
                    getStringOrNull(index).orEmpty()
                }
            }
        }
    }

    fun deleteLevel(course: String, level: String) {
        database.delete("'$course'", "level = '$level'", null)
    }

    fun renameLevel(course: String, from: String, to: String) {
        database.update(
            "'$course'",
            mapOf("level" to to).toContentValues(),
            "level = '$from'",
            null
        )
    }

    fun getWords(course: String): Map<Int, Word> {
        val stat = course + "_stat"
        val ml = course + "_ml"

        val columns = "'$course'.id, word, translation, audio, level"
        val columnsStat = "accessed, skip, difficult, rating, $stat.n_repeat, $stat.sum_correct"
        val columnsMl =
            "s_lapsed, type_repeat, n_hint" // TODO: Select n_repeat and sum_correct from _ml

        return buildMap {
            query(
                "SELECT $columns, $columnsStat, $columnsMl " +
                        "FROM '$course' " +
                        "LEFT JOIN '$stat' ON '$course'.id = '$stat'.id " +
                        "LEFT JOIN '$ml' ON '$course'.id = '$ml'.id"
            ) {
                while (moveToNext()) {
                    this@buildMap[getInt(0)] = Word(
                        name = getStringOrNull(1).orEmpty(),
                        translation = getStringOrNull(2).orEmpty(),
                        audios = getStringOrNull(3)?.split(",", ";")?.let { audios ->
                            List(audios.size) { index ->
                                File(
                                    File(
                                        folder,
                                        "audio"
                                    ),
                                    "$course/${audios[index].trim()}"
                                )
                            }
                        },
                        level = getString(4),
                        accessed = getLong(5),
                        skip = getInt(6) == 1,
                        difficult = getInt(7) == 1,
                        rating = getInt(8),
                        repetitions = getInt(9),
                        correctAnswers = getInt(10),
                        secondsLapsed = getLong(11),
                        typeRepeat = PreferencesViewModel.Session
                            .entries
                            .getOrNull(getIntOrNull(12) ?: -1),
                        hintsUsed = getInt(13)
                    )
                }
            }
        }
    }

    fun deleteWord(course: String, id: Int) {
        database.delete("'$course'", "id = $id", null)
    }

    fun updateWord(course: String, id: Int, word: Word) {
        updateOrInsert(
            table = course,
            values = mapOf(
                "id" to id,
                "word" to word.name,
                "translation" to word.translation,
                "audio" to word.audios?.joinToString(transform = File::getName),
                "level" to word.level
            ),
            whereClause = "id = $id"
        )

        updateOrInsert(
            table = course + "_stat",
            values = mapOf(
                "id" to id,
                "accessed" to word.accessed,
                "skip" to word.skip,
                "difficult" to word.difficult,
                "rating" to word.rating,
                "n_repeat" to word.repetitions,
                "sum_correct" to word.correctAnswers
            ),
            whereClause = "id = $id"
        )

        updateOrInsert(
            table = course + "_ml",
            values = mapOf(
                "id" to id,
                "n_repeat" to word.repetitions,
                "sum_correct" to word.correctAnswers,
                "cur_rating" to word.rating,
                "s_lapsed" to word.secondsLapsed,
                "type_repeat" to word.typeRepeat?.ordinal,
                "n_hint" to word.hintsUsed
            ),
            whereClause = "id = $id"
        )
    }

    /*
    06.01.26
     */

    private fun replicateCourse(master: String, slave: String, course: String) {
        createTable(
            course,
            "id INTEGER PRIMARY KEY",
            "word TEXT",
            "translation TEXT",
            "audio TEXT",
            "level TEXT"
        )

        database.execSQL("INSERT INTO $master.'$course' SELECT * FROM $slave.'$course'")

        val stat = course + "_stat"

        createTable(
            stat,
            "id INTEGER PRIMARY KEY REFERENCES '$course'(id) ON DELETE CASCADE",
            "accessed INTEGER DEFAULT 0",
            "skip INTEGER DEFAULT 0",
            "difficult INTEGER DEFAULT 0",
            "rating INTEGER DEFAULT 0",
            "n_repeat INTEGER DEFAULT 0",
            "sum_correct INTEGER DEFAULT 0"
        )

        database.execSQL("INSERT INTO $master.'$stat' SELECT * FROM $slave.'$stat'")

        val ml = course + "_ml"

        createTable(
            ml,
            "id INTEGER PRIMARY KEY REFERENCES '$course'(id) ON DELETE CASCADE",
            "n_repeat INTEGER DEFAULT 0",
            "sum_correct INTEGER DEFAULT 0",
            "cur_rating INTEGER DEFAULT 0",
            "s_lapsed INTEGER DEFAULT 0",
            "type_repeat INTEGER",
            "n_hint INTEGER DEFAULT 0"
        )

        database.execSQL("INSERT INTO $master.'$ml' SELECT * FROM $slave.'$ml'")
    }

    private fun replicateWords(master: String, slave: String, course: String) {
        // Insert only those fields, which are absent in master, but exist in slave.
        database.execSQL(
            "INSERT INTO $master.'$course' (id, word, translation, audio, level) " +
                    "SELECT id, word, translation, audio, level " +
                    "FROM $slave.'$course' " +
                    "WHERE NOT EXISTS (SELECT 1 FROM $master.'$course' " +
                    "WHERE $master.'$course'.id = $slave.'$course'.id)"
        )
    }

    private fun replicateStat(master: String, slave: String, stat: String) {
        // Insert only those fields, which are absent in master, but exist in slave.
        database.execSQL(
            "INSERT INTO $master.'$stat' (id, accessed, skip, difficult, rating, n_repeat, sum_correct) " +
                    "SELECT id, accessed, skip, difficult, rating, n_repeat, sum_correct " +
                    "FROM $slave.'$stat' " +
                    "WHERE NOT EXISTS (SELECT 1 FROM $master.'$stat' " +
                    "WHERE $master.'$stat'.id = $slave.'$stat'.id)"
        )

        // Update only those fields, where master.accessed < slave.accessed
        database.execSQL(
            "UPDATE $master.'$stat' " +
                    "SET (accessed, skip, difficult, rating, n_repeat, sum_correct) = (" +
                    "SELECT accessed, skip, difficult, rating, n_repeat, sum_correct " +
                    "FROM $slave.'$stat' WHERE $slave.'$stat'.id = $master.'$stat'.id) " +
                    "WHERE EXISTS (SELECT 1 FROM $slave.'$stat' " +
                    "WHERE $slave.'$stat'.id = $master.'$stat'.id " +
                    "AND $slave.'$stat'.accessed > $master.'$stat'.accessed)"
        )
    }

    private fun replicateMl(master: String, slave: String, ml: String, stat: String) {
        // Insert only those fields, which are absent in master, but exist in slave.
        database.execSQL(
            "INSERT INTO $master.'$ml' (id, n_repeat, sum_correct, cur_rating, s_lapsed, type_repeat, n_hint) " +
                    "SELECT id, n_repeat, sum_correct, cur_rating, s_lapsed, type_repeat, n_hint " +
                    "FROM $slave.'$ml' " +
                    "WHERE NOT EXISTS (SELECT 1 FROM $master.'$ml' " +
                    "WHERE $master.'$ml'.id = $slave.'$ml'.id)"
        )

        // Update only those fields, where master.accessed < slave.accessed
        database.execSQL(
            "UPDATE $master.'$ml' " +
                    "SET (n_repeat, sum_correct, cur_rating, s_lapsed, type_repeat, n_hint) = (" +
                    "SELECT n_repeat, sum_correct, cur_rating, s_lapsed, type_repeat, n_hint " +
                    "FROM $slave.'$ml' WHERE id = $master.'$ml'.id) " +
                    "WHERE EXISTS (SELECT 1 FROM $slave.'$stat' " +
                    "JOIN $master.'$stat' ON $master.'$stat'.id = $slave.'$stat'.id " +
                    "WHERE $master.'$stat'.id = $master.'$ml'.id " +
                    "AND $slave.'$stat'.accessed > $master.'$stat'.accessed)"
        )
    }

    fun replicate(from: File) {
        try {
            database.execSQL("ATTACH DATABASE '$from' AS $SLAVE")

            val slaveCourses = getCoursesNames(SLAVE)
            val masterCourses = getCoursesNames(MASTER)

            slaveCourses.forEach { course ->
                if (course !in masterCourses) {
                    replicateCourse(MASTER, SLAVE, course)
                }
            }

            masterCourses.forEach { course ->
                if (course in slaveCourses) {
                    replicateWords(MASTER, SLAVE, course)
                    replicateMl(MASTER, SLAVE, course + "_ml", course + "_stat") // Do NOT change the order.
                    replicateStat(MASTER, SLAVE, course + "_stat")
                }
            }
        } finally {
            database.execSQL("DETACH DATABASE $SLAVE")
        }
    }
}