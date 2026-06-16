package ru.dimarzio.rulearn2.compose.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import ru.dimarzio.rulearn2.R
import ru.dimarzio.rulearn2.compose.AppBarActions
import ru.dimarzio.rulearn2.compose.MultiChoiceDialog
import ru.dimarzio.rulearn2.compose.ProgressDialog
import ru.dimarzio.rulearn2.compose.RenameDeleteBox
import ru.dimarzio.rulearn2.compose.SingleChoiceDialog
import ru.dimarzio.rulearn2.compose.TextFieldDialog
import ru.dimarzio.rulearn2.models.Course
import ru.dimarzio.rulearn2.utils.toast
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Courses(
    onExportCsvClick: (String?) -> Unit,
    onExportDatabaseClick: () -> Unit,
    endpoints: Map<String, String>,
    connectedId: String?,
    connectingId: String?,
    connectionCode: String?,
    onConnectionRequested: (String) -> Unit,
    acceptConnection: () -> Unit,
    rejectConnection: () -> Unit,
    advertise: Boolean,
    startAdvertising: () -> Unit,
    stopAdvertising: () -> Unit,
    startDiscovery: () -> Unit,
    stopDiscovery: () -> Unit,
    onReplicateClick: (String) -> Unit,
    onSQLiteClick: (String) -> Unit,
    onSettingsActionClick: () -> Unit,
    onImportClick: () -> Unit,
    courses: Map<String, Course>,
    onCourseClick: (String) -> Unit,
    onDeleteCourseClick: (String) -> Unit,
    onRenameClick: (String, String) -> Unit,
    modelLoaded: (String) -> Boolean,
    onChangeIconClick: (String) -> Unit,
    importProgress: Float?,
    deleteProgress: Float?,
    exportProgress: Float?,
    transferProgress: Float?,
    alreadyReplicated: Long?,
    onAlreadyReplicatedDismissed: () -> Unit,
    onAlreadyConfirmation: () -> Unit,
    onAlreadyDenial: () -> Unit,
    replicationCourses: List<String>?,
    onReplicationCoursesDismissed: () -> Unit,
    onReplicationCoursesSelected: (Set<String>) -> Unit,
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
                        endpoints = endpoints,
                        connectedId = connectedId,
                        connectingId = connectingId,
                        onConnectionRequested = onConnectionRequested,
                        advertise = advertise,
                        onAdvertiseChange = { advertise ->
                            if (advertise) {
                                startAdvertising()
                            } else {
                                stopAdvertising()
                            }
                        },
                        startDiscovery = startDiscovery,
                        stopDiscovery = stopDiscovery,
                        onExportActionClick = { csvMethod: Boolean ->
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

        if (transferProgress != null) {
            ProgressDialog(
                title = "Transferring...",
                progress = transferProgress.roundToInt(),
                onDismissRequest = {}
            )
        }

        if (connectionCode != null) {
            ConnectionCodeDialog(
                code = connectionCode,
                onDismissRequest = rejectConnection,
                onConfirmation = acceptConnection
            )
        }

        if (alreadyReplicated != null) {
            AlreadyReplicatedDialog(
                onDismissRequest = onAlreadyReplicatedDismissed,
                onConfirmation = onAlreadyConfirmation,
                onDenial = onAlreadyDenial,
                millis = alreadyReplicated
            )
        }

        if (replicationCourses != null) {
            ReplicationCoursesDialog(
                onDismissRequest = onReplicationCoursesDismissed,
                onConfirmation = onReplicationCoursesSelected,
                courses = replicationCourses
            )
        }

        if (isReplicating) {
            ProgressDialog(
                title = "Replicating...",
                message = "Please wait...",
                onDismissRequest = {}
            )
        }

        CoursesList(
            modifier = Modifier.padding(innerPadding),
            courses = courses,
            onCourseClick = onCourseClick,
            onExportCourseClick = onExportCsvClick,
            onDeleteCourseClick = onDeleteCourseClick,
            onRenameClick = onRenameClick,
            modelLoaded = modelLoaded,
            onChangeIconClick = onChangeIconClick
        )
    }
}

@Composable
private fun rememberP2PPermissions(
    onGranted: () -> Unit,
    onDenied: () -> Unit
): () -> Unit {
    val context = LocalContext.current

    val permissions = remember {
        buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            } else {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.all { it.value }) onGranted() else onDenied()
    }

    val checkAndLaunch: () -> Unit = {
        val needRequest = permissions.any {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needRequest) {
            launcher.launch(permissions.toTypedArray())
        } else {
            onGranted()
        }
    }

    return checkAndLaunch
}

@Composable
private fun TopBarActions(
    endpoints: Map<String, String>,
    connectedId: String?,
    connectingId: String?,
    onConnectionRequested: (String) -> Unit,
    advertise: Boolean,
    onAdvertiseChange: (Boolean) -> Unit,
    startDiscovery: () -> Unit,
    stopDiscovery: () -> Unit,
    onExportActionClick: (Boolean) -> Unit,
    onReplicateClick: (String) -> Unit,
    onSQLiteClick: (String) -> Unit,
    onSettingsActionClick: () -> Unit
) {
    val context = LocalContext.current

    var showExportMethodDialog by remember { mutableStateOf(false) }
    var showReplicateDialog by remember { mutableStateOf(false) }
    var showSQLiteQueryDialog by remember { mutableStateOf(false) }

    if (showExportMethodDialog) {
        ExportMethodDialog(
            onDismissRequest = { showExportMethodDialog = false },
            onConfirmation = onExportActionClick
        )
    }

    if (showReplicateDialog) {
        ReplicateDialog(
            endpoints = endpoints,
            connectedId = connectedId,
            connectingId = connectingId,
            onConnectionRequested = onConnectionRequested,
            onDismissRequest = {
                showReplicateDialog = false
                stopDiscovery()
            },
            onConfirmation = onReplicateClick
        )
    }

    if (showSQLiteQueryDialog) {
        SQLiteQueryDialog(
            onDismissRequest = { showSQLiteQueryDialog = false },
            onConfirmation = onSQLiteClick
        )
    }

    val exportIcon = ImageVector.vectorResource(id = R.drawable.mdi_export_24)
    val transferIcon = ImageVector.vectorResource(id = R.drawable.mdi_transfer_24)
    val queryIcon = ImageVector.vectorResource(id = R.drawable.baseline_terminal_24)

    val discoveryLauncher = rememberP2PPermissions(
        onGranted = {
            startDiscovery()
            showReplicateDialog = true
        },
        onDenied = {
            context.toast("Permissions were not granted.")
        }
    )

    val advertisingLauncher = rememberP2PPermissions(
        onGranted = {
            onAdvertiseChange(true)
        },
        onDenied = {
            context.toast("Permissions were not granted.")
        }
    )

    AppBarActions(
        actions = {
            Action(exportIcon, "Export all courses") {
                showExportMethodDialog = true
            }
            Action(transferIcon, "Replicate database", discoveryLauncher)
        },
        overflowMenu = {
            CheckboxAction("Advertise", advertise) { checked ->
                if (checked) {
                    advertisingLauncher.invoke()
                } else {
                    onAdvertiseChange(false)
                }
            }
            OverflowAction("Execute SQLite query") { showSQLiteQueryDialog = true }
            SettingsAction(onSettingsActionClick)
            AboutAction()
        }
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
    endpoints: Map<String, String>,
    connectedId: String?,
    connectingId: String?,
    onConnectionRequested: (String) -> Unit,
    onDismissRequest: () -> Unit,
    onConfirmation: (String) -> Unit,
) {
    SingleChoiceDialog(
        title = "Replicate...",
        confirmButton = "Replicate",
        items = endpoints.keys,
        selected = connectedId ?: connectingId,
        confirmEnabled = connectingId == null,
        onItemSelected = onConnectionRequested,
        onDismissRequest = onDismissRequest,
        onConfirmation = onConfirmation,
        getLabel = endpoints::getValue
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
private fun ConnectionCodeDialog(
    code: String,
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onConfirmation) {
                Text(text = "Accept")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = "Reject")
            }
        },
        title = {
            Text(text = "Confirm connection")
        },
        text = {
            Text(text = "The code on the other device is $code.")
        }
    )
}

private fun Long.millisToString(): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(this)
}

@Composable
private fun AlreadyReplicatedDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    onDenial: () -> Unit,
    millis: Long
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onConfirmation) {
                Text(text = "Redownload")
            }
        },
        dismissButton = {
            TextButton(onClick = onDenial) {
                Text(text = "Use existing")
            }
        },
        title = {
            Text(text = "Download the file again?")
        },
        text = {
            Text(text = "Replication from this host ran at ${millis.millisToString()}.")
        }
    )
}

@Composable
private fun ReplicationCoursesDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: (Set<String>) -> Unit,
    courses: List<String>
) {
    MultiChoiceDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = "Replicate",
        onConfirmation = onConfirmation,
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = "Cancel")
            }
        },
        title = "Choose replication courses...",
        items = courses.associateWith { true }
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
    modelLoaded: (String) -> Boolean,
    onChangeIconClick: (String) -> Unit
) {
    LazyColumn(modifier = modifier) {
        items(courses.toList()) { (name, course) ->
            CoursesListItem(
                name = name,
                course = course,
                onClick = { onCourseClick(name) },
                onExportClick = { onExportCourseClick(name) },
                onDeleteClick = { onDeleteCourseClick(name) },
                onRenameClick = { to -> onRenameClick(name, to) },
                modelLoaded = modelLoaded(name),
                onChangeIconClick = { onChangeIconClick(name) }
            )
        }
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
    modelLoaded: Boolean,
    onChangeIconClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showRenameDialog) {
        RenameDialog(
            onDismissRequest = { showRenameDialog = false },
            onConfirmation = onRenameClick,
            course = name
        )
    }

    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            course = name,
            onDismissRequest = { showDeleteDialog = false },
            onConfirmation = onDeleteClick
        )
    }

    RenameDeleteBox(
        onRenameClick = { showRenameDialog = true },
        onDeleteClick = { showDeleteDialog = true }
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = name,
                    fontSize = 16.sp,
                    fontStyle = if (modelLoaded) {
                        FontStyle.Italic
                    } else {
                        FontStyle.Normal
                    },
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