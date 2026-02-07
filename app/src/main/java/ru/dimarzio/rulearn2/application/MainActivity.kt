package ru.dimarzio.rulearn2.application

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ru.dimarzio.rulearn2.compose.ErrorDialog
import ru.dimarzio.rulearn2.compose.screens.Course
import ru.dimarzio.rulearn2.compose.screens.Courses
import ru.dimarzio.rulearn2.compose.screens.Level
import ru.dimarzio.rulearn2.compose.screens.Settings
import ru.dimarzio.rulearn2.compose.screens.Word
import ru.dimarzio.rulearn2.compose.screens.sessions.guessing.GuessingReview
import ru.dimarzio.rulearn2.compose.screens.sessions.joint.DifficultWords
import ru.dimarzio.rulearn2.compose.screens.sessions.joint.LearnNewWords
import ru.dimarzio.rulearn2.compose.screens.sessions.typing.TypingReview
import ru.dimarzio.rulearn2.models.Word
import ru.dimarzio.rulearn2.routes.MainRoutes
import ru.dimarzio.rulearn2.ui.theme.RuLearn2Theme
import ru.dimarzio.rulearn2.utils.navigate
import ru.dimarzio.rulearn2.utils.notifyPermissionGranted
import ru.dimarzio.rulearn2.utils.play
import ru.dimarzio.rulearn2.utils.storagePermissionGranted
import ru.dimarzio.rulearn2.utils.toast
import ru.dimarzio.rulearn2.viewmodels.CourseViewModel
import ru.dimarzio.rulearn2.viewmodels.CoursesViewModel
import ru.dimarzio.rulearn2.viewmodels.DifficultWordsViewModel
import ru.dimarzio.rulearn2.viewmodels.LearnWordsViewModel
import ru.dimarzio.rulearn2.viewmodels.LevelViewModel
import ru.dimarzio.rulearn2.viewmodels.PreferencesViewModel
import ru.dimarzio.rulearn2.viewmodels.ReviewViewModel
import ru.dimarzio.rulearn2.viewmodels.WordViewModel

// Yeah, this class is kinda big..
class MainActivity : ComponentActivity() {
    private val player = MediaPlayer()
    private val tts by lazy {
        TextToSpeech(this) { code ->
            if (code == TextToSpeech.ERROR) {
                toast("Loading TTS module failed.")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefsViewModel by viewModels<PreferencesViewModel>(
            factoryProducer = {
                viewModelFactory {
                    addInitializer(PreferencesViewModel::class) {
                        PreferencesViewModel(applicationContext as Application)
                    }
                }
            }
        )

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(
            NotificationChannel(
                RepeatReceiver.CHANNEL_ID,
                "Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )

        if (prefsViewModel.notify) {
            prefsViewModel.scheduleRepeatReceiver(prefsViewModel.notifyPer)
        }

        setContent {
            RuLearn2Theme(dynamicColor = prefsViewModel.dynamicColors) {
                MainScreen(
                    preferencesViewModel = prefsViewModel,
                    player = player,
                    tts = tts
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
        tts.shutdown()
    }
}

@Composable
fun CoursesRoute(
    prefsViewModel: PreferencesViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    var error by remember { mutableStateOf(null as String?) }

    if (error != null) {
        ErrorDialog(
            onDismissRequest = { error = null },
            message = error!!
        )
    }

    val coursesViewModel = viewModel<CoursesViewModel>(
        factory = viewModelFactory {
            addInitializer(CoursesViewModel::class) {
                object : CoursesViewModel(
                    prefsViewModel.database,
                    prefsViewModel.inDir,
                    prefsViewModel.outDir,
                    lifecycle
                ) {
                    override fun invoke(exception: Throwable) {
                        if (exception.localizedMessage != null) {
                            error = exception.localizedMessage
                            context.toast(exception.localizedMessage!!)
                        } else {
                            context.toast("Error")
                        }
                        // context.toast(exception.localizedMessage ?: "Error", Toast.LENGTH_LONG)
                    }
                }
            }

            build()
        }
    )

    var exportCourse by remember { mutableStateOf(null as String?) }
    var iconCourse by remember { mutableStateOf(null as String?) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { coursesViewModel.importCourse(context, uri, prefsViewModel.appFolder) }
    }

    val exportCourseLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let {
            coursesViewModel.exportCourse(context, uri, prefsViewModel.appFolder, exportCourse)
        }
    }

    val exportDatabaseLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let { coursesViewModel.exportDatabase(context, uri, prefsViewModel.appFolder) }
    }

    val iconLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            coursesViewModel.changeIcon(context, uri, prefsViewModel.appFolder, iconCourse!!)
        }
    }

    Courses(
        onExportCsvClick = { course ->
            exportCourseLauncher.launch(course?.let { "$course.zip" } ?: "rulearn.zip")
            exportCourse = course
        },
        onExportDatabaseClick = {
            exportDatabaseLauncher.launch("rulearn.zip")
        },
        getLocalHosts = coursesViewModel::localHosts,
        replicationLogs = coursesViewModel.replicationLogs.collectAsState().value,
        onReplicateClick = coursesViewModel::replicate,
        onSQLiteClick = coursesViewModel::runSQLiteQuery,
        onSettingsActionClick = { navController.navigate(MainRoutes.Settings.route) },
        onImportClick = {
            importLauncher.launch(
                arrayOf(
                    "application/zip",
                    "text/comma-separated-values",
                    "application/octet-stream"
                )
            )
        },
        courses = coursesViewModel.sortedCourses,
        onCourseClick = { course ->
            prefsViewModel.updateSelectedCourse(course)
            navController.navigate(MainRoutes.Course.route, "course" to course)
        },
        onDeleteCourseClick = { course ->
            if (course == prefsViewModel.selectedCourse) {
                prefsViewModel.updateSelectedCourse(null)
            }
            coursesViewModel.deleteCourse(course, prefsViewModel.appFolder)
        },
        onRenameClick = { from, to ->
            coursesViewModel.renameCourse(from, to, prefsViewModel.appFolder)
        },
        onChangeIconClick = { course ->
            iconLauncher.launch("image/*")
            iconCourse = course
        },
        importProgress = coursesViewModel.importProgress.collectAsState().value,
        deleteProgress = coursesViewModel.deleteProgress.collectAsState().value,
        exportProgress = coursesViewModel.exportProgress.collectAsState().value,
        isReplicating = coursesViewModel.isReplicating
    )
}

fun NavController.routeInStack(route: String) = runCatching { getBackStackEntry(route) }.isSuccess

@Composable
fun CourseRoute(
    course: String,
    lifecycle: Lifecycle,
    prefsViewModel: PreferencesViewModel,
    navController: NavController
) {
    val context = LocalContext.current

    var error by remember { mutableStateOf(null as String?) }

    if (error != null) {
        ErrorDialog(
            onDismissRequest = { error = null },
            message = error!!
        )
    }

    val coursesViewModel = navController.coursesViewModel()
    val courseViewModel = viewModel<CourseViewModel>(
        factory = viewModelFactory {
            addInitializer(CourseViewModel::class) {
                object : CourseViewModel(prefsViewModel.database, course, lifecycle) {
                    override fun invoke(exception: Throwable) {
                        if (exception.localizedMessage != null) {
                            error = exception.localizedMessage
                        } else {
                            context.toast("Error")
                        }
                    }
                }
            }
        }
    )

    Course(
        course = course,
        onNavigationIconClick = {
            if (navController.routeInStack(MainRoutes.Courses.route)) {
                navController.popBackStack()
            } else {
                navController.navigate(MainRoutes.Courses.route)
            }
        },
        onAddActionClick = { level ->
            navController.navigate(MainRoutes.Level.route, "level" to level)
        },
        filterRepeat = courseViewModel.filterRepeat,
        filterNotRepeat = courseViewModel.filterNotRepeat,
        filterDifficult = courseViewModel.filterDifficult,
        filterNotDifficult = courseViewModel.filterNotDifficult,
        filterSkip = courseViewModel.filterSkip,
        filterNotSkip = courseViewModel.filterNotSkip,
        filterLearned = courseViewModel.filterLearned,
        filterNotLearned = courseViewModel.filterNotLearned,
        onFilterActionClick = courseViewModel::filter,
        onSettingsActionClick = { navController.navigate(MainRoutes.Settings.route) },
        searching = courseViewModel.showSearchingIndicator,
        searchResults = courseViewModel.filteredSearchResults,
        query = courseViewModel.query,
        onQueryChange = courseViewModel::updateQuery,
        onSearch = courseViewModel::search,
        words = courseViewModel.words.collectAsState().value,
        selectedSession = prefsViewModel.selectedSession,
        onLearnNewWordsClick = {
            prefsViewModel.updateSelectedSession(PreferencesViewModel.Session.LearnNewWords)
            navController.navigate(MainRoutes.LearnNewWords.route, "level" to null)
        },
        onDifficultWordsClick = {
            prefsViewModel.updateSelectedSession(PreferencesViewModel.Session.DifficultWords)
            navController.navigate(MainRoutes.DifficultWords.route, "level" to null)
        },
        onTypingReviewClick = {
            prefsViewModel.updateSelectedSession(PreferencesViewModel.Session.TypingReview)
            navController.navigate(MainRoutes.TypingReview.route, "level" to null)
        },
        onGuessingReviewClick = {
            prefsViewModel.updateSelectedSession(PreferencesViewModel.Session.GuessingReview)
            navController.navigate(MainRoutes.GuessingReview.route, "level" to null)
        },
        loading = courseViewModel.showLoadingIndicator.collectAsState().value,
        onWordClick = { id, level ->
            navController.navigate(
                MainRoutes.Word.route, "id" to id, "level" to level
            )
        },
        levels = courseViewModel.levels.collectAsState().value,
        onLevelClick = { level ->
            navController.navigate(MainRoutes.Level.route, "level" to level)
        },
        confirmRenameLevel = courseViewModel::confirmRenameLevel,
        onRenameLevelClick = courseViewModel::renameLevel,
        onDeleteLevelClick = { level ->
            courseViewModel.deleteLevel(level)
            coursesViewModel?.updateCourse(course)
        }
    )
}

@Composable
fun LevelRoute(
    level: String,
    prefsViewModel: PreferencesViewModel,
    player: MediaPlayer,
    tts: TextToSpeech,
    navController: NavController
) {
    val coursesViewModel = navController.coursesViewModel()
    val courseViewModel = navController.courseViewModel()

    if (courseViewModel != null) {
        val courseWords by courseViewModel.words.collectAsState()
        val levelViewModel = viewModel<LevelViewModel>(
            factory = viewModelFactory {
                addInitializer(LevelViewModel::class) {
                    LevelViewModel(level, courseWords)
                }
            }
        )

        Level(
            level = level,
            onNavigationIconClick = navController::popBackStack,
            onAddActionClick = {
                navController.navigate(MainRoutes.Word.route, "id" to -1, "level" to level)
            },
            sortMethod = levelViewModel.sortMethod,
            onSortActionClick = levelViewModel::sort,
            onSettingsActionClick = { navController.navigate(MainRoutes.Settings.route) },
            words = levelViewModel.sortedWords,
            selectedSession = prefsViewModel.selectedSession,
            onLearnNewWordsClick = {
                prefsViewModel.updateSelectedSession(PreferencesViewModel.Session.LearnNewWords)
                navController.navigate(MainRoutes.LearnNewWords.route, "level" to level)
            },
            onDifficultWordsClick = {
                prefsViewModel.updateSelectedSession(PreferencesViewModel.Session.DifficultWords)
                navController.navigate(MainRoutes.DifficultWords.route, "level" to level)
            },
            onTypingReviewClick = {
                prefsViewModel.updateSelectedSession(PreferencesViewModel.Session.TypingReview)
                navController.navigate(MainRoutes.TypingReview.route, "level" to level)
            },
            onGuessingReviewClick = {
                prefsViewModel.updateSelectedSession(PreferencesViewModel.Session.GuessingReview)
                navController.navigate(MainRoutes.GuessingReview.route, "level" to level)
            },
            player = player,
            tts = tts,
            locale = courseViewModel.locale,
            onWordRemoved = { id ->
                levelViewModel.removeWord(id)
                courseViewModel.removeWord(id)
                coursesViewModel?.updateCourse(courseViewModel.course)
            },
            onWordUpdated = { id, word ->
                levelViewModel.updateWord(id, word)
                courseViewModel.updateWord(id, word)
                coursesViewModel?.updateCourse(courseViewModel.course)
            },
            onWordClick = { id, level ->
                navController.navigate(
                    MainRoutes.Word.route, "id" to id, "level" to level
                )
            }
        )
    }
}

@Composable
fun WordRoute(
    id: Int?,
    level: String,
    player: MediaPlayer,
    tts: TextToSpeech,
    navController: NavController
) {
    val coursesViewModel = navController.coursesViewModel()
    val courseViewModel = navController.courseViewModel()
    val levelViewModel = navController.levelViewModel()

    if (courseViewModel != null) {
        val words by courseViewModel.words.collectAsState()
        val wordViewModel = viewModel<WordViewModel>(
            factory = viewModelFactory {
                addInitializer(WordViewModel::class) {
                    WordViewModel(words, id, level)
                }
            }
        )

        val levelsForName by remember(key1 = words, key2 = wordViewModel.newWord) {
            mutableStateOf(wordViewModel.getLevelsForName(words))
        }

        val levelsForTranslation by remember(key1 = words, key2 = wordViewModel.newWord) {
            mutableStateOf(wordViewModel.getLevelsForTranslation(words))
        }

        Word(
            id = wordViewModel.newId,
            word = wordViewModel.newWord,
            modified = wordViewModel.modified,
            tts = tts,
            locale = courseViewModel.locale,
            onNavigationIconClick = navController::popBackStack,
            onDeleteActionClick = {
                levelViewModel?.removeWord(wordViewModel.newId)
                courseViewModel.removeWord(wordViewModel.newId)
                coursesViewModel?.updateCourse(courseViewModel.course)

                navController.popBackStack()
            },
            onSettingsActionClick = { navController.navigate(MainRoutes.Settings.route) },
            onAudioClick = player::play,
            onWordUpdated = wordViewModel::updateWord,
            onAccessedClick = wordViewModel::pickAccessed,
            levelsForName = levelsForName,
            levelsForTranslation = levelsForTranslation,
            otherLevels = courseViewModel.levels.collectAsState().value.keys,
            onWordSaved = {
                val newWord = wordViewModel.save()

                levelViewModel?.updateWord(wordViewModel.newId, newWord)
                courseViewModel.updateWord(wordViewModel.newId, newWord)
                coursesViewModel?.updateCourse(courseViewModel.course)

                navController.popBackStack()
            }
        )
    }
}

@Composable
fun GuessingReviewRoute(
    level: String?,
    limit: Int,
    prefsViewModel: PreferencesViewModel,
    player: MediaPlayer,
    tts: TextToSpeech,
    navController: NavController
) {
    val coursesViewModel = navController.coursesViewModel()
    val courseViewModel = navController.courseViewModel()
    val levelViewModel = navController.levelViewModel()

    val context = LocalContext.current

    if (courseViewModel != null) {
        val words by courseViewModel.words.collectAsState()
        val reviewViewModel = viewModel<ReviewViewModel>(
            factory = viewModelFactory {
                addInitializer(ReviewViewModel::class) {
                    ReviewViewModel(words, level, limit)
                }
            }
        )

        if (reviewViewModel.currentId != null && reviewViewModel.currentWord != null) {
            BackHandler(enabled = prefsViewModel.backGesture) {
                reviewViewModel.reselectCurrentWord()
            }

            GuessingReview(
                currentId = reviewViewModel.currentId!!,
                currentWord = reviewViewModel.currentWord!!,
                repeatedWords = reviewViewModel.repeatedWords,
                player = player,
                tts = tts,
                locale = courseViewModel.locale,
                onWordRemoved = { id ->
                    reviewViewModel.removeWord(id)
                    reviewViewModel.reselectCurrentWord()

                    levelViewModel?.removeWord(id)
                    courseViewModel.removeWord(id)
                    coursesViewModel?.updateCourse(courseViewModel.course)
                },
                onWordUpdated = { id, word ->
                    reviewViewModel.updateWord(id, word)
                    reviewViewModel.reselectCurrentWord()

                    levelViewModel?.updateWord(id, word)
                    courseViewModel.updateWord(id, word)
                    coursesViewModel?.updateCourse(courseViewModel.course)
                },
                courseWords = words,
                otherLevels = courseViewModel.levels.collectAsState().value.keys,
                similarWords = prefsViewModel.similarWords,
                skippedWords = prefsViewModel.skippedWords,
                correctAnswers = reviewViewModel.correctAnswers,
                onNavigationIconClick = navController::navigateUp,
                progress = reviewViewModel.progress,
                useTts = prefsViewModel.tts,
                onAnswer = { correct ->
                    val write = { id: Int, word: Word ->
                        levelViewModel?.updateWord(id, word)
                        courseViewModel.updateWord(id, word)
                        coursesViewModel?.updateCourse(courseViewModel.course)

                        Unit
                    }

                    reviewViewModel.answer(
                        correct,
                        prefsViewModel.markDifficult,
                        PreferencesViewModel.Session.GuessingReview,
                        0,
                        write
                    )
                },
                onRefreshRequested = reviewViewModel::reselectCurrentWord,
                ended = reviewViewModel.ended,
                hidden = reviewViewModel.hidden,
                hide = reviewViewModel::hide,
                getWord = courseViewModel::getWord,
                onSettingsActionClick = { navController.navigate(MainRoutes.Settings.route) }
            )
        } else {
            LaunchedEffect(key1 = Unit) {
                context.toast("currentWord = null")
                navController.navigateUp()
            }
        }
    }
}

@Composable
fun TypingReviewRoute(
    level: String?,
    limit: Int,
    prefsViewModel: PreferencesViewModel,
    player: MediaPlayer,
    tts: TextToSpeech,
    navController: NavController
) {
    val coursesViewModel = navController.coursesViewModel()
    val courseViewModel = navController.courseViewModel()
    val levelViewModel = navController.levelViewModel()

    val context = LocalContext.current

    if (courseViewModel != null) {
        val words by courseViewModel.words.collectAsState()
        val reviewViewModel = viewModel<ReviewViewModel>(
            factory = viewModelFactory {
                addInitializer(ReviewViewModel::class) {
                    ReviewViewModel(words, level, limit)
                }
            }
        )

        if (reviewViewModel.currentId != null && reviewViewModel.currentWord != null) {
            BackHandler(enabled = prefsViewModel.backGesture) {
                reviewViewModel.reselectCurrentWord()
            }

            TypingReview(
                currentId = reviewViewModel.currentId!!,
                currentWord = reviewViewModel.currentWord!!,
                repeatedWords = reviewViewModel.repeatedWords,
                player = player,
                tts = tts,
                locale = courseViewModel.locale,
                onWordRemoved = { id ->
                    reviewViewModel.removeWord(id)
                    reviewViewModel.reselectCurrentWord()

                    levelViewModel?.removeWord(id)
                    courseViewModel.removeWord(id)
                    coursesViewModel?.updateCourse(courseViewModel.course)
                },
                onWordUpdated = { id, word ->
                    reviewViewModel.updateWord(id, word)
                    reviewViewModel.reselectCurrentWord()

                    levelViewModel?.updateWord(id, word)
                    courseViewModel.updateWord(id, word)
                    coursesViewModel?.updateCourse(courseViewModel.course)
                },
                correctAnswers = reviewViewModel.correctAnswers,
                onNavigationIconClick = navController::popBackStack,
                progress = reviewViewModel.progress,
                courseWords = words,
                otherLevels = courseViewModel.levels.collectAsState().value.keys,
                useTts = prefsViewModel.tts,
                papasHints = prefsViewModel.papasHints,
                onAnswer = { correct, hintsUsed ->
                    val write = { id: Int, word: Word ->
                        levelViewModel?.updateWord(id, word)
                        courseViewModel.updateWord(id, word)
                        coursesViewModel?.updateCourse(courseViewModel.course)

                        Unit
                    }

                    reviewViewModel.answer(
                        correct,
                        prefsViewModel.markDifficult,
                        PreferencesViewModel.Session.TypingReview,
                        hintsUsed,
                        write
                    )
                },
                onRefreshRequested = reviewViewModel::reselectCurrentWord,
                ended = reviewViewModel.ended,
                onSettingsActionClick = { navController.navigate(MainRoutes.Settings.route) }
            )
        } else {
            LaunchedEffect(key1 = Unit) {
                context.toast("currentWord = null")
                navController.navigateUp()
            }
        }
    }
}

@Composable
fun LearnWordsRoute(
    level: String?,
    limit: Int,
    prefsViewModel: PreferencesViewModel,
    player: MediaPlayer,
    tts: TextToSpeech,
    navController: NavController
) {
    val coursesViewModel = navController.coursesViewModel()
    val courseViewModel = navController.courseViewModel()
    val levelViewModel = navController.levelViewModel()

    val context = LocalContext.current

    if (courseViewModel != null) {
        val words by courseViewModel.words.collectAsState()
        val learnWordsViewModel = viewModel<LearnWordsViewModel>(
            factory = viewModelFactory {
                addInitializer(LearnWordsViewModel::class) {
                    LearnWordsViewModel(words, level, limit)
                }
            }
        )
        if (learnWordsViewModel.currentId != null && learnWordsViewModel.currentWord != null) {
            BackHandler(
                enabled = prefsViewModel.backGesture,
                onBack = learnWordsViewModel::randomizeCurrentWord
            )

            LearnNewWords(
                learnedWords = learnWordsViewModel.learnedWords,
                player = player,
                tts = tts,
                locale = courseViewModel.locale,
                onWordRemoved = { id ->
                    learnWordsViewModel.removeWord(id)
                    learnWordsViewModel.randomizeCurrentWord()

                    levelViewModel?.removeWord(id)
                    courseViewModel.removeWord(id)
                    coursesViewModel?.updateCourse(courseViewModel.course)
                },
                onWordUpdated = { id, word ->
                    learnWordsViewModel.updateWord(id, word)
                    learnWordsViewModel.randomizeCurrentWord()

                    levelViewModel?.updateWord(id, word)
                    courseViewModel.updateWord(id, word)
                    coursesViewModel?.updateCourse(courseViewModel.course)
                },
                currentId = learnWordsViewModel.currentId!!,
                currentWord = learnWordsViewModel.currentWord!!,
                getWord = courseViewModel::getWord,
                onSettingsActionClick = { navController.navigate(MainRoutes.Settings.route) },
                courseWords = words,
                otherLevels = courseViewModel.levels.collectAsState().value.keys,
                similarWords = prefsViewModel.similarWords,
                skippedWords = prefsViewModel.skippedWords,
                onNavigationIconClick = navController::popBackStack,
                progress = learnWordsViewModel.progress,
                useTts = prefsViewModel.tts,
                papasHints = prefsViewModel.papasHints,
                onAnswer = { correct, hintsUsed ->
                    learnWordsViewModel.answer(correct, hintsUsed) { id, word ->
                        levelViewModel?.updateWord(id, word)
                        courseViewModel.updateWord(id, word)
                        coursesViewModel?.updateCourse(courseViewModel.course)
                    }
                },
                onRefreshRequested = learnWordsViewModel::randomizeCurrentWord,
                ended = learnWordsViewModel.ended,
                hidden = learnWordsViewModel.hidden,
                hide = learnWordsViewModel::hide
            )
        } else {
            LaunchedEffect(key1 = Unit) {
                context.toast("currentWord = null")
                navController.navigateUp()
            }
        }
    }
}

@Composable
fun DifficultWordsRoute(
    level: String?,
    limit: Int,
    prefsViewModel: PreferencesViewModel,
    player: MediaPlayer,
    tts: TextToSpeech,
    navController: NavController
) {
    val coursesViewModel = navController.coursesViewModel()
    val courseViewModel = navController.courseViewModel()
    val levelViewModel = navController.levelViewModel()

    val context = LocalContext.current

    if (courseViewModel != null) {
        val words by courseViewModel.words.collectAsState()
        val difficultWordsViewModel = viewModel<DifficultWordsViewModel>(
            factory = viewModelFactory {
                addInitializer(DifficultWordsViewModel::class) {
                    DifficultWordsViewModel(words, level, limit)
                }
            }
        )
        if (
            difficultWordsViewModel.currentId != null
            && difficultWordsViewModel.currentWord != null
            && difficultWordsViewModel.currentState != null
        ) {
            BackHandler(enabled = prefsViewModel.backGesture) {
                difficultWordsViewModel.randomizeCurrentWord()
            }

            DifficultWords(
                memorizedWords = difficultWordsViewModel.memorizedWords,
                player = player,
                tts = tts,
                locale = courseViewModel.locale,
                onWordRemoved = { id ->
                    difficultWordsViewModel.removeWord(id)
                    difficultWordsViewModel.randomizeCurrentWord()

                    levelViewModel?.removeWord(id)
                    courseViewModel.removeWord(id)
                    coursesViewModel?.updateCourse(courseViewModel.course)
                },
                onWordUpdated = { id, word, state ->
                    difficultWordsViewModel.updateWord(id, word, state)
                    difficultWordsViewModel.randomizeCurrentWord()

                    levelViewModel?.updateWord(id, word)
                    courseViewModel.updateWord(id, word)
                    coursesViewModel?.updateCourse(courseViewModel.course)
                },
                currentId = difficultWordsViewModel.currentId!!,
                currentWord = difficultWordsViewModel.currentWord!!,
                currentState = difficultWordsViewModel.currentState!!,
                getWord = courseViewModel::getWord,
                onSettingsActionClick = { navController.navigate(MainRoutes.Settings.route) },
                courseWords = words,
                otherLevels = courseViewModel.levels.collectAsState().value.keys,
                similarWords = prefsViewModel.similarWords,
                skippedWords = prefsViewModel.skippedWords,
                onNavigationIconClick = navController::popBackStack,
                progress = difficultWordsViewModel.progress,
                useTts = prefsViewModel.tts,
                papasHints = prefsViewModel.papasHints,
                onAnswer = { correct, hintsUsed ->
                    difficultWordsViewModel.answer(correct, hintsUsed) { id, word ->
                        levelViewModel?.updateWord(id, word)
                        courseViewModel.updateWord(id, word)
                        coursesViewModel?.updateCourse(courseViewModel.course)
                    }
                },
                onRefreshRequested = difficultWordsViewModel::randomizeCurrentWord,
                ended = difficultWordsViewModel.ended,
                hidden = difficultWordsViewModel.hidden,
                hide = difficultWordsViewModel::hide
            )
        } else {
            LaunchedEffect(key1 = Unit) {
                context.toast("currentWord = null")
                navController.navigateUp()
            }
        }
    }
}

@SuppressLint("InlinedApi")
@Composable
fun SettingsRoute(
    prefsViewModel: PreferencesViewModel,
    navController: NavController
) {
    val context = LocalContext.current

    val notifyPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            prefsViewModel.updateNotify(true)
        } else {
            context.toast("POST_NOTIFICATIONS permission is not granted.")
        }
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (!result.containsValue(false)) {
            prefsViewModel.updateCustomFolder(true)
            context.toast("Restart an app to apply changes")
        } else {
            context.toast(
                "WRITE_EXTERNAL_STORAGE and READ_EXTERNAL_STORAGE permissions are not granted."
            )
        }
    }

    val manageStorageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (Environment.isExternalStorageManager()) { // resultCode does not work.
            prefsViewModel.updateCustomFolder(true)
            context.toast("Restart an app to apply changes")
        } else {
            context.toast("ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION is not granted.")
        }
    }

    Settings(
        onNavigationIconClick = navController::popBackStack,
        markDifficult = prefsViewModel.markDifficult,
        onMarkDifficultChange = prefsViewModel::updateMarkDifficult,
        papasHints = prefsViewModel.papasHints,
        onPapasHintsChange = prefsViewModel::updatePapasHints,
        similarWords = prefsViewModel.similarWords,
        onSimilarWordsChange = prefsViewModel::updateSimilarWords,
        skippedWords = prefsViewModel.skippedWords,
        onSkippedWordsChange = prefsViewModel::updateSkippedWords,
        tts = prefsViewModel.tts,
        onTtsChange = prefsViewModel::updateTts,
        backGesture = prefsViewModel.backGesture,
        onBackGestureChange = prefsViewModel::updateBackGesture,
        notify = prefsViewModel.notify,
        onNotifyChange = { notify ->
            if (notify && !context.notifyPermissionGranted) {
                notifyPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                prefsViewModel.updateNotify(notify)
            }
        },
        notifyPer = prefsViewModel.notifyPer,
        onNotifyPerChange = prefsViewModel::updateNotifyPer,
        customFolder = prefsViewModel.useCustomFolder,
        onCustomFolderChange = { useCustomFolder ->
            if (useCustomFolder && !context.storagePermissionGranted) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    manageStorageLauncher.launch(
                        Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    )
                } else {
                    storagePermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                    )
                }
            } else {
                prefsViewModel.updateCustomFolder(useCustomFolder)
                context.toast("Restart an app to apply changes")
            }
        },
        customFolderPath = prefsViewModel.customFolderPath,
        onCustomFolderPathChange = { input ->
            prefsViewModel.updateCustomFolderPath(input)
            context.toast("Restart an app to apply changes")
        },
        dynamicColorsEnabled = prefsViewModel.dynamicColorsEnabled,
        dynamicColors = prefsViewModel.dynamicColors,
        onDynamicColorsChange = prefsViewModel::updateDynamicColors
    )
}

@Composable
inline fun <reified VM : ViewModel> NavController.viewModel(route: String) = runCatching {
    viewModel<VM>(viewModelStoreOwner = getBackStackEntry(route))
}

@Composable
fun NavController.coursesViewModel() =
    viewModel<CoursesViewModel>(route = MainRoutes.Courses.route).getOrNull()

@Composable
fun NavController.courseViewModel() =
    viewModel<CourseViewModel>(route = MainRoutes.Course.route).getOrNull()

@Composable
fun NavController.levelViewModel() =
    viewModel<LevelViewModel>(route = MainRoutes.Level.route).getOrNull()

@Composable
fun MainScreen(
    preferencesViewModel: PreferencesViewModel,
    player: MediaPlayer,
    tts: TextToSpeech
) {
    val navController = rememberNavController()
    val lifecycle = LocalLifecycleOwner.current.lifecycle // Pass top-level lifecycle.

    NavHost(
        navController = navController,
        startDestination = MainRoutes.Courses.route
    ) {
        composable(MainRoutes.Courses.route) {
            CoursesRoute(
                prefsViewModel = preferencesViewModel,
                navController = navController
            )
        }

        composable(MainRoutes.Course.route) { backStackEntry ->
            val course = backStackEntry.arguments?.getString("course")
            if (course != null) {
                CourseRoute(
                    course = course,
                    lifecycle = lifecycle,
                    prefsViewModel = preferencesViewModel,
                    navController = navController
                )
            }
        }

        composable(MainRoutes.Level.route) { backStackEntry ->
            val level = backStackEntry.arguments?.getString("level")
            if (level != null) {
                LevelRoute(
                    level = level,
                    prefsViewModel = preferencesViewModel,
                    player = player,
                    tts = tts,
                    navController = navController
                )
            }
        }

        composable(
            MainRoutes.Word.route,
            listOf(navArgument("id") { type = NavType.IntType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id")?.let { id ->
                if (id != -1) {
                    id
                } else {
                    null
                }
            }
            val level = backStackEntry.arguments?.getString("level")
            if (level != null) {
                WordRoute(
                    id = id,
                    level = level,
                    player = player,
                    tts = tts,
                    navController = navController
                )
            }
        }

        composable(
            MainRoutes.GuessingReview.route,
            listOf(navArgument("level") { nullable = true })
        ) { backStackEntry ->
            GuessingReviewRoute(
                level = backStackEntry.arguments?.getString("level"),
                limit = 25, // TODO
                prefsViewModel = preferencesViewModel,
                player = player,
                tts = tts,
                navController = navController
            )
        }

        composable(
            MainRoutes.TypingReview.route,
            listOf(navArgument("level") { nullable = true })
        ) { backStackEntry ->
            TypingReviewRoute(
                level = backStackEntry.arguments?.getString("level"),
                limit = 25, // TODO
                prefsViewModel = preferencesViewModel,
                player = player,
                tts = tts,
                navController = navController
            )
        }

        composable(
            MainRoutes.LearnNewWords.route,
            listOf(navArgument("level") { nullable = true })
        ) { backStackEntry ->
            LearnWordsRoute(
                level = backStackEntry.arguments?.getString("level"),
                limit = 5, // TODO
                prefsViewModel = preferencesViewModel,
                player = player,
                tts = tts,
                navController = navController
            )
        }

        composable(
            MainRoutes.DifficultWords.route,
            listOf(navArgument("level") { nullable = true })
        ) { backStackEntry ->
            DifficultWordsRoute(
                level = backStackEntry.arguments?.getString("level"),
                limit = 15, // TODO
                prefsViewModel = preferencesViewModel,
                player = player,
                tts = tts,
                navController = navController
            )
        }

        composable(MainRoutes.Settings.route) {
            SettingsRoute(
                prefsViewModel = preferencesViewModel,
                navController = navController
            )
        }
    }
}