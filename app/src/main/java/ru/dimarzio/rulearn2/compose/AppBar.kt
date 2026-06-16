package ru.dimarzio.rulearn2.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun NavigationIcon(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Navigate up"
        )
    }
}

class AppBarActionsScope {
    @Composable
    fun Action(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
        IconButton(onClick = onClick) {
            Icon(imageVector = icon, contentDescription = contentDescription)
        }
    }
}

class OverflowMenuScope {
    @Composable
    private fun LabeledCheckbox(
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        label: String
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = { onCheckedChange(!checked) })
        ) {
            Text(text = label)

            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }

    @Composable
    fun OverflowAction(text: String, onClick: () -> Unit) {
        DropdownMenuItem(
            text = { Text(text = text) },
            onClick = onClick
        )
    }

    @Composable
    fun CheckboxAction(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
        DropdownMenuItem(
            text = {
                LabeledCheckbox(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    label = label
                )
            },
            onClick = { onCheckedChange(!checked) }
        )
    }

    @Composable
    fun SettingsAction(onClick: () -> Unit) {
        DropdownMenuItem(
            text = { Text(text = "Settings") },
            onClick = onClick
        )
    }

    @Composable
    fun AboutAction() {
        var show by remember { mutableStateOf(false) }

        if (show) {
            AboutDialog { show = false }
        }

        DropdownMenuItem(
            text = { Text(text = "About") },
            onClick = {
                show = true
            }
        )
    }
}

@Composable
fun AppBarActions(
    actions: @Composable AppBarActionsScope.() -> Unit,
    overflowMenu: (@Composable OverflowMenuScope.() -> Unit)? = null
) {
    remember { AppBarActionsScope() }.actions()

    if (overflowMenu != null) {
        var menuExpanded by remember { mutableStateOf(false) }

        Box {
            IconButton(onClick = { menuExpanded = !menuExpanded }) {
                Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "Ещё")
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                remember { OverflowMenuScope() }.overflowMenu()
            }
        }
    }
}