package ru.dimarzio.rulearn2.compose.screens.sessions.joint

import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import ru.dimarzio.rulearn2.compose.ErrorDialog
import ru.dimarzio.rulearn2.compose.SessionContainer
import ru.dimarzio.rulearn2.compose.screens.sessions.guessing.GuessingTest
import ru.dimarzio.rulearn2.compose.screens.sessions.typing.TypingTest
import ru.dimarzio.rulearn2.models.Word
import ru.dimarzio.rulearn2.routes.SessionRoutes
import ru.dimarzio.rulearn2.utils.navigate
import ru.dimarzio.rulearn2.utils.navigateCleaning
import ru.dimarzio.rulearn2.utils.play
import ru.dimarzio.rulearn2.utils.toast
import ru.dimarzio.rulearn2.viewmodels.DifficultWordsViewModel.DifficultState
import ru.dimarzio.rulearn2.viewmodels.WordViewModel
import ru.dimarzio.rulearn2.viewmodels.sessions.GuessingTestViewModel
import ru.dimarzio.rulearn2.viewmodels.sessions.TypingTestViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DifficultWords(
    memorizedWords: Map<Int, Word>,
    player: MediaPlayer,
    tts: TextToSpeech,
    locale: Locale,
    onWordRemoved: (Int) -> Unit,
    onWordUpdated: (Int, Word, DifficultState) -> Unit,
    currentId: Int,
    currentWord: Word,
    currentState: DifficultState,
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
        onWordUpdated = { id, word -> onWordUpdated(id, word, currentState) },
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
                                WordViewModel(courseWords, id, null)
                            }
                        }
                    )

                    val levelsForName by remember(
                        key1 = courseWords,
                        key2 = wordViewModel.newWord
                    ) {
                        mutableStateOf(wordViewModel.getLevelsForName(courseWords))
                    }

                    val levelsForTranslation by remember(
                        key1 = courseWords,
                        key2 = wordViewModel.newWord
                    ) {
                        mutableStateOf(wordViewModel.getLevelsForTranslation(courseWords))
                    }

                    ru.dimarzio.rulearn2.compose.screens.Word(
                        id = id,
                        word = wordViewModel.newWord,
                        modified = wordViewModel.modified,
                        tts = tts,
                        locale = locale,
                        onNavigationIconClick = navController::navigateUp,
                        onDeleteActionClick = {
                            onWordRemoved(id)
                            navController.popBackStack()
                        },
                        onSettingsActionClick = onSettingsActionClick,
                        onAudioClick = player::play,
                        onWordUpdated = wordViewModel::updateWord,
                        onAccessedClick = wordViewModel::pickAccessed,
                        levelsForName = levelsForName,
                        levelsForTranslation = levelsForTranslation,
                        otherLevels = otherLevels,
                        onWordSaved = {
                            // Reset the state
                            onWordUpdated(id, wordViewModel.save(), DifficultState.None)
                            navController.popBackStack()
                        }
                    )
                }
            }

            composable(SessionRoutes.GuessingTest.route) {
                val guessingTestViewModel = viewModel<GuessingTestViewModel>()

                var error by remember { mutableStateOf(null as String?) }

                if (error != null) {
                    ErrorDialog(
                        onDismissRequest = { error = null },
                        message = error!!
                    )
                }

                LaunchedEffect(key1 = currentWord, key2 = currentState, key3 = currentId) {
                    if (currentState == DifficultState.None || currentState == DifficultState.Typing) {
                        navController.navigateCleaning(SessionRoutes.TypingTest.route)
                        hide(false)
                    } else {
                        guessingTestViewModel.reverse()
                        hide(false)

                        coroutineScope.launch {
                            val deferred = guessingTestViewModel.generateTranslations(
                                this,
                                currentId,
                                currentWord,
                                courseWords,
                                similarWords,
                                skippedWords
                            )

                            deferred.await()?.let { e ->
                                e.localizedMessage?.let { error = it } ?: context.toast("Message")
                            }
                        }
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
                        onWordUpdated(currentId, currentWord.copy(skip = true), currentState)
                    },
                    onDifficultClick = {
                        onWordUpdated(currentId, currentWord.copy(difficult = it), currentState)
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
                    if (currentState == DifficultState.None || currentState == DifficultState.Typing) {
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
                        onWordUpdated(currentId, currentWord.copy(skip = true), currentState)
                    },
                    onDifficultClick = {
                        onWordUpdated(currentId, currentWord.copy(difficult = it), currentState)
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
                    helperText = currentState == DifficultState.None,
                    onDoneClick = {
                        if (currentState != DifficultState.None) {
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