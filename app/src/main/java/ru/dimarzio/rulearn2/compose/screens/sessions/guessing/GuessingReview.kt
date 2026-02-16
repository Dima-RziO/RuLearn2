package ru.dimarzio.rulearn2.compose.screens.sessions.guessing

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
import ru.dimarzio.rulearn2.compose.screens.Word
import ru.dimarzio.rulearn2.models.Word
import ru.dimarzio.rulearn2.routes.SessionRoutes
import ru.dimarzio.rulearn2.utils.navigate
import ru.dimarzio.rulearn2.utils.play
import ru.dimarzio.rulearn2.utils.toast
import ru.dimarzio.rulearn2.viewmodels.WordViewModel
import ru.dimarzio.rulearn2.viewmodels.sessions.GuessingTestViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuessingReview(
    currentId: Int,
    currentWord: Word,
    repeatedWords: Map<Int, Word>,
    player: MediaPlayer,
    tts: TextToSpeech,
    locale: Locale, // For tts.
    onWordRemoved: (Int) -> Unit,
    onWordUpdated: (Int, Word) -> Unit,
    courseWords: Map<Int, Word>,
    otherLevels: Set<String>,
    similarWords: Boolean,
    skippedWords: Boolean,
    correctAnswers: Int,
    onNavigationIconClick: () -> Unit,
    progress: Float,
    useTts: Boolean,
    onAnswer: (Boolean) -> Unit,
    onRefreshRequested: () -> Unit,
    ended: Boolean,
    hidden: Boolean,
    hide: (Boolean) -> Unit,
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
                val guessingTestViewModel = viewModel<GuessingTestViewModel>()

                var error by remember { mutableStateOf(null as String?) }

                if (error != null) {
                    ErrorDialog(
                        onDismissRequest = { error = null },
                        message = error!!
                    )
                }

                LaunchedEffect(key1 = currentWord, key2 = currentId) {
                    if (currentWord.isRepeat && !ended) {
                        guessingTestViewModel.reverse()
                        hide(false)

                        coroutineScope.launch {
                            val result = guessingTestViewModel.generateTranslations(
                                this,
                                currentId,
                                currentWord,
                                courseWords,
                                similarWords,
                                skippedWords
                            )

                            result.await()?.let { e ->
                                e.localizedMessage?.let { error = it } ?: context.toast("Message")
                            }
                        }
                    }
                }

                GuessingTest(
                    title = "Correct: $correctAnswers",
                    onNavigationIconClick = onNavigationIconClick,
                    onSettingsActionClick = onSettingsActionClick,
                    onRefreshActionClick = onRefreshRequested,
                    progress = progress,
                    reversed = guessingTestViewModel.reversed,
                    word = currentWord,
                    onLearnedClick = { onWordUpdated(currentId, currentWord.copy(skip = true)) },
                    onDifficultClick = {
                        onWordUpdated(currentId, currentWord.copy(difficult = it))
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

                    Word(
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