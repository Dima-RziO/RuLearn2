package ru.dimarzio.rulearn2.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PreferenceCategory(
    title: String,
    dividerAbove: Boolean = false,
    dividerBelow: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(10.dp)
    ) {
        if (dividerAbove) {
            HorizontalDivider()
        }

        Text(
            text = title,
            color = MaterialTheme.colorScheme.primary
        )

        content()

        if (dividerBelow) {
            HorizontalDivider()
        }
    }
}

@Composable
fun Preference(
    title: String,
    summary: String? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .run {
                if (onClick != null) {
                    clickable(onClick = onClick)
                } else {
                    this
                }
            }
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )

            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.size(10.dp))

        content()
    }
}

@Composable
fun SwitchPreference(
    title: String,
    summary: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Preference(
        title = title,
        summary = summary
    ) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
fun <T> DropdownPreference(
    title: String,
    selected: T,
    enabled: Boolean = true,
    items: Set<T>,
    getItemTitle: (T) -> String = { item -> item.toString() },
    onItemClick: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Preference(
            title = title,
            summary = getItemTitle(selected),
            onClick = if (enabled) {
                fun() { expanded = true }
            } else {
                null
            }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(text = getItemTitle(item))
                    },
                    onClick = {
                        onItemClick(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun TextFieldPreference(
    title: String,
    input: String? = null,
    confirmButton: String,
    onConfirmation: (String) -> Unit,
    label: String,
    confirmButtonEnabled: (String) -> Boolean = { true },
    enabled: Boolean = true
) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        TextFieldDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = confirmButton,
            onConfirmation = onConfirmation,
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(text = "Cancel")
                }
            },
            title = title,
            initialInput = input.orEmpty(),
            label = label,
            confirmButtonEnabled = confirmButtonEnabled
        )
    }

    Preference(
        title = title,
        summary = input,
        onClick = if (enabled) {
            fun() { showDialog = true }
        } else {
            null
        }
    )
}