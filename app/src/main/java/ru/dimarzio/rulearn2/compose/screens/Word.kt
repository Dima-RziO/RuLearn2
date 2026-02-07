package ru.dimarzio.rulearn2.compose.screens

import android.speech.tts.TextToSpeech
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.dimarzio.rulearn2.R
import ru.dimarzio.rulearn2.compose.AboutDialog
import ru.dimarzio.rulearn2.compose.AppBarActions
import ru.dimarzio.rulearn2.compose.NavigationIcon
import ru.dimarzio.rulearn2.compose.NumberPicker
import ru.dimarzio.rulearn2.compose.SelectAudioDialog
import ru.dimarzio.rulearn2.models.Word
import ru.dimarzio.rulearn2.utils.format
import ru.dimarzio.rulearn2.utils.say
import ru.dimarzio.rulearn2.utils.toast
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.time.Duration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Word(
    id: Int,
    word: Word,
    modified: Boolean,
    tts: TextToSpeech,
    locale: Locale,
    onNavigationIconClick: () -> Unit,
    onDeleteActionClick: () -> Unit,
    onSettingsActionClick: () -> Unit,
    onAudioClick: (File) -> Unit,
    onAccessedClick: (dateMillis: Long, minutes: Int, hour: Int) -> Unit,
    onWordUpdated: (Word) -> Unit,
    levelsForName: Set<String>,
    levelsForTranslation: Set<String>,
    otherLevels: Set<String>,
    onWordSaved: () -> Unit
) {
    var showSaveChangesDialog by remember { mutableStateOf(false) }

    if (showSaveChangesDialog) {
        SaveChangesDialog(
            onDismissRequest = {
                showSaveChangesDialog = false
                onNavigationIconClick()
            },
            onConfirmation = onWordSaved
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (modified) "*$id" else id.toString(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    NavigationIcon(
                        onClick = if (modified) {
                            fun() { showSaveChangesDialog = true }
                        } else {
                            onNavigationIconClick
                        }
                    )
                },
                actions = {
                    AppBarActions(
                        word = word,
                        tts = tts,
                        locale = locale,
                        onAudioClick = onAudioClick,
                        onAccessedClick = onAccessedClick,
                        onDeleteActionClick = onDeleteActionClick,
                        onSettingsActionClick = onSettingsActionClick
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onWordSaved) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_save_24),
                    contentDescription = "Save"
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            Column(
                modifier = Modifier
                    .padding(10.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                InputFields(
                    name = word.name,
                    onNameChanged = { onWordUpdated(word.copy(name = it)) },
                    levelsForName = levelsForName,
                    translation = word.translation,
                    onTranslationChanged = { onWordUpdated(word.copy(translation = it)) },
                    levelsForTranslation = levelsForTranslation
                )

                Spacer(Modifier.size(10.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max)
                ) {
                    Checkboxes(
                        repeat = word.isRepeat,
                        difficult = word.difficult,
                        skip = word.skip,
                        learned = word.learned,
                        onDifficultChange = { onWordUpdated(word.copy(difficult = it)) },
                        onSkipChange = { onWordUpdated(word.copy(skip = it)) }
                    )

                    RepeatInterval(
                        modifier = Modifier.fillMaxHeight(),
                        correctAnswers = word.correctAnswers,
                        onCorrectAnswersChange = { onWordUpdated(word.copy(correctAnswers = it)) },
                        repetitions = word.repetitions,
                        onRepetitionsChange = { onWordUpdated(word.copy(repetitions = it)) },
                        lastlyRepeated = word.lastlyRepeated,
                        repeatDuration = word.repeatDuration
                    )
                }

                Spacer(modifier = Modifier.size(10.dp))

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    LevelField(
                        level = word.level,
                        levels = otherLevels,
                        onLevelChange = { level -> onWordUpdated(word.copy(level = level)) }
                    )
                }

                Spacer(modifier = Modifier.size(10.dp))

                RatingSlider(rating = word.rating) { rating ->
                    onWordUpdated(word.copy(rating = rating))
                }

                BackHandler(enabled = modified) {
                    showSaveChangesDialog = true
                }
            }
        }
    }
}

@Composable
private fun SaveChangesDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                    onConfirmation()
                }
            ) {
                Text(text = "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = "Cancel")
            }
        },
        title = {
            Text("Save changes?")
        },
        text = {
            Text("This cannot be undone.")
        }
    )
}

@Composable
private fun ConfirmDeleteDialog(
    name: String,
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmation()
                    onDismissRequest()
                }
            ) {
                Text(text = "Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = "Cancel")
            }
        },
        title = {
            Text(text = "Delete $name?")
        },
        text = {
            Text(text = "This cannot be undone.")
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppBarActions(
    word: Word,
    tts: TextToSpeech,
    locale: Locale,
    onAudioClick: (File) -> Unit,
    onAccessedClick: (dateMillis: Long, minutes: Int, hour: Int) -> Unit,
    onDeleteActionClick: () -> Unit,
    onSettingsActionClick: () -> Unit
) {
    var showAudioDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }

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

    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            name = word.name,
            onDismissRequest = { showDeleteDialog = false },
            onConfirmation = onDeleteActionClick
        )
    }

    if (showAboutDialog) {
        AboutDialog {
            showAboutDialog = false
        }
    }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = word.accessed)
    val timePickerState = rememberTimePickerState(
        initialMinute = word.accessedMinutes,
        initialHour = word.accessedHours
    )

    if (showDatePickerDialog) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePickerDialog = false
                        showTimePickerDialog = true
                    },
                    enabled = datePickerState.selectedDateMillis != null
                ) {
                    Text(text = "Pick")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text(text = "Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePickerDialog) {
        AlertDialog(
            onDismissRequest = { showTimePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showTimePickerDialog = false

                        datePickerState.selectedDateMillis?.let { dateMillis ->
                            onAccessedClick(
                                dateMillis,
                                timePickerState.minute,
                                timePickerState.hour
                            )
                        }
                    }
                ) {
                    Text(text = "Pick")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePickerDialog = false }) {
                    Text(text = "Cancel")
                }
            },
            title = {
                Text(text = "Select time")
            },
            text = {
                TimeInput(state = timePickerState)
            }
        )
    }

    val audioIcon = ImageVector.vectorResource(id = R.drawable.baseline_music_note_24)
    val accessedIcon = ImageVector.vectorResource(id = R.drawable.baseline_access_time_24)

    AppBarActions(
        Triple(audioIcon, "Play audio") { showAudioDialog = true },
        Triple(accessedIcon, "Pick accessed") { showDatePickerDialog = true },
        Triple(Icons.Filled.Delete, "Delete") { showDeleteDialog = true },
        Triple(null, "Settings", onSettingsActionClick),
        Triple(null, "About") { showAboutDialog = true }
    )
}

@Composable
private fun InputFields(
    name: String,
    onNameChanged: (String) -> Unit,
    levelsForName: Set<String>,
    translation: String,
    onTranslationChanged: (String) -> Unit,
    levelsForTranslation: Set<String>
) {
    Column(modifier = Modifier.width(IntrinsicSize.Max)) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChanged,
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(text = "Name")
            },
            supportingText = {
                if (levelsForName.isNotEmpty()) {
                    val levels = levelsForName.joinToString()
                    Text(text = "This name already exists in $levels")
                }
            }
        )

        OutlinedTextField(
            value = translation,
            onValueChange = onTranslationChanged,
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(text = "Translation")
            },
            supportingText = {
                if (levelsForTranslation.isNotEmpty()) {
                    val levels = levelsForTranslation.joinToString()
                    Text(text = "This translation already exists in $levels")
                }
            }
        )
    }
}

@Composable
private fun Checkboxes(
    repeat: Boolean,
    difficult: Boolean,
    skip: Boolean,
    learned: Boolean,
    onDifficultChange: (Boolean) -> Unit,
    onSkipChange: (Boolean) -> Unit
) {
    Column(modifier = Modifier.width(IntrinsicSize.Max)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Repeat")

            Checkbox(
                checked = repeat,
                onCheckedChange = {},
                enabled = false
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Difficult")

            Checkbox(
                checked = difficult,
                onCheckedChange = onDifficultChange,
                enabled = learned
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Skip")

            Checkbox(
                checked = skip,
                onCheckedChange = onSkipChange
            )
        }
    }
}

@Composable
private fun RatioFields(
    correctAnswers: Int,
    onCorrectAnswersChange: (Int) -> Unit,
    repetitions: Int,
    onRepetitionsChange: (Int) -> Unit
) {
    Row(modifier = Modifier.height(IntrinsicSize.Max)) {
        NumberPicker(
            value = correctAnswers,
            onValueChange = onCorrectAnswersChange,
            range = 0..Int.MAX_VALUE,
            label = "cor"
        )

        VerticalDivider(
            modifier = Modifier
                .fillMaxHeight()
                .padding(5.dp)
        )
        NumberPicker(
            value = repetitions,
            onValueChange = onRepetitionsChange,
            range = 0..Int.MAX_VALUE,
            label = "rep"
        )
    }
}

@Composable
private fun LastlyRepeated(lastlyRepeated: Duration) {
    if (lastlyRepeated.isFinite()) {
        Text(text = "Lastly repeated " + lastlyRepeated.format() + " ago")
    } else {
        Text(text = "Never repeated before")
    }
}

@Composable
private fun NextRepetition(repeatDuration: Duration) {
    when (repeatDuration) {
        Duration.INFINITE -> Text(text = "Next repetition never")
        Duration.ZERO -> Text(text = "Next repetition now")
        else -> Text(text = "Next repetition in " + repeatDuration.format())
    }
}

@Composable
private fun RepeatInterval(
    modifier: Modifier = Modifier,
    correctAnswers: Int,
    onCorrectAnswersChange: (Int) -> Unit,
    repetitions: Int,
    onRepetitionsChange: (Int) -> Unit,
    lastlyRepeated: Duration,
    repeatDuration: Duration
) {
    Column(
        verticalArrangement = Arrangement.SpaceEvenly,
        modifier = modifier
    ) {
        RatioFields(
            correctAnswers = correctAnswers,
            onCorrectAnswersChange = onCorrectAnswersChange,
            repetitions = repetitions,
            onRepetitionsChange = onRepetitionsChange
        )

        LastlyRepeated(lastlyRepeated = lastlyRepeated)

        NextRepetition(repeatDuration = repeatDuration)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LevelField(
    level: String,
    levels: Set<String>,
    onLevelChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        TextField(
            value = level,
            onValueChange = onLevelChange,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded,
                    Modifier.menuAnchor(MenuAnchorType.PrimaryEditable)
                )
            },
            singleLine = true,
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
            label = {
                Text(text = "Level")
            }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            levels.forEach { level ->
                DropdownMenuItem(
                    text = {
                        Text(text = level)
                    },
                    onClick = {
                        onLevelChange(level)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@Composable
private fun RatingSlider(
    rating: Int,
    onRatingChange: (Int) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Slider(
            value = rating.toFloat(),
            onValueChange = { rating -> onRatingChange(rating.roundToInt()) },
            modifier = Modifier.weight(1f),
            valueRange = 0f..Word.MAX_RATING.toFloat()
        )

        Spacer(modifier = Modifier.size(10.dp))

        Text(
            text = "Rating: $rating",
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    RoundedCornerShape(4.dp)
                )
                .padding(5.dp)
        )
    }
}