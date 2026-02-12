package ru.dimarzio.rulearn2.viewmodels

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.doyaaaaaken.kotlincsv.client.KotlinCsvExperimental
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.dimarzio.rulearn2.application.Database
import ru.dimarzio.rulearn2.models.Course
import ru.dimarzio.rulearn2.utils.ImageFile
import ru.dimarzio.rulearn2.utils.deleteDirectory
import ru.dimarzio.rulearn2.utils.extension
import ru.dimarzio.rulearn2.utils.getName
import ru.dimarzio.rulearn2.utils.percentageFrom
import ru.dimarzio.rulearn2.utils.read
import ru.dimarzio.rulearn2.utils.toMutableStateMap
import ru.dimarzio.rulearn2.utils.zipDirectory
import vladis.luv.wificopy.transport.FileInfo
import vladis.luv.wificopy.transport.Host
import vladis.luv.wificopy.transport.Peer
import vladis.luv.wificopy.transport.Prefs
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.coroutines.CoroutineContext

abstract class CoursesViewModel(
    private val database: Database,
    inDir: File,
    outDir: File,
    lifecycle: Lifecycle
) : ViewModel(),
        (Throwable) -> Unit {
    private val courses = runCatching { database.courses }
        .onFailure(::invoke)
        .getOrDefault(emptyMap())
        .toMutableStateMap()

    private val _importProgress = MutableStateFlow(null as Float?)
    private val _exportProgress = MutableStateFlow(null as Float?)
    private val _deleteProgress = MutableStateFlow(null as Float?)

    private val _replicationLogs =
        MutableStateFlow(emptyList<String>()) // MutableStateList does not support multithread operations.

    private val peer: Peer

    val sortedCourses by derivedStateOf {
        courses.entries
            .sortedByDescending { (_, course) -> course.repeat }
            .associate(MutableMap.MutableEntry<String, Course>::toPair)
    }

    val importProgress = _importProgress.asStateFlow()
    val exportProgress = _exportProgress.asStateFlow()
    val deleteProgress = _deleteProgress.asStateFlow()

    val replicationLogs = _replicationLogs.asStateFlow()

    var isReplicating by mutableStateOf(false)
        private set

    val localHosts: List<Host> get() = peer.hosts

    init {
        // 29.12.25
        Prefs.outboxFolder = outDir
        Prefs.inboxFolder = inDir

        Prefs.hostname = Build.MODEL

        peer = Peer { message -> _replicationLogs.value += message }
        peer.startPeer()

        lifecycle.addObserver(
            LifecycleEventObserver { _, e ->
                if (e == Lifecycle.Event.ON_RESUME) {
                    courses.forEach { (name, course) ->
                        runCatching { database.getRepeatWords(name) }
                            .onFailure(::invoke)
                            .onSuccess { count ->
                                if (count > course.repeat) {
                                    courses[name] = course.copy(repeat = count)
                                }
                            }
                    }
                }
            }
        )
    }

    private suspend fun <R> context(
        context: CoroutineContext = Dispatchers.Default,
        block: () -> R
    ) = withContext(context) { runCatching<R> { block.invoke() } }

    private fun importCsv(`is`: InputStream, tableName: String) {
        database.deleteCourse(tableName)
        database.createCourse(tableName)

        database.importLines(tableName, csvReader { skipEmptyLine = true }.readAll(`is`))
    }

    private fun importZip(
        `is`: InputStream,
        folder: File,
        onProgressUpdate: (Float) -> Unit,
    ) {
        ZipInputStream(`is`).use { zis ->
            val initial = `is`.available()
            zis.read { zipEntry ->
                when (zipEntry.name.extension) {
                    "csv" -> {
                        // Clone `is` to avoid closing it
                        ByteArrayInputStream(zis.readBytes()).use { bis ->
                            importCsv(bis, File(zipEntry.name).name.removeSuffix(".csv"))
                        }
                    }

                    "db" -> {
                        File(database.database.path).outputStream().use { os -> zis.copyTo(os) }
                    }

                    "png", "jpg", "mp3" -> {
                        val file = File(folder, zipEntry.name)

                        file.parentFile?.mkdirs()
                        file.createNewFile()

                        file.outputStream().use { os -> zis.copyTo(os) }
                    }
                }

                // zis.available() returns something different from length.
                onProgressUpdate(initial - `is`.available() percentageFrom initial)
            }
        }
    }

    private fun importCourse(
        context: Context,
        uri: Uri,
        folder: File,
        onProgressUpdate: (Float) -> Unit
    ) {
        val name = checkNotNull(uri.getName(context)) { "Could not determine uri's name" }

        context.contentResolver.openInputStream(uri)?.use { `is` ->
            when (name.extension) {
                "db" -> File(database.database.path).outputStream().use { os -> `is`.copyTo(os) }
                "csv" -> importCsv(`is`, name.removeSuffix(".csv"))
                "zip" -> importZip(`is`, folder, onProgressUpdate)
            }
        }
    }

    private fun deleteCourse(
        name: String,
        folder: File,
        onProgressUpdate: (Float) -> Unit
    ) {
        val toDelete = listOf(
            File(File(folder, "audio"), "$name/"),
            File(File(folder, "pictures"), "$name/"),
            ImageFile("$folder/icons/$name")
        )

        toDelete.forEach { dir -> dir?.deleteDirectory(onProgressUpdate) }

        database.deleteCourse(name)
    }

    @OptIn(KotlinCsvExperimental::class)
    private fun exportCourse(
        context: Context,
        target: Uri,
        folder: File,
        course: String?,
        onProgressUpdate: (Float) -> Unit
    ) {
        ZipOutputStream(context.contentResolver.openOutputStream(target)).use { zos ->
            val toExport = if (course != null) {
                listOf(
                    File(File(folder, "audio"), course),
                    File(File(folder, "pictures"), course),
                    ImageFile("$folder/icons/$course")
                )
            } else {
                listOf(
                    File(folder, "audio"),
                    File(folder, "pictures"),
                    File(folder, "icons")
                )
            }

            toExport.forEach { dirToZip ->
                if (dirToZip?.canRead() == true) {
                    zos.zipDirectory(
                        toZip = dirToZip,
                        getEntryName = { file ->
                            file.path.removePrefix("$folder/")
                        },
                        onProgressUpdate = onProgressUpdate
                    )
                }
            }

            val tables = if (course != null) {
                listOf(course, course + "_stat", course + "_ml")
            } else {
                courses.keys + courses.keys.map { it + "_stat" } + courses.keys.map { it + "_ml" }
            }

            tables.forEach { table ->
                zos.putNextEntry(ZipEntry("$table.csv"))
                // Use raw writer to avoid closing zos
                csvWriter().openAndGetRawWriter(zos).writeRows(database.exportTable(table))
            }
        }
    }

    private fun exportDatabase(
        context: Context,
        target: Uri,
        folder: File,
        onProgressUpdate: (Float) -> Unit
    ) {
        ZipOutputStream(context.contentResolver.openOutputStream(target)).use { zos ->
            val toExport = listOf(
                File(folder, "audio"),
                File(folder, "pictures"),
                File(folder, "icons")
            )

            toExport.forEach { dirToZip ->
                if (dirToZip.canRead()) {
                    zos.zipDirectory(
                        toZip = dirToZip,
                        getEntryName = { file ->
                            file.path.removePrefix("$folder/")
                        },
                        onProgressUpdate = onProgressUpdate
                    )
                }
            }

            zos.putNextEntry(ZipEntry(Database.DB_NAME))
            File(database.database.path).inputStream().use { `is` -> `is`.copyTo(zos) }
        }
    }

    fun importCourse(context: Context, uri: Uri, folder: File) {
        viewModelScope.launch(Dispatchers.Main) {
            _importProgress.value = 0f

            val result = context {
                importCourse(context, uri, folder) { progress ->
                    _importProgress.value = progress
                }
            }

            _importProgress.value = null

            courses.clear()
            runCatching { courses += database.courses }.onFailure(::invoke)

            result.onFailure(::invoke)
        }
    }

    fun exportCourse(context: Context, target: Uri, folder: File, course: String?) {
        viewModelScope.launch {
            _exportProgress.value = 0f

            val result = context {
                exportCourse(context, target, folder, course) { progress ->
                    _exportProgress.value = progress
                }
            }

            _exportProgress.value = null

            result.onFailure(::invoke)
        }
    }

    fun exportDatabase(context: Context, target: Uri, folder: File) {
        viewModelScope.launch {
            _exportProgress.value = 0f

            val result = context {
                exportDatabase(context, target, folder) { progress ->
                    _exportProgress.value = progress
                }
            }

            _exportProgress.value = null

            result.onFailure(::invoke)
        }
    }

    fun replicate(hosts: Set<Host>) {
        viewModelScope.launch {
            isReplicating = true

            val result = context(Dispatchers.Default) {
                hosts.forEach { host ->
                    val file = FileInfo("rulearn.db", 0, 0)
                    val stem = host.ip.hostAddress ?: host.hostname
                    val dest = File(Prefs.inboxFolder, "$stem.db")

                    peer.getSelectedFileFromServer(host, file, dest)

                    database.replicate(dest)
                }
            }

            isReplicating = false

            courses.clear()
            runCatching { courses += database.courses }.onFailure(::invoke)

            result.onFailure(::invoke)
        }
    }

    fun deleteCourse(name: String, folder: File) {
        viewModelScope.launch {
            _deleteProgress.value = 0f

            val result = context {
                deleteCourse(name, folder) { progress ->
                    _deleteProgress.value = progress
                }
            }

            _deleteProgress.value = null

            result.onSuccess { courses -= name }.onFailure(::invoke)
        }
    }

    fun renameCourse(from: String, to: String, folder: File) {
        val result = runCatching {
            database.renameCourse(from, to)

            val icon = ImageFile("$folder/icons/$from")
            val renamedIcon = icon?.let { File("$folder/icons/$to." + icon.extension) }

            File(folder, "audio/$from").renameTo(File(folder, "audio/$to"))
            File(folder, "pictures/$from").renameTo(File(folder, "pictures/$to"))

            if (renamedIcon != null) {
                icon.renameTo(renamedIcon)
            }

            val course = courses.remove(from)
            if (course != null) {
                courses[to] = course.copy(icon = renamedIcon)
            }
        }

        result.onFailure(::invoke)
    }

    fun changeIcon(context: Context, uri: Uri, folder: File, name: String) {
        val result = runCatching {
            context.contentResolver.openInputStream(uri)?.use { `is` ->
                val extension = uri.getName(context)?.extension
                val icon =
                    ImageFile("$folder/icons/$name") ?: File(folder, "icons/$name.$extension")

                icon.parentFile?.mkdirs()
                icon.createNewFile()
                icon.outputStream().use { os -> `is`.copyTo(os) }

                courses.forEach { (key, course) ->
                    if (key == name) {
                        courses[key] = course.copy(icon = icon)
                    }
                }
            }
        }

        result.onFailure(::invoke)
    }

    fun updateCourse(name: String) {
        runCatching { courses[name] = database.getCourse(name) }.onFailure(::invoke)
    }

    fun runSQLiteQuery(query: String) {
        runCatching { database.database.execSQL(query) }.onFailure(::invoke).onSuccess {
            courses.clear()
            runCatching { courses += database.courses }.onFailure(::invoke)
        }
    }
}
