package ru.dimarzio.rulearn2.compose

import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.dimarzio.rulearn2.R
import ru.dimarzio.rulearn2.models.Word
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SessionContainer(
    onBottomSheetCollapsed: () -> Boolean, // Return value is whether backstack popped or not.
    label: String,
    words: Map<Int, Word>,
    player: MediaPlayer,
    tts: TextToSpeech,
    locale: Locale,
    onWordRemoved: (Int, Word) -> Unit,
    onWordUpdated: (Int, Word, Word) -> Unit,
    onWordClick: (Int) -> Unit,
    content: @Composable (PaddingValues, expand: () -> Unit) -> Unit
) {
    val scope = rememberCoroutineScope()

    var isQuitTriggered by remember { mutableStateOf(false) }
    val sheetState = rememberStandardBottomSheetState(
        confirmValueChange = { newValue ->
            if (newValue == SheetValue.PartiallyExpanded && !isQuitTriggered) {
                isQuitTriggered = onBottomSheetCollapsed()
            }
            true
        }
    )
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)

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
                                onDeleteClick = { onWordRemoved(id, word) },
                                onDifficultClick = {
                                    onWordUpdated(id, word, word.copy(difficult = it))
                                },
                                onSkipClick = { onWordUpdated(id, word, word.copy(skip = it)) },
                                onClick = {
                                    onBottomSheetCollapsed()
                                    scope.launch {
                                        sheetState.partialExpand()
                                    }

                                    onWordClick(id)
                                }
                            )
                        }
                    }
                }
            }
        },
        content = { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                content(innerPadding) {
                    scope.launch {
                        sheetState.expand()
                    }
                }

                val isExpanded = sheetState.currentValue == SheetValue.Expanded

                val scrimColor by animateColorAsState(
                    targetValue = if (isExpanded) {
                        Color.Black.copy(alpha = 0.5f)
                    } else {
                        Color.Transparent
                    },
                    animationSpec = tween(durationMillis = 300),
                    label = "ScrimColorAnimation"
                )

                if (isExpanded) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(scrimColor)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                scope.launch {
                                    isQuitTriggered = onBottomSheetCollapsed()
                                    sheetState.partialExpand()
                                }
                            }
                    )
                }
            }
        }
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