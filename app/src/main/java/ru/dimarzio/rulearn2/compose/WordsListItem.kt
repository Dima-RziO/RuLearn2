package ru.dimarzio.rulearn2.compose

import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.dimarzio.rulearn2.R
import ru.dimarzio.rulearn2.models.Word
import ru.dimarzio.rulearn2.utils.percentageFrom
import ru.dimarzio.rulearn2.utils.play
import ru.dimarzio.rulearn2.utils.say
import java.util.Locale

@Composable
fun CircularIndicator(
    modifier: Modifier = Modifier,
    iconResource: Int
) {
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer

    Icon(
        painter = painterResource(id = iconResource),
        contentDescription = "Indicator",
        modifier = Modifier
            .size(20.dp)
            .then(modifier)
            .drawBehind {
                drawCircle(primaryContainer)
            }
            .padding(3.dp)
    )
}

@Composable
fun WordIndicator(word: Word, maxRating: Int = Word.MAX_RATING) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(
            progress = { (word.rating percentageFrom maxRating) / 100 },
            modifier = Modifier.size(48.dp),
            strokeWidth = 4.dp
        )

        Spacer(modifier = Modifier.size(3.dp))

        Column {
            CircularIndicator(
                modifier = Modifier.alpha(if (word.isRepeat) 1f else 0f),
                iconResource = R.drawable.baseline_repeat_24
            )

            Spacer(modifier = Modifier.size(3.dp))

            CircularIndicator(
                modifier = Modifier.alpha(if (word.difficult) 1f else 0f),
                iconResource = R.drawable.baseline_difficult_24
            )
        }
    }
}

@Composable
fun WordsListItemContainer(
    onDeleteClick: () -> Unit,
    onAudioClick: () -> Unit,
    content: @Composable () -> Unit
) {
    SwipeToRevealBox(
        hiddenContentEnd = {
            Row {
                Text(text = "Delete")

                Spacer(modifier = Modifier.size(5.dp))

                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete"
                )
            }
        },
        hiddenContentEndBackground = MaterialTheme.colorScheme.errorContainer,
        onHiddenContentEndClick = onDeleteClick,
        hiddenContentStart = {
            Icon(
                painter = painterResource(id = R.drawable.baseline_sound_24),
                contentDescription = "Play audio"
            )
        },
        onHiddenContentStartClick = onAudioClick
    ) {
        content()
    }
}

@Composable
fun WordsListItem(
    word: Word,
    player: MediaPlayer,
    tts: TextToSpeech,
    locale: Locale,
    onDeleteClick: () -> Unit,
    onDifficultClick: (Boolean) -> Unit,
    onSkipClick: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    WordsListItemContainer(
        onDeleteClick = onDeleteClick,
        onAudioClick = {
            word.randomAudio?.let { audio ->
                player.play(audio)
            } ?: run {
                tts.say(word.name, locale)
            }
        }
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = word.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.alpha(if (word.skip) 0.3f else 1f)
                )
            },
            supportingContent = {
                Text(
                    text = word.translation,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.alpha(if (word.skip) 0.3f else 1f)
                )
            },
            leadingContent = {
                WordIndicator(word = word)
            },
            trailingContent = {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More"
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(text = "Difficult")
                        },
                        onClick = {
                            onDifficultClick(!word.difficult)
                            menuExpanded = false
                        },
                        trailingIcon = {
                            Checkbox(
                                checked = word.difficult,
                                onCheckedChange = onDifficultClick,
                                enabled = word.learned
                            )
                        },
                        enabled = word.learned
                    )

                    DropdownMenuItem(
                        text = {
                            Text(text = "Skip")
                        },
                        onClick = {
                            onSkipClick(!word.skip)
                            menuExpanded = false
                        },
                        trailingIcon = {
                            Checkbox(
                                checked = word.skip,
                                onCheckedChange = onSkipClick
                            )
                        }
                    )
                }
            },
            modifier = Modifier.clickable(onClick = onClick)
        )
    }
}