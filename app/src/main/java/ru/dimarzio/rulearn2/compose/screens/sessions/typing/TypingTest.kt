package ru.dimarzio.rulearn2.compose.screens.sessions.typing

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.dimarzio.rulearn2.R
import ru.dimarzio.rulearn2.compose.AboutDialog
import ru.dimarzio.rulearn2.compose.AppBarActions
import ru.dimarzio.rulearn2.compose.AutoSizeText
import ru.dimarzio.rulearn2.compose.NavigationIcon
import ru.dimarzio.rulearn2.compose.SessionOptions
import ru.dimarzio.rulearn2.compose.WordIndicator
import ru.dimarzio.rulearn2.models.Word

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypingTest(
    title: String,
    onNavigationIconClick: () -> Unit,
    onRefreshActionClick: () -> Unit,
    onSettingsActionClick: () -> Unit,
    progress: Float,
    word: Word,
    onLearnedClick: () -> Unit,
    onDifficultClick: (Boolean) -> Unit,
    inputValue: TextFieldValue,
    onInputValueChange: (TextFieldValue) -> Unit,
    inputEnabled: Boolean,
    onHintClick: () -> Unit,
    error: Boolean,
    helperText: Boolean,
    onDoneClick: () -> Unit,
    maxRating: Int = Word.MAX_RATING,
    hidden: Boolean = false
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
                        onRefreshActionClick = onRefreshActionClick,
                        onSettingsActionClick = onSettingsActionClick
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

            Row(modifier = Modifier.padding(10.dp)) {
                AutoSizeText(
                    text = if (!hidden) word.translation else "...",
                    maxFontSize = 24.sp,
                    minFontSize = 16.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 12,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.width(50.dp))

                Column {
                    WordIndicator(
                        word = word,
                        maxRating = maxRating
                    )

                    SessionOptions(
                        enabled = inputEnabled,
                        difficult = word.difficult,
                        onLearnedClick = onLearnedClick,
                        onDifficultClick = onDifficultClick
                    )
                }
            }

            InputField(
                value = inputValue,
                onValueChange = onInputValueChange,
                readOnly = !inputEnabled,
                onHintClick = onHintClick,
                errorMessage = word.name.takeIf { error },
                helperText = word.name.takeIf { helperText },
                onDoneClick = onDoneClick
            )
        }
    }
}

@Composable
private fun TopBarActions(
    onRefreshActionClick: () -> Unit,
    onSettingsActionClick: () -> Unit
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
private fun InputField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    readOnly: Boolean,
    onHintClick: () -> Unit,
    errorMessage: String?,
    helperText: String?,
    onDoneClick: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            if (!readOnly) {
                onValueChange(input)
            }
        },
        modifier = Modifier
            .padding(10.dp)
            .fillMaxWidth(),
        readOnly = readOnly,
        label = {
            Text(text = "Enter translation")
        },
        trailingIcon = {
            if (helperText == null) {
                IconButton(
                    onClick = onHintClick,
                    enabled = !readOnly
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_hint_24),
                        contentDescription = "Take hint"
                    )
                }
            }
        },
        isError = errorMessage != null,
        supportingText = {
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error
                )
            } else if (helperText != null) {
                Text(text = helperText)
            }
        },
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Done,
            keyboardType = KeyboardType.Email,
            autoCorrectEnabled = false
        ),
        keyboardActions = KeyboardActions { onDoneClick() },
        textStyle = TextStyle(fontSize = 16.sp)
    )
}