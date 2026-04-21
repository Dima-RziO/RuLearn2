package ru.dimarzio.rulearn2.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.dimarzio.rulearn2.BuildConfig
import ru.dimarzio.rulearn2.R
import ru.dimarzio.rulearn2.utils.toast
import java.io.File
import java.util.Locale

@Composable
private fun LabeledCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { onCheckedChange(!checked) })
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )

        Text(text = label)
    }
}

@Composable
fun <T> MultiChoiceDialog(
    onDismissRequest: () -> Unit,
    confirmButton: String,
    onConfirmation: (Set<T>) -> Unit,
    dismissButton: @Composable () -> Unit,
    title: String,
    items: Map<T, Boolean>,
    getLabel: (T) -> String = { item -> item.toString() }
) {
    val checkedList = remember {
        items.filterValues { checked -> checked }.keys.toMutableStateList()
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                    onConfirmation(checkedList.toSet())
                }
            ) {
                Text(text = confirmButton)
            }
        },
        dismissButton = dismissButton,
        title = {
            Text(text = title)
        },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                items.forEach { (item, _) ->
                    LabeledCheckbox(
                        checked = item in checkedList,
                        onCheckedChange = { checked ->
                            if (checked) {
                                checkedList += item
                            } else {
                                checkedList -= item
                            }
                        },
                        label = getLabel(item)
                    )
                }
            }
        }
    )
}

@Composable
private fun ExpandButton(
    expanded: Boolean,
    onClick: (Boolean) -> Unit
) {
    if (expanded) {
        IconButton(onClick = { onClick(false) }) {
            Icon(
                painter = painterResource(R.drawable.baseline_arrow_up_24),
                contentDescription = "Shrink"
            )
        }
    } else {
        IconButton(onClick = { onClick(true) }) {
            Icon(
                painter = painterResource(R.drawable.baseline_arrow_down_24),
                contentDescription = "Expand"
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun <T> GroupedMultiChoiceDialog(
    onDismissRequest: () -> Unit,
    confirmButton: String,
    onConfirmation: (Set<T>) -> Unit,
    dismissButton: @Composable () -> Unit,
    title: String,
    groupedItems: Map<String, Map<T, Boolean>>, // key is header
    getLabel: (T) -> String = { item -> item.toString() }
) {
    val checkedList = remember {
        groupedItems.values
            .flatMap(Map<T, Boolean>::entries)
            .filter { (_, checked) -> checked }
            .map(Map.Entry<T, Boolean>::key)
            .toMutableStateList()
    }

    var expanded by remember { mutableStateOf(null as String?) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                    onConfirmation(checkedList.toSet())
                }
            ) {
                Text(text = confirmButton)
            }
        },
        dismissButton = dismissButton,
        title = {
            Text(text = title)
        },
        text = {
            Column {
                groupedItems.forEach { (header, items) ->
                    FilledHeader(header = header) {
                        ExpandButton(expanded = header == expanded) {
                            expanded = if (it) header else null
                        }
                    }

                    AnimatedVisibility(visible = header == expanded) {
                        Column {
                            items.forEach { (item, _) ->
                                LabeledCheckbox(
                                    checked = item in checkedList,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            checkedList += item
                                        } else {
                                            checkedList -= item
                                        }
                                    },
                                    label = getLabel(item)
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun LabeledRadioButton(
    selected: Boolean,
    onClick: () -> Unit,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )

        Text(text = label)
    }
}

@Composable
fun <T> SingleChoiceDialog(
    onDismissRequest: () -> Unit,
    confirmButton: String,
    onConfirmation: (T) -> Unit,
    dismissButton: @Composable () -> Unit,
    title: String,
    items: Set<T>,
    selected: T?,
    getLabel: (T) -> String = { item -> item.toString() }
) {
    val context = LocalContext.current
    var selected by remember {
        mutableStateOf(selected)
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    if (selected != null) {
                        onDismissRequest()
                        onConfirmation(selected!!)
                    } else {
                        context.toast("selected = null")
                    }
                },
                enabled = selected != null
            ) {
                Text(text = confirmButton)
            }
        },
        dismissButton = dismissButton,
        title = {
            Text(text = title)
        },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                items.forEach { item ->
                    LabeledRadioButton(
                        selected = item == selected,
                        onClick = { selected = item },
                        label = getLabel(item)
                    )
                }
            }
        }
    )
}

@Composable
fun SelectAudioDialog(
    onDismissRequest: () -> Unit,
    onAudioSelected: (File) -> Unit,
    onTextToSpeechSelected: () -> Unit,
    audios: List<File>,
    locale: Locale
) {
    SingleChoiceDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = "Play",
        onConfirmation = { item ->
            audios.find { it.name == item }?.let(onAudioSelected) ?: onTextToSpeechSelected()
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = "Cancel")
            }
        },
        title = "Play audio",
        items = audios
            .map(File::getName)
            .plus("TTS (${locale.displayName})")
            .toSet(),
        selected = null
    )
}

@Composable
fun TextFieldDialog(
    onDismissRequest: () -> Unit,
    confirmButton: String,
    confirmButtonEnabled: (String) -> Boolean = { input -> input.isNotBlank() },
    onConfirmation: (String) -> Unit,
    dismissButton: @Composable () -> Unit,
    title: String,
    initialInput: String,
    label: String
) {
    var input by remember { mutableStateOf(initialInput) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                    onConfirmation(input)
                },
                enabled = confirmButtonEnabled(input)
            ) {
                Text(text = confirmButton)
            }
        },
        dismissButton = dismissButton,
        title = {
            Text(text = title)
        },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = {
                    Text(text = label)
                }
            )
        }
    )
}

@Composable
fun ProgressDialog(
    title: String,
    progress: Int,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = { },
        title = { Text(text = title) },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { progress.toFloat() / 100 },
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.size(8.dp))

                Text(text = "$progress%")
            }
        }
    )
}

@Composable
fun ProgressDialog(
    title: String,
    message: String,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = { },
        title = { Text(text = title) },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator()

                Spacer(modifier = Modifier.size(16.dp))

                Text(text = message)
            }
        }
    )
}

@Composable
fun ErrorDialog(
    onDismissRequest: () -> Unit,
    message: String
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = "Cancel")
            }
        },
        title = {
            Text(text = "An error occurred")
        },
        text = {
            Text(
                text = message,
                modifier = Modifier
                    .fillMaxHeight(0.4f)
                    .verticalScroll(rememberScrollState()),
            )
        }
    )
}

@Composable
fun AboutDialog(onDismissRequest: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = { },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = "Cancel")
            }
        },
        title = {
            Text(text = "${stringResource(id = R.string.app_name)} ${BuildConfig.VERSION_NAME}")
        },
        text = {
            Text(
                text = "Changelog:\n" +
                        "• General bug fixes and other improvements"
            )
        }
    )
}