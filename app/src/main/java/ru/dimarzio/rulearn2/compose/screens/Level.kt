package ru.dimarzio.rulearn2.compose.screens

import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ru.dimarzio.rulearn2.R
import ru.dimarzio.rulearn2.compose.AboutDialog
import ru.dimarzio.rulearn2.compose.AppBarActions
import ru.dimarzio.rulearn2.compose.NavigationIcon
import ru.dimarzio.rulearn2.compose.Sessions
import ru.dimarzio.rulearn2.compose.SingleChoiceDialog
import ru.dimarzio.rulearn2.compose.WordsListItem
import ru.dimarzio.rulearn2.models.Word
import ru.dimarzio.rulearn2.utils.toast
import ru.dimarzio.rulearn2.viewmodels.LevelViewModel
import ru.dimarzio.rulearn2.viewmodels.PreferencesViewModel.Session
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Level(
    level: String,
    onNavigationIconClick: () -> Unit,
    onAddActionClick: () -> Unit,
    sortMethod: LevelViewModel.SortMethod,
    onSortActionClick: (LevelViewModel.SortMethod) -> Unit,
    onSettingsActionClick: () -> Unit,
    selectedSession: Session,
    onLearnNewWordsClick: () -> Unit,
    onDifficultWordsClick: () -> Unit,
    onTypingReviewClick: () -> Unit,
    onGuessingReviewClick: () -> Unit,
    words: Map<Int, Word>,
    player: MediaPlayer,
    tts: TextToSpeech,
    locale: Locale,
    onWordRemoved: (Int) -> Unit,
    onWordUpdated: (Int, Word) -> Unit,
    onWordClick: (Int, String) -> Unit
) {
    val listState = rememberLazyListState()

    val scrollButtonVisible by remember {
        derivedStateOf {
            if (words.any { (_, word) -> word.rating >= 10 }) {
                val lastIndex = words.values.indexOfLast { word -> word.learned && !word.skip }
                lastIndex !in listState.layoutInfo.visibleItemsInfo.map(LazyListItemInfo::index)
            } else {
                false
            }
        }
    }

    val scrollButtonUp by remember {
        derivedStateOf {
            if (listState.layoutInfo.visibleItemsInfo.isNotEmpty()) {
                val lastIndex = words.values.indexOfLast { word -> word.learned && !word.skip }
                lastIndex < listState.layoutInfo.visibleItemsInfo.last().index
            } else {
                false
            }
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = level,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    NavigationIcon(onClick = onNavigationIconClick)
                },
                actions = {
                    TopBarActions(
                        onAddActionClick = onAddActionClick,
                        sortMethod = sortMethod,
                        onSortActionClick = onSortActionClick,
                        onSettingsActionClick = onSettingsActionClick
                    )
                }
            )
        },
        floatingActionButton = {
            Sessions(
                selectedMode = selectedSession,
                onLearnNewWordsClick = onLearnNewWordsClick,
                onDifficultWordsClick = onDifficultWordsClick,
                onTypingReviewClick = onTypingReviewClick,
                onGuessingReviewClick = onGuessingReviewClick,
                toLearnNumber = words.count { (_, word) -> !word.learned && !word.skip },
                difficultNumber = words.count { (_, word) -> word.isDifficult },
                toRepeatNumber = words.count { (_, word) -> word.isRepeat },
                scrollButtonVisible = scrollButtonVisible,
                onScrollClick = {
                    val lastIndex = words.values.indexOfLast { word -> word.learned && !word.skip }
                    coroutineScope.launch {
                        listState.scrollToItem(lastIndex)
                    }
                },
                scrollUp = scrollButtonUp,
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            val learnedNumber = words.count { (_, word) -> word.learned && !word.skip }

            val totalWords = words.count { (_, word) -> !word.skip }

            Text(
                text = "$learnedNumber/$totalWords learned",
                modifier = Modifier.padding(10.dp),
                fontSize = 16.sp,
                fontStyle = FontStyle.Italic
            )

            WordsList(
                state = listState,
                words = words,
                player = player,
                tts = tts,
                locale = locale,
                onWordRemoved = onWordRemoved,
                onWordUpdated = onWordUpdated,
                onWordClick = onWordClick
            )
        }

        val toRepeatNumber by remember(key1 = words) {
            mutableIntStateOf(words.count { (_, word) -> word.isRepeat })
        }

        LaunchedEffect(key1 = words) {
            if (toRepeatNumber > 0) {
                context.toast("$toRepeatNumber words for repetition are found!")
            }
        }
    }
}

@Composable
private fun SortDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: (LevelViewModel.SortMethod) -> Unit,
    sortMethod: LevelViewModel.SortMethod
) {
    SingleChoiceDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = "Sort",
        onConfirmation = onConfirmation,
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = "Cancel")
            }
        },
        title = "Sort by",
        items = enumValues<LevelViewModel.SortMethod>().toSet(),
        selected = sortMethod
    )
}

@Composable
private fun TopBarActions(
    onAddActionClick: () -> Unit,
    sortMethod: LevelViewModel.SortMethod,
    onSortActionClick: (LevelViewModel.SortMethod) -> Unit,
    onSettingsActionClick: () -> Unit,
) {
    var showSortDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    if (showSortDialog) {
        SortDialog(
            onDismissRequest = { showSortDialog = false },
            onConfirmation = onSortActionClick,
            sortMethod = sortMethod
        )
    }

    if (showAboutDialog) {
        AboutDialog {
            showAboutDialog = false
        }
    }

    val sortIcon = ImageVector.vectorResource(id = R.drawable.baseline_sort_24)
    AppBarActions(
        Triple(Icons.Filled.Add, "Add word", onAddActionClick),
        Triple(sortIcon, "Sort words") { showSortDialog = true },
        Triple(null, "Settings", onSettingsActionClick),
        Triple(null, "About") { showAboutDialog = true }
    )
}

@Composable
private fun WordsList(
    state: LazyListState,
    words: Map<Int, Word>,
    player: MediaPlayer,
    tts: TextToSpeech,
    locale: Locale,
    onWordRemoved: (Int) -> Unit,
    onWordUpdated: (Int, Word) -> Unit,
    onWordClick: (Int, String) -> Unit,
) {
    LazyColumn(state = state) {
        items(words.toList()) { (id, word) ->
            WordsListItem(
                word = word,
                onDeleteClick = { onWordRemoved(id) },
                onDifficultClick = { onWordUpdated(id, word.copy(difficult = it)) },
                onSkipClick = { onWordUpdated(id, word.copy(skip = it)) },
                player = player,
                tts = tts,
                locale = locale,
                onClick = { onWordClick(id, word.level) }
            )
        }
    }
}