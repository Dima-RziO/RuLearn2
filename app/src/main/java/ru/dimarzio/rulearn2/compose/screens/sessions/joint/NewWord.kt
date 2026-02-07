package ru.dimarzio.rulearn2.compose.screens.sessions.joint

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.dimarzio.rulearn2.R
import ru.dimarzio.rulearn2.compose.AboutDialog
import ru.dimarzio.rulearn2.compose.AppBarActions
import ru.dimarzio.rulearn2.compose.AutoSizeText
import ru.dimarzio.rulearn2.compose.NavigationIcon
import ru.dimarzio.rulearn2.compose.SelectAudioDialog
import ru.dimarzio.rulearn2.compose.SessionOptions
import ru.dimarzio.rulearn2.compose.WordIndicator
import ru.dimarzio.rulearn2.models.Word
import ru.dimarzio.rulearn2.utils.say
import ru.dimarzio.rulearn2.utils.toast
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewWord(
    title: String,
    word: Word,
    tts: TextToSpeech,
    locale: Locale,
    onNavigationIconClick: () -> Unit,
    onRefreshActionClick: () -> Unit,
    onAudioClick: (File) -> Unit,
    onSettingsActionClick: () -> Unit,
    progress: Float,
    onLearnedClick: () -> Unit,
    onDifficultClick: (Boolean) -> Unit,
    ended: Boolean,
    hidden: Boolean,
    onContinueClick: () -> Unit,
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
                        word = word,
                        tts = tts,
                        locale = locale,
                        onRefreshActionClick = onRefreshActionClick,
                        onAudioClick = onAudioClick,
                        onSettingsActionClick = onSettingsActionClick
                    )
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = {
                    Text(text = "Continue")
                },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Continue"
                    )
                },
                onClick = {
                    if (!ended) {
                        onContinueClick()
                    }
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

            Row(modifier = Modifier.padding(10.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    AutoSizeText(
                        text = if (!hidden) word.name else "...",
                        maxFontSize = 24.sp,
                        minFontSize = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    AutoSizeText(
                        text = if (!hidden) word.translation else "...",
                        maxFontSize = 16.sp,
                        minFontSize = 12.sp,
                        maxLines = 12,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(50.dp))

                Column {
                    WordIndicator(
                        word = word,
                        maxRating = maxRating
                    )

                    SessionOptions(
                        enabled = !ended,
                        difficult = word.difficult,
                        onLearnedClick = onLearnedClick,
                        onDifficultClick = onDifficultClick
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBarActions(
    word: Word,
    tts: TextToSpeech,
    locale: Locale,
    onRefreshActionClick: () -> Unit,
    onAudioClick: (File) -> Unit,
    onSettingsActionClick: () -> Unit,
) {
    var showAudioDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    if (showAudioDialog) {
        val context = LocalContext.current

        SelectAudioDialog(
            onDismissRequest = { showAudioDialog = false },
            onAudioSelected = { audio ->
                if (audio.canRead()) {
                    onAudioClick(audio)
                } else {
                    context.toast("Audio file does not exist!")
                }
            },
            onTextToSpeechSelected = {
                tts.say(word.name, locale) { success ->
                    if (!success) {
                        context.toast("Error.")
                    }
                }
            },
            audios = word.audios?.filter(File::isFile) ?: emptyList(),
            locale = locale
        )
    }

    if (showAboutDialog) {
        AboutDialog {
            showAboutDialog = false
        }
    }

    val audioIcon = ImageVector.vectorResource(id = R.drawable.baseline_music_note_24)
    AppBarActions(
        Triple(Icons.Filled.Refresh, "Refresh", onRefreshActionClick),
        Triple(audioIcon, "Play audio") { showAudioDialog = true },
        Triple(null, "Settings", onSettingsActionClick),
        Triple(null, "About") { showAboutDialog = true }
    )
}