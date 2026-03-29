package ru.dimarzio.rulearn2.compose.screens.sessions

import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
import kotlinx.coroutines.launch
import ru.dimarzio.rulearn2.compose.SessionContainer
import ru.dimarzio.rulearn2.compose.screens.sessions.tests.GuessingTest
import ru.dimarzio.rulearn2.compose.screens.sessions.tests.TypingTest
import ru.dimarzio.rulearn2.models.Word
import ru.dimarzio.rulearn2.routes.SessionRoutes
import ru.dimarzio.rulearn2.tflite.TFLiteModel
import ru.dimarzio.rulearn2.utils.navigate
import ru.dimarzio.rulearn2.utils.navigateCleaning
import ru.dimarzio.rulearn2.utils.play
import ru.dimarzio.rulearn2.viewmodels.ErrorHandler
import ru.dimarzio.rulearn2.viewmodels.WordViewModel
import ru.dimarzio.rulearn2.viewmodels.sessions.difficult.DifficultState
import ru.dimarzio.rulearn2.viewmodels.sessions.difficult.NoneState
import ru.dimarzio.rulearn2.viewmodels.sessions.difficult.TypingState
import ru.dimarzio.rulearn2.viewmodels.sessions.tests.GuessingTestViewModel
import ru.dimarzio.rulearn2.viewmodels.sessions.tests.TypingTestViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DifficultWords(
    memorizedWords: Map<Int, Word>,
    player: MediaPlayer,
    tts: TextToSpeech,
    locale: Locale,
    model: TFLiteModel?,
    onWordRemoved: (Int, Word) -> Unit,
    onWordUpdated: (Int, Word?, Word, DifficultState) -> Unit,
    currentId: Int,
    currentWord: Word,
    currentState: DifficultState,
    getWord: (Int) -> Word?,
    onSettingsActionClick: () -> Unit,
    courseWords: Map<Int, Word>,
    otherLevels: Set<String>,
    handler: ErrorHandler,
    similarWords: Boolean,
    skippedWords: Boolean,
    onNavigationIconClick: () -> Unit,
    progress: Float,
    useTts: Boolean,
    papasHints: Boolean,
    onAnswer: (Boolean, Int) -> Unit,
    onRefreshRequested: () -> Unit,
    ended: Boolean,
    hidden: Boolean,
    hide: (Boolean) -> Unit
) {
    val scaffoldState = rememberBottomSheetScaffoldState()
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current

    SessionContainer(
        scaffoldState = scaffoldState,
        label = memorizedWords.size.toString() + " memorized",
        words = memorizedWords,
        player = player,
        tts = tts,
        locale = locale,
        onWordRemoved = onWordRemoved,
        onWordUpdated = { id, old, new -> onWordUpdated(id, old, new, currentState) },
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
            modifier = Modifier.padding(innerPadding),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None }
        ) {
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

                    ru.dimarzio.rulearn2.compose.screens.Word(
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
                            navController.popBackStack()
                        },
                        onSettingsActionClick = onSettingsActionClick,
                        onAudioClick = player::play,
                        onWordUpdated = wordViewModel::updateWord,
                        onAccessedClick = wordViewModel::pickAccessed,
                        levelsForName = wordViewModel.levelsForName,
                        levelsForTranslation = wordViewModel.levelsForTranslation,
                        otherLevels = otherLevels,
                        onWordSaved = {
                            // Reset the state
                            onWordUpdated(
                                id,
                                wordViewModel.oldWord,
                                wordViewModel.save(),
                                NoneState
                            )
                            navController.popBackStack()
                        }
                    )
                }
            }

            composable(SessionRoutes.GuessingTest.route) {
                val guessingTestViewModel = viewModel<GuessingTestViewModel>(
                    factory = viewModelFactory {
                        addInitializer(GuessingTestViewModel::class) {
                            GuessingTestViewModel(handler)
                        }
                    }
                )

                LaunchedEffect(key1 = currentWord, key2 = currentState, key3 = currentId) {
                    if (currentState == NoneState || currentState == TypingState) {
                        navController.navigateCleaning(SessionRoutes.TypingTest.route)
                        hide(false)
                    } else {
                        guessingTestViewModel.reverse()
                        hide(false)

                        guessingTestViewModel.generateTranslations(
                            id = currentId,
                            word = currentWord,
                            courseWords = courseWords,
                            similarWords = similarWords,
                            skippedWords = skippedWords
                        )
                    }
                }

                GuessingTest(
                    title = "Memorized: " + memorizedWords.size,
                    onNavigationIconClick = onNavigationIconClick,
                    onSettingsActionClick = onSettingsActionClick,
                    onRefreshActionClick = onRefreshRequested,
                    progress = progress,
                    reversed = guessingTestViewModel.reversed,
                    word = currentWord,
                    onLearnedClick = {
                        val new = currentWord.copy(skip = true)
                        onWordUpdated(currentId, currentWord, new, currentState)
                    },
                    onDifficultClick = {
                        val new = currentWord.copy(difficult = it)
                        onWordUpdated(currentId, currentWord, new, currentState)
                    },
                    loading = guessingTestViewModel.loading,
                    translations = guessingTestViewModel.translations,
                    getWord = getWord,
                    ended = ended,
                    hidden = hidden,
                    onAnswer = { clicked ->
                        guessingTestViewModel.answer(
                            context = context,
                            player = player,
                            tts = tts.takeIf { useTts },
                            locale = locale,
                            correctId = currentId,
                            correctWord = currentWord,
                            clicked = clicked,
                            onRefreshRequested = onRefreshRequested
                        )

                        onAnswer(currentId == clicked, 0)
                    },
                    onTranslationLongClick = { id ->
                        navController.navigate(SessionRoutes.Word.route, "id" to id)
                    }
                )
            }

            composable(SessionRoutes.TypingTest.route) {
                val typingTestViewModel = viewModel<TypingTestViewModel>()

                LaunchedEffect(key1 = currentWord, key2 = currentState, key3 = currentId) {
                    if (currentState == NoneState || currentState == TypingState) {
                        typingTestViewModel.reset()
                        hide(false)
                    } else {
                        navController.navigateCleaning(SessionRoutes.GuessingTest.route)
                        hide(false)
                    }
                }

                TypingTest(
                    title = "Memorized: " + memorizedWords.size,
                    onNavigationIconClick = onNavigationIconClick,
                    onSettingsActionClick = onSettingsActionClick,
                    onRefreshActionClick = onRefreshRequested,
                    progress = progress,
                    word = currentWord,
                    onLearnedClick = {
                        val new = currentWord.copy(skip = true)
                        onWordUpdated(currentId, currentWord, new, currentState)
                    },
                    onDifficultClick = {
                        val new = currentWord.copy(difficult = it)
                        onWordUpdated(currentId, currentWord, new, currentState)
                    },
                    inputValue = typingTestViewModel.inputValue,
                    onInputValueChange = { value ->
                        if (
                            typingTestViewModel.type(
                                context = context,
                                player = player,
                                tts = tts.takeIf { useTts },
                                locale = locale,
                                correct = currentWord,
                                value = value,
                                onRefreshRequested = onRefreshRequested
                            )
                        ) {
                            onAnswer(true, typingTestViewModel.hintsUsed)
                        }
                    },
                    inputEnabled = typingTestViewModel.inputEnabled && !ended,
                    onHintClick = {
                        if (
                            typingTestViewModel.takeHint(
                                context = context,
                                player = player,
                                tts = tts.takeIf { useTts },
                                locale = locale,
                                papasHints = papasHints,
                                correct = currentWord,
                                onRefreshRequested = onRefreshRequested
                            )
                        ) {
                            onAnswer(true, typingTestViewModel.hintsUsed)
                        }
                    },
                    error = typingTestViewModel.error,
                    helperText = currentState == NoneState,
                    onDoneClick = {
                        if (currentState != NoneState) {
                            typingTestViewModel.answer(
                                context = context,
                                player = player,
                                tts = tts.takeIf { useTts },
                                locale = locale,
                                word = currentWord,
                                correct = false,
                                onRefreshRequested = onRefreshRequested
                            )
                            onAnswer(false, typingTestViewModel.hintsUsed)
                        }
                    },
                    hidden = hidden
                )
            }
        }
    }

    LaunchedEffect(key1 = ended) {
        if (ended) {
            scaffoldState.bottomSheetState.expand()
        }
    }
}