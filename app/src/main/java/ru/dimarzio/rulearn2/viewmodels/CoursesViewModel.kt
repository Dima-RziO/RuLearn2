package ru.dimarzio.rulearn2.viewmodels

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.dimarzio.rulearn2.application.Database
import ru.dimarzio.rulearn2.models.Course
import ru.dimarzio.rulearn2.models.Word
import ru.dimarzio.rulearn2.utils.ImageFile
import ru.dimarzio.rulearn2.utils.toMutableStateMap
import ru.dimarzio.rulearn2.viewmodels.io.export.ExportComponent
import ru.dimarzio.rulearn2.viewmodels.io.export.ExportFactory
import ru.dimarzio.rulearn2.viewmodels.io.import.CSV
import ru.dimarzio.rulearn2.viewmodels.io.import.ImportComponent
import ru.dimarzio.rulearn2.viewmodels.io.import.ImportFactory
import vladis.luv.wificopy.transport.FileInfo
import vladis.luv.wificopy.transport.Host
import vladis.luv.wificopy.transport.Peer
import vladis.luv.wificopy.transport.Prefs
import java.io.File
import java.util.zip.ZipOutputStream

class CoursesViewModel(
    private val database: Database,
    private val handler: ErrorHandler,
    inDir: File,
    outDir: File,
    lifecycle: Lifecycle
) : ViewModel(), Observer {
    private val courses = runCatching { database.courses }
        .onFailure(handler::onErrorHandled)
        .getOrDefault(emptyMap())
        .toMutableStateMap()

    private val _importProgress = MutableStateFlow(null as Float?)
    private val _exportProgress = MutableStateFlow(null as Float?)
    private val _deleteProgress = MutableStateFlow(null as Float?)

    private val _replicationLogs =
        MutableStateFlow(emptyList<String>()) // MutableStateList does not support multithread operations.

    private val peer: Peer

    private val importedCourses = mutableSetOf<String>()

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
                            .onFailure(handler::onErrorHandled)
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

    private fun Uri.getName(context: Context): String? {
        return context.contentResolver.query(this, null, null, null, null)?.use { cursor ->
            cursor.moveToFirst()
            cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
        }
    }

    private val String.extension get() = substringAfterLast('.')

    fun import(context: Context, uri: Uri, folder: File) {
        viewModelScope.launch(Dispatchers.Main) {
            _importProgress.value = 0f

            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val name =
                        checkNotNull(uri.getName(context)) { "Could not determine uri's name" }

                    val factory = ImportFactory(database, folder)
                    val component = factory.create(name)

                    component.attach(this@CoursesViewModel)

                    context.contentResolver.openInputStream(uri)?.use { `is` ->
                        component.import(`is`)
                    }
                }
            }

            _importProgress.value = null

            importedCourses.forEach { course ->
                val result = runCatching { courses[course] = database.getCourse(course) }
                result.onFailure(handler::onErrorHandled)
            }

            importedCourses.clear()

            result
                .onFailure(handler::onErrorHandled)
                .onSuccess { handler.onMessageReceived("Success.") }
        }
    }

    fun export(context: Context, uri: Uri, folder: File, factory: ExportFactory) {
        viewModelScope.launch {
            _exportProgress.value = 0f

            val result = withContext(Dispatchers.IO) {
                val component = factory.make("root", folder, database)
                component.attach(this@CoursesViewModel)

                runCatching {
                    ZipOutputStream(context.contentResolver.openOutputStream(uri)).use { zos ->
                        component.export(zos)
                    }
                }
            }

            _exportProgress.value = null

            result
                .onFailure(handler::onErrorHandled)
                .onSuccess { handler.onMessageReceived("Success.") }
        }
    }

    fun replicate(hosts: Set<Host>) {
        viewModelScope.launch {
            isReplicating = true

            val replicated = mutableSetOf<String>()
            val result = withContext(Dispatchers.Default) {
                runCatching {
                    hosts.forEach { host ->
                        val file = FileInfo("rulearn.db", 0, 0)
                        val stem = host.ip.hostAddress ?: host.hostname
                        val dest = File(Prefs.inboxFolder, "$stem.db")

                        peer.getSelectedFileFromServer(host, file, dest)

                        database.replicate(dest)

                        replicated.addAll(replicated)
                    }
                }
            }

            isReplicating = false

            replicated.forEach { course ->
                courses[course] = database.getCourse(course)
            }

            result
                .onFailure(handler::onErrorHandled)
                .onSuccess { handler.onMessageReceived("Success.") }
        }
    }

    fun deleteCourse(name: String, folder: File) {
        viewModelScope.launch {
            _deleteProgress.value = 0f

            val result = withContext(Dispatchers.Default) {
                runCatching {
                    val audio = File(folder, "audio")
                    val pictures = File(folder, "pictures")
                    val tflite = File(folder, "tflite")

                    File(audio, "$name/").deleteRecursively()
                    File(pictures, "$name/").deleteRecursively()
                    File(tflite, "$name.tflite").delete()
                    ImageFile("$folder/icons/$name")?.delete()

                    database.deleteCourse(name)
                }
            }

            courses.remove(name)
            _deleteProgress.value = null

            result
                .onFailure(handler::onErrorHandled)
                .onSuccess { handler.onMessageReceived("Success.") }
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

        result.onFailure(handler::onErrorHandled)
    }

    fun changeIcon(context: Context, uri: Uri, folder: File, name: String) {
        val result = runCatching {
            viewModelScope.launch(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { `is` ->
                    val extension = uri.getName(context)?.extension

                    ImageFile("$folder/icons/$name")?.delete() // Delete previous icon
                    val icon = File(folder, "icons/$name.$extension")

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
        }

        result.onFailure(handler::onErrorHandled)
    }

    fun updateCourse(name: String) {
        courses[name] = database.getCourse(name)
    }

    fun updateCourse(name: String, old: Word?, new: Word) {
        val course = courses.getValue(name)

        courses[name] = if (old != null) {
            course.copy(
                total = if (old.skip && !new.skip) {
                    course.total + 1
                } else if (!old.skip && new.skip) {
                    course.total - 1
                } else {
                    course.total
                },
                repeat = if (old.isRepeat && !new.isRepeat) {
                    course.repeat - 1
                } else if (!old.isRepeat && new.isRepeat) {
                    course.repeat + 1
                } else {
                    course.repeat
                },
                learned = if (old.learned && !new.learned) {
                    course.learned - 1
                } else if (!old.learned && new.learned) {
                    course.learned + 1
                } else {
                    course.learned
                }
            )
        } else {
            course.copy(
                total = if (!new.skip) course.total + 1 else course.total,
                repeat = if (new.isRepeat) course.repeat + 1 else course.repeat,
                learned = if (new.learned) course.learned + 1 else course.learned
            )
        }
    }

    fun updateCourse(name: String, toRemove: Word) { // Remove word
        val course = courses.getValue(name)

        courses[name] = course.copy(
            total = if (!toRemove.skip) course.total - 1 else course.total,
            repeat = if (toRemove.isRepeat) course.repeat - 1 else course.repeat,
            learned = if (toRemove.learned) course.learned - 1 else course.learned
        )
    }

    fun runSQLiteQuery(query: String) {
        val result = runCatching {
            database.rawQuery(query)

            courses.clear()
            courses.putAll(database.courses)
        }

        result
            .onFailure(handler::onErrorHandled)
            .onSuccess { handler.onMessageReceived("Success.") }
    }

    override fun update(subject: Subject) {
        when (subject) {
            is CSV -> { // CSV imported.
                val tableName = subject.name.removeSuffix(".csv")
                if (!tableName.endsWith("_ml")) {
                    val course = tableName.removeSuffix("_stat")
                    importedCourses.add(course)
                }
            }

            is ImportComponent -> {
                val progress = subject.getProgress() * 100
                if (progress != _importProgress.value) {
                    _importProgress.value = progress
                }
            }

            is ExportComponent -> {
                val progress = subject.getProgress() * 100
                if (progress != _exportProgress.value) {
                    _exportProgress.value = progress
                }
            }
        }
    }
}