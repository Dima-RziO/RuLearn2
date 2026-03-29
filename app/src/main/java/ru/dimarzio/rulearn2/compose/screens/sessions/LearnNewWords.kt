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
import ru.dimarzio.rulearn2.compose.screens.sessions.tests.NewWord
import ru.dimarzio.rulearn2.compose.screens.sessions.tests.TypingTest
import ru.dimarzio.rulearn2.models.Word
import ru.dimarzio.rulearn2.routes.SessionRoutes
import ru.dimarzio.rulearn2.tflite.TFLiteModel
import ru.dimarzio.rulearn2.utils.navigate
import ru.dimarzio.rulearn2.utils.navigateCleaning
import ru.dimarzio.rulearn2.utils.play
import ru.dimarzio.rulearn2.viewmodels.ErrorHandler
import ru.dimarzio.rulearn2.viewmodels.WordViewModel
import ru.dimarzio.rulearn2.viewmodels.sessions.tests.GuessingTestViewModel
import ru.dimarzio.rulearn2.viewmodels.sessions.tests.NewWordViewModel
import ru.dimarzio.rulearn2.viewmodels.sessions.tests.TypingTestViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnNewWords(
    learnedWords: Map<Int, Word>,
    player: MediaPlayer,
    tts: TextToSpeech,
    locale: Locale,
    onWordRemoved: (Int, Word) -> Unit,
    onWordUpdated: (Int, Word?, Word) -> Unit,
    currentId: Int,
    currentWord: Word,
    handler: ErrorHandler,
    getWord: (Int) -> Word?,
    onSettingsActionClick: () -> Unit,
    courseWords: Map<Int, Word>,
    otherLevels: Set<String>,
    similarWords: Boolean,
    skippedWords: Boolean,
    onNavigationIconClick: () -> Unit,
    progress: Float,
    useTts: Boolean,
    papasHints: Boolean,
    model: TFLiteModel?,
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
        label = learnedWords.size.toString() + " learned",
        words = learnedWords,
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
            modifier = Modifier.padding(innerPadding),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None }
        ) {
            composable(SessionRoutes.NewWord.route) {
                val newWordViewModel = viewModel<NewWordViewModel>()

                LaunchedEffect(key1 = currentWord, key2 = currentId) {
                    if (!ended) {
                        if (currentWord.rating in 1..8) {
                            navController.navigateCleaning(SessionRoutes.GuessingTest.route)
                        } else if (currentWord.rating == 9) {
                            navController.navigateCleaning(SessionRoutes.TypingTest.route)
                        }

                        hide(false)
                    }
                }

                NewWord(
                    title = "Learned: " + learnedWords.size,
                    word = currentWord,
                    tts = tts,
                    locale = locale,
                    onNavigationIconClick = onNavigationIconClick,
                    onRefreshActionClick = onRefreshRequested,
                    onAudioClick = player::play,
                    onSettingsActionClick = onSettingsActionClick,
                    progress = progress,
                    onLearnedClick = {
                        onWordUpdated(currentId, currentWord, currentWord.copy(rating = 10))
                    },
                    onDifficultClick = {
                        onWordUpdated(currentId, currentWord, currentWord.copy(difficult = it))
                    },
                    ended = ended,
                    hidden = hidden,
                    onContinueClick = {
                        newWordViewModel.next(context, player, tts, locale, currentWord)
                        onWordUpdated(currentId, currentWord, currentWord.copy(rating = 1))
                    },
                    maxRating = 10
                )
            }

            composable(SessionRoutes.GuessingTest.route) {
                val guessingTestViewModel = viewModel<GuessingTestViewModel>(
                    factory = viewModelFactory {
                        addInitializer(GuessingTestViewModel::class) {
                            GuessingTestViewModel(handler)
                        }
                    }
                )

                LaunchedEffect(key1 = currentWord, key2 = currentId) {
                    if (!ended) {
                        when (currentWord.rating) {
                            0 -> {
                                navController.navigateCleaning(SessionRoutes.NewWord.route)
                                hide(false)
                            }

                            in 1..8 -> {
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

                            9 -> {
                                navController.navigateCleaning(SessionRoutes.TypingTest.route)
                                hide(false)
                            }
                        }
                    }
                }

                GuessingTest(
                    title = "Learned: " + learnedWords.size,
                    onNavigationIconClick = onNavigationIconClick,
                    onSettingsActionClick = onSettingsActionClick,
                    onRefreshActionClick = onRefreshRequested,
                    progress = progress,
                    reversed = guessingTestViewModel.reversed,
                    word = currentWord,
                    onLearnedClick = {
                        onWordUpdated(currentId, currentWord, currentWord.copy(rating = 10))
                    },
                    onDifficultClick = {
                        onWordUpdated(currentId, currentWord, currentWord.copy(difficult = it))
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

                        onAnswer(clicked == currentId, 0)
                    },
                    onTranslationLongClick = { id ->
                        navController.navigate(SessionRoutes.Word.route, "id" to id)
                    },
                    maxRating = 10
                )
            }

            composable(SessionRoutes.TypingTest.route) {
                val typingTestViewModel = viewModel<TypingTestViewModel>()

                LaunchedEffect(key1 = currentWord, key2 = currentId) {
                    if (!ended) {
                        when (currentWord.rating) {
                            0 -> {
                                navController.navigateCleaning(SessionRoutes.NewWord.route)
                            }

                            in 1..8 -> {
                                navController.navigateCleaning(SessionRoutes.GuessingTest.route)
                            }

                            9 -> {
                                typingTestViewModel.reset()
                            }
                        }

                        hide(false)
                    }
                }

                TypingTest(
                    title = "Learned: " + learnedWords.size,
                    onNavigationIconClick = onNavigationIconClick,
                    onSettingsActionClick = onSettingsActionClick,
                    onRefreshActionClick = onRefreshRequested,
                    progress = progress,
                    word = currentWord,
                    onLearnedClick = {
                        onWordUpdated(currentId, currentWord, currentWord.copy(rating = 10))
                    },
                    onDifficultClick = {
                        onWordUpdated(currentId, currentWord, currentWord.copy(difficult = it))
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
                    helperText = false,
                    onDoneClick = {
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
                    },
                    maxRating = 10,
                    hidden = hidden
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

                    ru.dimarzio.rulearn2.compose.screens.Word(
                        id = wordViewModel.newId,
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
                            onWordUpdated(id, wordViewModel.oldWord, wordViewModel.save())
                            navController.popBackStack()
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