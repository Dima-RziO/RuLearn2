package ru.dimarzio.rulearn2.compose.screens.sessions

import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import ru.dimarzio.rulearn2.compose.SessionContainer
import ru.dimarzio.rulearn2.compose.screens.Word
import ru.dimarzio.rulearn2.compose.screens.sessions.tests.GuessingTest
import ru.dimarzio.rulearn2.models.Word
import ru.dimarzio.rulearn2.routes.SessionRoutes
import ru.dimarzio.rulearn2.tflite.TFLiteModel
import ru.dimarzio.rulearn2.utils.navigate
import ru.dimarzio.rulearn2.utils.play
import ru.dimarzio.rulearn2.viewmodels.ErrorHandler
import ru.dimarzio.rulearn2.viewmodels.PreferencesViewModel
import ru.dimarzio.rulearn2.viewmodels.WordViewModel
import ru.dimarzio.rulearn2.viewmodels.sessions.SessionWord
import ru.dimarzio.rulearn2.viewmodels.sessions.tests.GuessingTestViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuessingReview(
    currentId: Int,
    currentWord: Word,
    navigationEvents: Flow<Pair<String, SessionWord>>,
    repeatedWords: Map<Int, Word>,
    player: MediaPlayer,
    tts: TextToSpeech,
    locale: Locale, // For tts.
    onWordRemoved: (Int, Word) -> Unit,
    onWordUpdated: (Int, Word?, Word) -> Unit,
    handler: ErrorHandler,
    courseWords: Map<Int, Word>,
    otherLevels: Set<String>,
    onNavigationIconClick: () -> Unit,
    progress: Float,
    onAnswer: (Boolean) -> Unit,
    onRefreshRequested: () -> Unit,
    ended: Boolean,
    model: TFLiteModel?,
    getWord: (Int) -> Word?,
    onSettingsActionClick: () -> Unit
) {
    val navController = rememberNavController()
    val scaffoldState = rememberBottomSheetScaffoldState()
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current

    SessionContainer(
        scaffoldState = scaffoldState,
        label = repeatedWords.size.toString() + " shown",
        words = repeatedWords,
        player = player,
        tts = tts,
        locale = locale,
        onWordRemoved = onWordRemoved,
        onWordUpdated = onWordUpdated,
        onWordClick = { id ->
            coroutineScope.launch {
                scaffoldState.bottomSheetState.partialExpand()
            }
            navController.navigate(SessionRoutes.Word.route, "id" to id)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = SessionRoutes.GuessingTest.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(SessionRoutes.GuessingTest.route) {
                val guessingTestViewModel = viewModel<GuessingTestViewModel>(
                    factory = viewModelFactory {
                        addInitializer(GuessingTestViewModel::class) {
                            GuessingTestViewModel(handler)
                        }
                    }
                )

                LaunchedEffect(Unit) {
                    guessingTestViewModel.generateTranslations(currentId, currentWord, courseWords)
                    navigationEvents.collect { (_, word) ->
                        guessingTestViewModel.reverse()
                        guessingTestViewModel.generateTranslations(
                            id = word.getId(),
                            word = word.getWord(),
                            courseWords = courseWords,
                        )
                    }
                }

                GuessingTest(
                    title = "Repeated: " + repeatedWords.size,
                    onNavigationIconClick = onNavigationIconClick,
                    onSettingsActionClick = onSettingsActionClick,
                    onRefreshActionClick = onRefreshRequested,
                    progress = progress,
                    reversed = guessingTestViewModel.reversed,
                    word = currentWord,
                    onLearnedClick = {
                        onWordUpdated(currentId, currentWord, currentWord.copy(skip = true))
                    },
                    onDifficultClick = {
                        onWordUpdated(currentId, currentWord, currentWord.copy(difficult = it))
                    },
                    loading = guessingTestViewModel.loading,
                    translations = guessingTestViewModel.translations,
                    getWord = getWord,
                    ended = ended,
                    onAnswer = { clicked ->
                        guessingTestViewModel.answer(
                            context = context,
                            player = player,
                            tts = tts.takeIf { PreferencesViewModel.settings.tts },
                            locale = locale,
                            correctId = currentId,
                            correctWord = currentWord,
                            clicked = clicked,
                            onRefreshRequested = onRefreshRequested
                        )
                        onAnswer(currentId == clicked)
                    },
                    onTranslationLongClick = { id ->
                        navController.navigate(SessionRoutes.Word.route, "id" to id)
                    }
                )
            }

            composable(
                SessionRoutes.Word.route,
                arguments = listOf(navArgument("id") { type = NavType.IntType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getInt("id")

                if (id != null) {
                    val wordViewModel = viewModel<WordViewModel>(
                        factory = viewModelFactory {
                            addInitializer(WordViewModel::class) {
                                WordViewModel(model, courseWords, id, null)
                            }
                        }
                    )

                    Word(
                        id = id,
                        word = wordViewModel.newWord,
                        modified = wordViewModel.modified,
                        tts = tts,
                        locale = locale,
                        onNavigationIconClick = navController::navigateUp,
                        onDeleteActionClick = {
                            if (wordViewModel.oldWord != null) {
                                onWordRemoved(id, wordViewModel.oldWord)
                            }
                            navController.navigateUp()
                        },
                        onSettingsActionClick = onSettingsActionClick,
                        onAudioClick = player::play,
                        onWordUpdated = wordViewModel::updateWord,
                        onAccessedClick = wordViewModel::pickAccessed,
                        levelsForName = wordViewModel.levelsForName,
                        levelsForTranslation = wordViewModel.levelsForTranslation,
                        otherLevels = otherLevels,
                        onWordSaved = {
                            onWordUpdated(id, wordViewModel.oldWord, wordViewModel.save())
                            navController.navigateUp()
                        }
                    )
                }
            }
        }
    }

    LaunchedEffect(key1 = ended) {
        if (ended) {
            scaffoldState.bottomSheetState.expand()
        }
    }
}