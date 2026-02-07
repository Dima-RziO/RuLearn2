package ru.dimarzio.rulearn2.compose.screens.sessions.typing

import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import ru.dimarzio.rulearn2.models.Word
import ru.dimarzio.rulearn2.routes.SessionRoutes
import ru.dimarzio.rulearn2.utils.navigate
import ru.dimarzio.rulearn2.utils.play
import ru.dimarzio.rulearn2.viewmodels.WordViewModel
import ru.dimarzio.rulearn2.viewmodels.sessions.TypingTestViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypingReview(
    currentId: Int,
    currentWord: Word,
    repeatedWords: Map<Int, Word>,
    player: MediaPlayer,
    tts: TextToSpeech,
    locale: Locale,
    onWordRemoved: (Int) -> Unit,
    onWordUpdated: (Int, Word) -> Unit,
    courseWords: Map<Int, Word>,
    otherLevels: Set<String>,
    correctAnswers: Int,
    onNavigationIconClick: () -> Unit,
    progress: Float,
    useTts: Boolean,
    papasHints: Boolean,
    onAnswer: (Boolean, Int) -> Unit,
    onRefreshRequested: () -> Unit,
    ended: Boolean,
    onSettingsActionClick: () -> Unit
) {
    val scaffoldState = rememberBottomSheetScaffoldState()
    val navController = rememberNavController()
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
            startDestination = SessionRoutes.TypingTest.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(SessionRoutes.TypingTest.route) {
                val typingTestViewModel = viewModel<TypingTestViewModel>()

                LaunchedEffect(key1 = currentWord, key2 = currentId) {
                    if (currentWord.isRepeat && !ended) {
                        typingTestViewModel.reset()
                    }
                }

                TypingTest(
                    title = "Correct: $correctAnswers",
                    onNavigationIconClick = onNavigationIconClick,
                    onSettingsActionClick = onSettingsActionClick,
                    onRefreshActionClick = onRefreshRequested,
                    progress = progress,
                    word = currentWord,
                    onLearnedClick = { onWordUpdated(currentId, currentWord.copy(skip = true)) },
                    onDifficultClick = {
                        onWordUpdated(currentId, currentWord.copy(difficult = it))
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
                            navController.navigateUp()
                        },
                        onSettingsActionClick = onSettingsActionClick,
                        onAudioClick = player::play,
                        onWordUpdated = wordViewModel::updateWord,
                        onAccessedClick = wordViewModel::pickAccessed,
                        levelsForName = levelsForName,
                        levelsForTranslation = levelsForTranslation,
                        otherLevels = otherLevels,
                        onWordSaved = {
                            onWordUpdated(id, wordViewModel.save())
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