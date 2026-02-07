package ru.dimarzio.rulearn2.viewmodels

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.core.content.getSystemService
import androidx.lifecycle.ViewModel
import ru.dimarzio.rulearn2.application.Database
import ru.dimarzio.rulearn2.application.RepeatReceiver
import ru.dimarzio.rulearn2.utils.notifyPermissionGranted
import ru.dimarzio.rulearn2.utils.storagePermissionGranted
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

class PreferencesViewModel(private val application: Application) : ViewModel() {
    private val prefs = application.getSharedPreferences("prefs", Context.MODE_PRIVATE)

    var selectedCourse by mutableStateOf(prefs.getString(SELECTED_COURSE_PREF, null))
        private set
    var selectedSession by mutableStateOf(
        enumValues<Session>()[
            prefs.getInt(SELECTED_SESSION, Session.LearnNewWords.ordinal)
        ]
    )
        private set

    var markDifficult by mutableStateOf(prefs.getBoolean(MARK_DIFFICULT, true))
        private set
    var papasHints by mutableStateOf(prefs.getBoolean(PAPAS_HINTS, false))
        private set
    var similarWords by mutableStateOf(prefs.getBoolean(SIMILAR_WORDS, true))
        private set
    var skippedWords by mutableStateOf(prefs.getBoolean(SKIPPED_WORDS, true))
        private set
    var tts by mutableStateOf(prefs.getBoolean(TTS, true))
        private set
    var backGesture by mutableStateOf(prefs.getBoolean(BACK_GESTURE, false))
        private set

    var notify by mutableStateOf(
        if (application.notifyPermissionGranted) {
            prefs.getBoolean(NOTIFY, application.notifyPermissionGranted)
        } else {
            false
        }
    )
        private set
    var notifyPer by mutableStateOf(
        Duration.parse(
            prefs.getString(NOTIFY_PER, null) ?: 5.hours.toString()
        )
    )
        private set

    var useCustomFolder by mutableStateOf(
        if (application.storagePermissionGranted) {
            prefs.getBoolean(USE_CUSTOM_FOLDER, false)
        } else {
            false
        }
    )
        private set
    var customFolderPath by mutableStateOf(prefs.getString(CUSTOM_FOLDER_PATH, null))
        private set

    var dynamicColors by mutableStateOf(prefs.getBoolean(DYNAMIC_COLORS, dynamicColorsEnabled))
        private set

    val dynamicColorsEnabled get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val appFolder: File

    init {
        val file = customFolderPath?.let { path -> File(path) }
        appFolder = if (useCustomFolder && file != null) {
            file
        } else {
            application.filesDir
        }
    }

    val database = Database(appFolder)

    val inDir = File(application.cacheDir, "in")
    val outDir: File = application.filesDir

    init {
        inDir.mkdir()
        outDir.mkdir()
    }

    private val notificationIntent = PendingIntent.getBroadcast(
        application,
        INTENT_REQUEST_CODE,
        Intent(application, RepeatReceiver::class.java).apply {
            putExtra("folder", appFolder.path)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private companion object {
        private const val SELECTED_COURSE_PREF = "selected_course"
        private const val SELECTED_SESSION = "selected_learn_mode"
        private const val MARK_DIFFICULT = "mark_difficult"
        private const val PAPAS_HINTS = "papas_hints"
        private const val SIMILAR_WORDS = "similar_words"
        private const val SKIPPED_WORDS = "skipped_words"
        private const val TTS = "tts"
        private const val BACK_GESTURE = "back_gesture"
        private const val NOTIFY = "notify"
        private const val NOTIFY_PER = "notify_per"
        private const val USE_CUSTOM_FOLDER = "use_custom_folder"
        private const val CUSTOM_FOLDER_PATH = "custom_folder_path"
        private const val DYNAMIC_COLORS = "dynamic_colors"

        private const val INTENT_REQUEST_CODE = 1
    }

    enum class Session {
        LearnNewWords,
        DifficultWords,
        TypingReview,
        GuessingReview
    }

    fun scheduleRepeatReceiver(duration: Duration) {
        application.getSystemService<AlarmManager>()?.setRepeating(
            AlarmManager.RTC,
            System.currentTimeMillis() + duration.inWholeMilliseconds,
            duration.inWholeMilliseconds,
            notificationIntent
        )
    }

    fun updateSelectedCourse(course: String?) {
        prefs.edit {
            putString(SELECTED_COURSE_PREF, course)
        }

        selectedCourse = course
    }

    fun updateSelectedSession(with: Session) {
        prefs.edit {
            putInt(SELECTED_SESSION, with.ordinal)
        }

        selectedSession = with
    }

    fun updateMarkDifficult(with: Boolean) {
        prefs.edit {
            putBoolean(MARK_DIFFICULT, with)
        }

        markDifficult = with
    }

    fun updatePapasHints(with: Boolean) {
        prefs.edit {
            putBoolean(PAPAS_HINTS, with)
        }

        papasHints = with
    }

    fun updateSimilarWords(with: Boolean) {
        prefs.edit {
            putBoolean(SIMILAR_WORDS, with)
        }

        similarWords = with
    }

    fun updateSkippedWords(with: Boolean) {
        prefs.edit {
            putBoolean(SKIPPED_WORDS, with)
        }

        skippedWords = with
    }

    fun updateTts(with: Boolean) {
        prefs.edit {
            putBoolean(TTS, with)
        }

        tts = with
    }

    fun updateBackGesture(with: Boolean) {
        prefs.edit {
            putBoolean(BACK_GESTURE, with)
        }

        backGesture = with
    }

    fun updateNotify(with: Boolean) {
        prefs.edit {
            putBoolean(NOTIFY, with)
        }
        notify = with

        if (with) {
            scheduleRepeatReceiver(notifyPer)
        } else {
            application.getSystemService<AlarmManager>()?.cancel(notificationIntent)
        }
    }

    fun updateNotifyPer(with: Duration) {
        prefs.edit {
            putString(NOTIFY_PER, with.toString())
        }
        notifyPer = with

        if (notify) {
            scheduleRepeatReceiver(with)
        }
    }

    fun updateCustomFolder(with: Boolean) {
        prefs.edit {
            putBoolean(USE_CUSTOM_FOLDER, with)
        }

        useCustomFolder = with
    }

    fun updateCustomFolderPath(with: String) {
        prefs.edit {
            putString(CUSTOM_FOLDER_PATH, with)
        }

        customFolderPath = with
    }

    fun updateDynamicColors(with: Boolean) {
        prefs.edit {
            putBoolean(DYNAMIC_COLORS, with)
        }

        dynamicColors = with
    }

    override fun onCleared() = database.database.close()
}