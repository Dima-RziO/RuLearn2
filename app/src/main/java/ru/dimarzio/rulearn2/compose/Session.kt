package ru.dimarzio.rulearn2.compose

import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ru.dimarzio.rulearn2.R
import ru.dimarzio.rulearn2.models.Word
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SessionContainer(
    scaffoldState: BottomSheetScaffoldState,
    label: String,
    words: Map<Int, Word>,
    player: MediaPlayer,
    tts: TextToSpeech,
    locale: Locale,
    onWordRemoved: (Int) -> Unit,
    onWordUpdated: (Int, Word) -> Unit,
    onWordClick: (Int) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetContent = {
            BoxWithConstraints {
                val topBarHeight = 64.dp // 64.dp is a standard height.
                val indicatorHeight = 4.dp // 4.dp is a standard height.

                val padding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
                val maxSheetHeight = this.maxHeight - topBarHeight - indicatorHeight - padding

                Column(modifier = Modifier.heightIn(max = maxSheetHeight)) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(10.dp)
                    )

                    Spacer(modifier = Modifier.size(20.dp))

                    LazyColumn {
                        items(words.toList()) { (id, word) ->
                            WordsListItem(
                                word = word,
                                player = player,
                                tts = tts,
                                locale = locale,
                                onDeleteClick = { onWordRemoved(id) },
                                onDifficultClick = {
                                    onWordUpdated(id, word.copy(difficult = it))
                                },
                                onSkipClick = { onWordUpdated(id, word.copy(skip = it)) },
                                onClick = { onWordClick(id) }
                            )
                        }
                    }
                }
            }
        },
        content = content
    )
}

@Composable
fun SessionOptions(
    enabled: Boolean,
    difficult: Boolean,
    onLearnedClick: () -> Unit,
    onDifficultClick: (Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { expanded = true },
            enabled = enabled
        ) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_more_24),
                contentDescription = "Options"
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = {
                    Text(text = "I already know this")
                },
                onClick = {
                    onLearnedClick()
                    expanded = false
                }
            )

            DropdownMenuItem(
                text = {
                    Text(text = "Difficult")
                },
                onClick = {
                    onDifficultClick(!difficult)
                    expanded = false
                },
                trailingIcon = {
                    Checkbox(
                        checked = difficult,
                        onCheckedChange = { checked ->
                            onDifficultClick(checked)
                            expanded = false
                        }
                    )
                }
            )
        }
    }
}