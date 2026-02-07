package ru.dimarzio.rulearn2.compose.screens.sessions.guessing

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.dimarzio.rulearn2.compose.AboutDialog
import ru.dimarzio.rulearn2.compose.AppBarActions
import ru.dimarzio.rulearn2.compose.AutoSizeText
import ru.dimarzio.rulearn2.compose.NavigationIcon
import ru.dimarzio.rulearn2.compose.SessionOptions
import ru.dimarzio.rulearn2.compose.WordIndicator
import ru.dimarzio.rulearn2.models.Word
import ru.dimarzio.rulearn2.viewmodels.sessions.GuessingTestViewModel.TranslationState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuessingTest(
    title: String,
    onNavigationIconClick: () -> Unit,
    onRefreshActionClick: () -> Unit,
    onSettingsActionClick: () -> Unit,
    progress: Float,
    reversed: Boolean,
    word: Word,
    onLearnedClick: () -> Unit,
    onDifficultClick: (Boolean) -> Unit,
    loading: Boolean,
    translations: Map<Int, TranslationState>,
    getWord: (Int) -> Word?,
    ended: Boolean,
    hidden: Boolean,
    onAnswer: (Int) -> Unit,
    onTranslationLongClick: (Int) -> Unit,
    maxRating: Int = Word.MAX_RATING
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = title)
                },
                navigationIcon = {
                    NavigationIcon(onClick = onNavigationIconClick)
                },
                actions = {
                    TopBarActions(
                        onSettingsActionClick = onSettingsActionClick,
                        onRefreshActionClick = onRefreshActionClick
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            LinearProgressIndicator(
                progress = { progress / 100 },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.size(10.dp))

            Row(
                modifier = Modifier
                    .weight(0.2f)
                    .padding(10.dp)
            ) {
                AutoSizeText(
                    text = if (!hidden) {
                        if (!reversed) {
                            word.name
                        } else {
                            word.translation
                        }
                    } else {
                        "..."
                    },
                    maxFontSize = 24.sp,
                    minFontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(50.dp))

                Column {
                    WordIndicator(
                        word = word,
                        maxRating = maxRating
                    )

                    SessionOptions(
                        enabled = !ended && translations.values.all { it == TranslationState.None },
                        difficult = word.difficult,
                        onLearnedClick = onLearnedClick,
                        onDifficultClick = onDifficultClick
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(0.8f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (!loading) {
                    TranslationsGrid(
                        reversed = reversed,
                        translations = translations,
                        getWord = getWord,
                        ended = ended,
                        onTranslationClick = onAnswer,
                        onTranslationLongClick = onTranslationLongClick
                    )
                } else {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun TopBarActions(
    onRefreshActionClick: () -> Unit,
    onSettingsActionClick: () -> Unit,
) {
    var showAboutDialog by remember { mutableStateOf(false) }

    if (showAboutDialog) {
        AboutDialog {
            showAboutDialog = false
        }
    }

    AppBarActions(
        Triple(Icons.Filled.Refresh, "Refresh", onRefreshActionClick),
        Triple(null, "Settings", onSettingsActionClick),
        Triple(null, "About") { showAboutDialog = true }
    )
}

@Composable
private fun Translation(
    name: String,
    translation: String,
    reversed: Boolean,
    state: TranslationState,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = when (state) {
                TranslationState.None -> Color.Unspecified
                TranslationState.Correct -> MaterialTheme.colorScheme.primaryContainer
                TranslationState.Wrong -> MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AutoSizeText(
                text = if (reversed) name else translation,
                maxFontSize = 20.sp,
                minFontSize = 12.sp,
                textAlign = TextAlign.Center
            )

            if (state == TranslationState.Wrong) {
                AutoSizeText(
                    text = if (reversed) translation else name,
                    maxFontSize = 20.sp,
                    minFontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun TranslationsGrid(
    reversed: Boolean,
    translations: Map<Int, TranslationState>,
    getWord: (Int) -> Word?,
    ended: Boolean,
    onTranslationClick: (Int) -> Unit,
    onTranslationLongClick: (Int) -> Unit
) {
    FlowRow(
        maxItemsInEachRow = 2,
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        translations.forEach { (id, state) ->
            val name by remember(key1 = translations) { derivedStateOf { getWord(id)?.name } }
            val translation by remember(key1 = translations) {
                derivedStateOf { getWord(id)?.translation }
            }

            if (name != null && translation != null) {
                Translation(
                    name = name!!,
                    translation = translation!!,
                    reversed = reversed,
                    state = state,
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .fillMaxHeight(0.333f) // 1f / 3
                        .padding(5.dp)
                        .combinedClickable(
                            enabled = !ended && translations.values.all {
                                it == TranslationState.None
                            },
                            onClick = {
                                onTranslationClick(id)
                            },
                            onLongClick = {
                                onTranslationLongClick(id)
                            }
                        )
                )
            }
        }
    }
}