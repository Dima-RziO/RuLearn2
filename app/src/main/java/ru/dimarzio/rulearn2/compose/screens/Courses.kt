package ru.dimarzio.rulearn2.compose.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import ru.dimarzio.rulearn2.R
import ru.dimarzio.rulearn2.compose.AboutDialog
import ru.dimarzio.rulearn2.compose.AppBarActions
import ru.dimarzio.rulearn2.compose.MultiChoiceDialog
import ru.dimarzio.rulearn2.compose.ProgressDialog
import ru.dimarzio.rulearn2.compose.SingleChoiceDialog
import ru.dimarzio.rulearn2.compose.SwipeToRevealBox
import ru.dimarzio.rulearn2.compose.TextFieldDialog
import ru.dimarzio.rulearn2.models.Course
import ru.dimarzio.rulearn2.utils.toast
import vladis.luv.wificopy.transport.Host
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Courses(
    onExportCsvClick: (String?) -> Unit,
    onExportDatabaseClick: () -> Unit,
    getLocalHosts: () -> List<Host>,
    replicationLogs: List<String>,
    onReplicateClick: (Set<Host>) -> Unit,
    onSQLiteClick: (String) -> Unit,
    onSettingsActionClick: () -> Unit,
    onImportClick: () -> Unit,
    courses: Map<String, Course>,
    onCourseClick: (String) -> Unit,
    onDeleteCourseClick: (String) -> Unit,
    onRenameClick: (String, String) -> Unit,
    onChangeIconClick: (String) -> Unit,
    importProgress: Float?,
    deleteProgress: Float?,
    exportProgress: Float?,
    isReplicating: Boolean
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Courses")
                },
                actions = {
                    TopBarActions(
                        getLocalHosts = getLocalHosts,
                        replicationLogs = replicationLogs,
                        onExportActionClick = { csvMethod ->
                            if (csvMethod) {
                                onExportCsvClick(null)
                            } else {
                                onExportDatabaseClick()
                            }
                        },
                        onReplicateClick = onReplicateClick,
                        onSQLiteClick = onSQLiteClick,
                        onSettingsActionClick = onSettingsActionClick
                    )
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onImportClick,
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.mdi_import_24),
                        contentDescription = "Import"
                    )
                },
                text = {
                    Text(text = "Import course")
                }
            )
        }
    ) { innerPadding ->
        val context = LocalContext.current

        if (importProgress != null) {
            ProgressDialog(
                title = "Importing course(s)...",
                progress = importProgress.roundToInt(),
                onDismissRequest = {}
            )
        }

        if (deleteProgress != null) {
            ProgressDialog(
                title = "Deleting course...",
                progress = deleteProgress.roundToInt(),
                onDismissRequest = {}
            )
        }

        if (exportProgress != null) {
            ProgressDialog(
                title = "Exporting course(s)...",
                progress = exportProgress.roundToInt(),
                onDismissRequest = {}
            )
        }

        if (isReplicating) {
            ProgressDialog(
                title = "Replicating...",
                message = "Please wait...",
                onDismissRequest = {}
            )
        }

        LaunchedEffect(key1 = isReplicating) {
            if (!isReplicating) {
                context.toast("Please check the replication logs for info.")
            }
        }

        CoursesList(
            modifier = Modifier.padding(innerPadding),
            courses = courses,
            onCourseClick = onCourseClick,
            onExportCourseClick = onExportCsvClick,
            onDeleteCourseClick = onDeleteCourseClick,
            onRenameClick = onRenameClick,
            onChangeIconClick = onChangeIconClick
        )
    }
}

@Composable
private fun TopBarActions(
    getLocalHosts: () -> List<Host>,
    replicationLogs: List<String>,
    onExportActionClick: (Boolean) -> Unit,
    onReplicateClick: (Set<Host>) -> Unit,
    onSQLiteClick: (String) -> Unit,
    onSettingsActionClick: () -> Unit
) {
    val context = LocalContext.current

    var showExportMethodDialog by remember { mutableStateOf(false) }
    var showReplicateDialog by remember { mutableStateOf(false) }
    var showLogsDialog by remember { mutableStateOf(false) }
    var showSQLiteQueryDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    if (showExportMethodDialog) {
        ExportMethodDialog(
            onDismissRequest = { showExportMethodDialog = false },
            onConfirmation = onExportActionClick
        )
    }

    if (showReplicateDialog) {
        ReplicateDialog(
            onDismissRequest = { showReplicateDialog = false },
            onConfirmation = onReplicateClick,
            getLocalHosts = getLocalHosts
        )
    }

    if (showLogsDialog) {
        ReplicationLogsDialog(
            logs = replicationLogs,
            onDismissRequest = { showLogsDialog = false }
        )
    }

    if (showSQLiteQueryDialog) {
        SQLiteQueryDialog(
            onDismissRequest = { showSQLiteQueryDialog = false },
            onConfirmation = onSQLiteClick
        )
    }

    if (showAboutDialog) {
        AboutDialog {
            showAboutDialog = false
        }
    }

    val exportIcon = ImageVector.vectorResource(id = R.drawable.mdi_export_24)
    val transferIcon = ImageVector.vectorResource(id = R.drawable.mdi_transfer_24)
    val queryIcon = ImageVector.vectorResource(id = R.drawable.baseline_terminal_24)

    AppBarActions(
        Triple(exportIcon, "Export all courses") { showExportMethodDialog = true },
        Triple(transferIcon, "Replicate database") {
            if (getLocalHosts().isNotEmpty()) {
                showReplicateDialog = true
            } else {
                context.toast("No hosts available.")
            }
        },
        Triple(null, "Replication logs") { showLogsDialog = true },
        Triple(null, "Execute SQLite query") { showSQLiteQueryDialog = true },
        Triple(null, "Settings", onSettingsActionClick),
        Triple(null, "About") { showAboutDialog = true }
    )
}

@Composable
private fun ConfirmDeleteDialog(
    course: String,
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
            Text(text = "Delete $course?")
        },
        text = {
            Text(text = "This cannot be undone.")
        }
    )
}

@Composable
private fun RenameDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: (String) -> Unit,
    course: String
) {
    TextFieldDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = "Rename",
        confirmButtonEnabled = { input -> input.isNotBlank() && input != course },
        onConfirmation = onConfirmation,
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = "Cancel")
            }
        },
        title = "Rename $course",
        initialInput = course,
        label = "Name"
    )
}

@Composable
private fun ExportMethodDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: (Boolean) -> Unit
) {
    val csvItem = "CSV Method"
    val dbItem = "DB Method"

    SingleChoiceDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = "Choose",
        onConfirmation = { item -> onConfirmation(item == csvItem) },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = "Cancel")
            }
        },
        title = "Choose export method",
        items = setOf(csvItem, dbItem),
        selected = csvItem
    )
}

@Composable
private fun ReplicateDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: (Set<Host>) -> Unit,
    getLocalHosts: () -> List<Host>
) {
    MultiChoiceDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = "Replicate...",
        onConfirmation = onConfirmation, // TODO
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = "Cancel")
            }
        },
        title = "Replicate",
        items = getLocalHosts().associateWith { true },
        getLabel = { host -> host.hostname }
    )
}

@Composable
private fun ReplicationLogsDialog(
    logs: List<String>,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = { },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = "Cancel")
            }
        },
        title = {
            Text(text = "Replication logs")
        },
        text = {
            LazyColumn {
                items(logs) { log ->
                    Text(text = log)
                }
            }
        }
    )
}

@Composable
private fun SQLiteQueryDialog(onDismissRequest: () -> Unit, onConfirmation: (String) -> Unit) {
    TextFieldDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = "Run",
        onConfirmation = onConfirmation,
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = "Cancel")
            }
        },
        title = "Run SQLite query",
        initialInput = "",
        label = "Query"
    )
}

@Composable
private fun CoursesList(
    modifier: Modifier = Modifier,
    courses: Map<String, Course>,
    onCourseClick: (String) -> Unit,
    onExportCourseClick: (String) -> Unit,
    onDeleteCourseClick: (String) -> Unit,
    onRenameClick: (String, String) -> Unit,
    onChangeIconClick: (String) -> Unit
) {
    LazyColumn(modifier = modifier) {
        items(courses.toList()) { (name, course) ->
            CoursesListItem(
                name = name,
                course = course,
                onClick = {
                    onCourseClick(name)
                },
                onExportClick = {
                    onExportCourseClick(name)
                },
                onDeleteClick = {
                    onDeleteCourseClick(name)
                },
                onRenameClick = { to ->
                    onRenameClick(name, to)
                },
                onChangeIconClick = {
                    onChangeIconClick(name)
                }
            )
        }
    }
}

@Composable
private fun CoursesListItemContainer(
    onDeleteClick: () -> Unit,
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
        onHiddenContentEndClick = onDeleteClick
    ) {
        content()
    }
}

@Composable
private fun CoursesListItem(
    name: String,
    course: Course,
    onClick: () -> Unit,
    onExportClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRenameClick: (String) -> Unit,
    onChangeIconClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            course = name,
            onDismissRequest = { showDeleteDialog = false },
            onConfirmation = onDeleteClick
        )
    }

    if (showRenameDialog) {
        RenameDialog(
            onDismissRequest = { showRenameDialog = false },
            onConfirmation = onRenameClick,
            course = name
        )
    }

    CoursesListItemContainer(onDeleteClick = { showDeleteDialog = true }) {
        ListItem(
            headlineContent = {
                Text(
                    text = name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            },
            modifier = Modifier
                .height(IntrinsicSize.Max)
                .clickable(onClick = onClick),
            leadingContent = {
                Box(
                    modifier = Modifier.fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    if (course.icon?.canRead() == true) {
                        Image(
                            painter = rememberAsyncImagePainter(model = course.icon),
                            contentDescription = "Icon",
                            modifier = Modifier.size(64.dp)
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_image_not_supported_24),
                            contentDescription = "Default icon",
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
            },
            supportingContent = {
                Column {
                    Text(
                        text = "${course.repeat} to repeat",
                        fontSize = 12.sp
                    )

                    Text(
                        text = "${course.learned}/${course.total} learned",
                        fontSize = 12.sp
                    )
                }
            },
            trailingContent = {
                Box(
                    modifier = Modifier.fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "More"
                        )
                    }
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(text = "Export")
                        },
                        onClick = {
                            menuExpanded = false
                            onExportClick()
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Text(text = "Rename")
                        },
                        onClick = {
                            menuExpanded = false
                            showRenameDialog = true
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Text(text = "Change icon")
                        },
                        onClick = {
                            menuExpanded = false
                            onChangeIconClick()
                        }
                    )
                }
            }
        )
    }
}