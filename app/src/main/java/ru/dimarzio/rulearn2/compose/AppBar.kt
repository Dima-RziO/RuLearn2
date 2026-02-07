package ru.dimarzio.rulearn2.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
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

@Composable
fun AppBarActions(vararg actions: Triple<ImageVector?, String, () -> Unit>) {
    actions.filterNot { it.first == null }.forEach { (icon, title, onClick) ->
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon!!,
                contentDescription = title
            )
        }
    }

    val nonIcon = actions.filter { it.first == null }
    if (nonIcon.isNotEmpty()) {
        var menuExpanded by remember { mutableStateOf(false) }

        Box {
            IconButton(onClick = { menuExpanded = !menuExpanded }) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Menu"
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                nonIcon.forEach { (_, title, onClick) ->
                    DropdownMenuItem(
                        text = {
                            Text(text = title)
                        },
                        onClick = {
                            onClick()
                            menuExpanded = false
                        }
                    )
                }
            }
        }
    }
}