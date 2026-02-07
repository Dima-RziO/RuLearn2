package ru.dimarzio.rulearn2.compose.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.dimarzio.rulearn2.compose.DropdownPreference
import ru.dimarzio.rulearn2.compose.NavigationIcon
import ru.dimarzio.rulearn2.compose.PreferenceCategory
import ru.dimarzio.rulearn2.compose.SwitchPreference
import ru.dimarzio.rulearn2.compose.TextFieldPreference
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Settings(
    onNavigationIconClick: () -> Unit,
    markDifficult: Boolean,
    onMarkDifficultChange: (Boolean) -> Unit,
    papasHints: Boolean,
    onPapasHintsChange: (Boolean) -> Unit,
    similarWords: Boolean,
    onSimilarWordsChange: (Boolean) -> Unit,
    skippedWords: Boolean,
    onSkippedWordsChange: (Boolean) -> Unit,
    tts: Boolean,
    onTtsChange: (Boolean) -> Unit,
    backGesture: Boolean,
    onBackGestureChange: (Boolean) -> Unit,
    notify: Boolean,
    onNotifyChange: (Boolean) -> Unit,
    notifyPer: Duration,
    onNotifyPerChange: (Duration) -> Unit,
    customFolder: Boolean,
    onCustomFolderChange: (Boolean) -> Unit,
    customFolderPath: String?,
    onCustomFolderPathChange: (String) -> Unit,
    dynamicColorsEnabled: Boolean,
    dynamicColors: Boolean,
    onDynamicColorsChange: (Boolean) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Settings")
                },
                navigationIcon = {
                    NavigationIcon(onClick = onNavigationIconClick)
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            PreferenceCategory(title = "Guessing & Typing sessions settings") {
                SwitchPreference(
                    title = "Move word to difficult",
                    summary = "Mark a word as difficult if sum_correct / n_repeat <= 0.75",
                    checked = markDifficult,
                    onCheckedChange = onMarkDifficultChange
                )

                SwitchPreference(
                    title = "Use papa's hints algorithm",
                    summary = "Use another hints algorithm in typing tests",
                    checked = papasHints,
                    onCheckedChange = onPapasHintsChange
                )

                SwitchPreference(
                    title = "Choose similar words",
                    summary = "Suggest similar words as translations in guessing tests",
                    checked = similarWords,
                    onCheckedChange = onSimilarWordsChange
                )

                SwitchPreference(
                    title = "Choose skipped words",
                    checked = skippedWords,
                    summary = "Suggest skipped words as translations in guessing tests",
                    onCheckedChange = onSkippedWordsChange
                )

                SwitchPreference(
                    title = "Use TTS",
                    summary = "Play TTS if no audio file was found",
                    checked = tts,
                    onCheckedChange = onTtsChange
                )

                SwitchPreference(
                    title = "Override back gesture",
                    summary = "Refresh current word on back gesture",
                    checked = backGesture,
                    onCheckedChange = onBackGestureChange
                )
            }

            PreferenceCategory(
                title = "Notifications settings",
                dividerAbove = true,
                dividerBelow = true
            ) {
                SwitchPreference(
                    title = "Allow notifications",
                    summary = "Notify if there are words to repeat",
                    checked = notify,
                    onCheckedChange = onNotifyChange
                )

                DropdownPreference(
                    title = "Send notifications every",
                    selected = notifyPer,
                    enabled = notify,
                    items = setOf(1.hours, 5.hours, 1.days),
                    onItemClick = onNotifyPerChange
                )
            }

            PreferenceCategory(title = "App's folder settings") {
                SwitchPreference(
                    title = "Use a custom folder",
                    summary = "Store app files in a custom folder",
                    checked = customFolder,
                    onCheckedChange = onCustomFolderChange
                )

                TextFieldPreference(
                    title = "Custom folder path",
                    input = customFolderPath,
                    confirmButton = "Save",
                    onConfirmation = onCustomFolderPathChange,
                    label = "Path",
                    confirmButtonEnabled = { input -> File(input).canWrite() },
                    enabled = customFolder
                )
            }

            PreferenceCategory(title = "Other settings") {
                SwitchPreference(
                    title = "Use material 3 dynamic colors",
                    summary = if (!dynamicColorsEnabled) {
                        "This setting is only available on API >= 31"
                    } else {
                        null
                    },
                    checked = dynamicColors,
                    onCheckedChange = onDynamicColorsChange,
                    enabled = dynamicColorsEnabled
                )
            }
        }
    }
}