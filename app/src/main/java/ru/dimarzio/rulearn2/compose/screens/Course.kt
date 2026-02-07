package ru.dimarzio.rulearn2.compose.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch
import ru.dimarzio.rulearn2.R
import ru.dimarzio.rulearn2.compose.AboutDialog
import ru.dimarzio.rulearn2.compose.AppBarActions
import ru.dimarzio.rulearn2.compose.FilledHeader
import ru.dimarzio.rulearn2.compose.GroupedMultiChoiceDialog
import ru.dimarzio.rulearn2.compose.Sessions
import ru.dimarzio.rulearn2.compose.SwipeToRevealBox
import ru.dimarzio.rulearn2.compose.TextFieldDialog
import ru.dimarzio.rulearn2.models.Level
import ru.dimarzio.rulearn2.models.Word
import ru.dimarzio.rulearn2.utils.percentageFrom
import ru.dimarzio.rulearn2.utils.toast
import ru.dimarzio.rulearn2.viewmodels.Filter
import ru.dimarzio.rulearn2.viewmodels.PreferencesViewModel.Session

@Composable
fun Course(
    course: String,
    onNavigationIconClick: () -> Unit,
    onAddActionClick: (String) -> Unit,
    filterRepeat: Boolean,
    filterNotRepeat: Boolean,
    filterDifficult: Boolean,
    filterNotDifficult: Boolean,
    filterSkip: Boolean,
    filterNotSkip: Boolean,
    filterLearned: Boolean,
    filterNotLearned: Boolean,
    onFilterActionClick: Filter,
    onSettingsActionClick: () -> Unit,
    searching: Boolean,
    searchResults: Map<String, Map<Int, Word>>,
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (CoroutineScope) -> Deferred<Boolean>,
    words: Map<Int, Word>,
    selectedSession: Session,
    onLearnNewWordsClick: () -> Unit,
    onDifficultWordsClick: () -> Unit,
    onTypingReviewClick: () -> Unit,
    onGuessingReviewClick: () -> Unit,
    loading: Boolean,
    onWordClick: (Int, String) -> Unit,
    levels: Map<String, Level>,
    onLevelClick: (String) -> Unit,
    confirmRenameLevel: (String) -> Boolean,
    onRenameLevelClick: (String, String) -> Unit,
    onDeleteLevelClick: (String) -> Unit
) {
    val listState = rememberLazyListState()

    val scrollButtonVisible by remember {
        derivedStateOf {
            if (levels.any { (_, level) -> level.learned > 0 }) {
                val index = levels.values.indexOfLast { level -> level.learned > 0 }
                index !in listState.layoutInfo.visibleItemsInfo.map(LazyListItemInfo::index)
            } else {
                false
            }
        }
    }

    val scrollButtonUp by remember {
        derivedStateOf {
            if (listState.layoutInfo.visibleItemsInfo.isNotEmpty()) {
                val index = levels.values.indexOfLast { level -> level.learned > 0 }
                index < listState.layoutInfo.visibleItemsInfo.last().index
            } else {
                false
            }
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            AppBar(
                course = course,
                searching = searching,
                results = searchResults,
                query = query,
                onQueryChange = onQueryChange,
                onSearch = onSearch,
                onNavigationIconClick = onNavigationIconClick,
                onAddActionClick = onAddActionClick,
                filterRepeat = filterRepeat,
                filterNotRepeat = filterNotRepeat,
                filterDifficult = filterDifficult,
                filterNotDifficult = filterNotDifficult,
                filterSkip = filterSkip,
                filterNotSkip = filterNotSkip,
                filterLearned = filterLearned,
                filterNotLearned = filterNotLearned,
                onFilterActionClick = onFilterActionClick,
                onSettingsActionClick = onSettingsActionClick,
                onWordClick = onWordClick
            )
        },
        floatingActionButton = {
            Sessions(
                selectedMode = selectedSession,
                onLearnNewWordsClick = onLearnNewWordsClick,
                onDifficultWordsClick = onDifficultWordsClick,
                onTypingReviewClick = onTypingReviewClick,
                onGuessingReviewClick = onGuessingReviewClick,
                toLearnNumber = words.count { (_, word) -> !word.learned && !word.skip },
                difficultNumber = words.count { (_, word) -> word.isDifficult },
                toRepeatNumber = words.count { (_, word) -> word.isRepeat },
                scrollButtonVisible = scrollButtonVisible,
                onScrollClick = {
                    coroutineScope.launch {
                        listState.scrollToItem(levels.values.indexOfLast { it.learned > 0 })
                    }
                },
                scrollUp = scrollButtonUp
            )
        },
    ) { innerPadding ->
        val learnedNumber = words.count { (_, word) -> word.learned && !word.skip }

        val totalWords = words.count { (_, word) -> !word.skip }

        Column(modifier = Modifier.padding(innerPadding)) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$learnedNumber/$totalWords learned over ${levels.size} levels",
                    fontStyle = FontStyle.Italic
                )

                Spacer(modifier = Modifier.size(5.dp))

                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                }
            }

            LevelsList(
                state = listState,
                levels = levels,
                onLevelClick = onLevelClick,
                confirmRename = confirmRenameLevel,
                onRenameClick = onRenameLevelClick,
                onDeleteClick = onDeleteLevelClick
            )

            val toRepeatNumber by remember(key1 = words) {
                mutableIntStateOf(words.count { (_, word) -> word.isRepeat })
            }

            LaunchedEffect(key1 = toRepeatNumber) {
                if (toRepeatNumber > 0) {
                    context.toast("$toRepeatNumber words for repetition are found!")
                }
            }
        }
    }
}

private fun AnnotatedString(text: String, color: Color, range: IntRange) = AnnotatedString(
    text,
    listOf(
        AnnotatedString.Range(
            SpanStyle(color),
            range.first,
            range.last
        )
    )
)

@Composable
private fun SearchListItem(
    id: Int,
    word: Word,
    searchText: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            val index = word.normalizedName.indexOf(searchText)

            Text(
                text = if (index != -1) {
                    AnnotatedString(
                        word.name,
                        MaterialTheme.colorScheme.primary,
                        index..index + searchText.length
                    )
                } else {
                    AnnotatedString(word.name)
                },
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            val index = word.translation.indexOf(searchText)

            Text(
                text = if (index != -1) {
                    AnnotatedString(
                        word.translation,
                        MaterialTheme.colorScheme.primary,
                        index..index + searchText.length
                    )
                } else {
                    AnnotatedString(word.translation)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            Text(
                text = id.toString(),
                fontWeight = FontWeight.Bold.takeIf { searchText.toIntOrNull() == id }
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchList(
    words: Map<String, Map<Int, Word>>,
    searchText: String,
    onWordClick: (Int, String) -> Unit
) {
    LazyColumn {
        words.forEach { (level, words) ->
            stickyHeader {
                FilledHeader(header = "$level (${words.size})")
            }

            items(words.toList()) { (id, word) ->
                SearchListItem(
                    id = id,
                    word = word,
                    searchText = searchText,
                    onClick = { onWordClick(id, word.level) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppBar(
    course: String,
    searching: Boolean,
    results: Map<String, Map<Int, Word>>,
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: CoroutineScope.() -> Deferred<Boolean>,
    onNavigationIconClick: () -> Unit,
    onAddActionClick: (String) -> Unit,
    filterRepeat: Boolean,
    filterNotRepeat: Boolean,
    filterDifficult: Boolean,
    filterNotDifficult: Boolean,
    filterSkip: Boolean,
    filterNotSkip: Boolean,
    filterLearned: Boolean,
    filterNotLearned: Boolean,
    onFilterActionClick: Filter,
    onSettingsActionClick: () -> Unit,
    onWordClick: (Int, String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = query,
                    onQueryChange = onQueryChange,
                    onSearch = { _ ->
                        coroutineScope.launch {
                            if (!onSearch().await()) {
                                context.toast("Nothing found")
                            }
                        }
                    },
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    leadingIcon = {
                        IconButton(onClick = onNavigationIconClick) {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_course_24),
                                contentDescription = "View courses"
                            )
                        }
                    },
                    trailingIcon = {
                        Row {
                            TopBarActions(
                                expanded = expanded,
                                onAddActionClick = onAddActionClick,
                                filterRepeat = filterRepeat,
                                filterNotRepeat = filterNotRepeat,
                                filterDifficult = filterDifficult,
                                filterNotDifficult = filterNotDifficult,
                                filterSkip = filterSkip,
                                filterNotSkip = filterNotSkip,
                                filterLearned = filterLearned,
                                filterNotLearned = filterNotLearned,
                                onFilterActionClick = onFilterActionClick,
                                onSettingsActionClick = onSettingsActionClick
                            )
                        }
                    },
                    placeholder = {
                        Text(text = course)
                    }
                )
            },
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            if (!searching) {
                SearchList(
                    words = results,
                    searchText = query,
                    onWordClick = onWordClick
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun AddLevelDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: (String) -> Unit
) {
    TextFieldDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = "Save",
        onConfirmation = onConfirmation,
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = "Cancel")
            }
        },
        title = "New level",
        initialInput = "",
        label = "Level name"
    )
}

@Composable
private fun FilterDialog(
    onDismissRequest: () -> Unit,
    repeat: Boolean,
    notRepeat: Boolean,
    difficult: Boolean,
    notDifficult: Boolean,
    skip: Boolean,
    notSkip: Boolean,
    learned: Boolean,
    notLearned: Boolean,
    onConfirmation: Filter
) {
    GroupedMultiChoiceDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = "Filter",
        onConfirmation = { checked ->
            onConfirmation(
                "Repeat" in checked,
                "Not repeat" in checked,
                "Difficult" in checked,
                "Not difficult" in checked,
                "Skip" in checked,
                "Not skip" in checked,
                "Learned" in checked,
                "Unlearned" in checked
            )
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = "Cancel")
            }
        },
        title = "Filter search results",
        groupedItems = mapOf(
            "Repeat" to mapOf("Repeat" to repeat, "Not repeat" to notRepeat),
            "Difficult" to mapOf("Difficult" to difficult, "Not difficult" to notDifficult),
            "Skip" to mapOf("Skip" to skip, "Not skip" to notSkip),
            "Learned" to mapOf("Learned" to learned, "Unlearned" to notLearned)
        )
    )
}

@Composable
private fun TopBarActions(
    expanded: Boolean,
    onAddActionClick: (String) -> Unit,
    filterRepeat: Boolean,
    filterNotRepeat: Boolean,
    filterDifficult: Boolean,
    filterNotDifficult: Boolean,
    filterSkip: Boolean,
    filterNotSkip: Boolean,
    filterLearned: Boolean,
    filterNotLearned: Boolean,
    onFilterActionClick: Filter,
    onSettingsActionClick: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AddLevelDialog(
            onDismissRequest = { showAddDialog = false },
            onConfirmation = onAddActionClick
        )
    }

    if (showFilterDialog) {
        FilterDialog(
            onDismissRequest = { showFilterDialog = false },
            repeat = filterRepeat,
            notRepeat = filterNotRepeat,
            difficult = filterDifficult,
            notDifficult = filterNotDifficult,
            skip = filterSkip,
            notSkip = filterNotSkip,
            learned = filterLearned,
            notLearned = filterNotLearned,
            onConfirmation = onFilterActionClick
        )
    }

    if (showAboutDialog) {
        AboutDialog {
            showAboutDialog = false
        }
    }

    if (expanded) {
        val filterIcon = ImageVector.vectorResource(id = R.drawable.baseline_filter_list_24)
        AppBarActions(
            Triple(filterIcon, "Filter search") { showFilterDialog = true },
            Triple(null, "Settings", onSettingsActionClick),
            Triple(null, "About") { showAboutDialog = true }
        )
    } else {
        AppBarActions(
            Triple(Icons.Filled.Add, "Add level") { showAddDialog = true },
            Triple(null, "Settings", onSettingsActionClick),
            Triple(null, "About") { showAboutDialog = true }
        )
    }
}

@Composable
private fun ConfirmRenameDialog(
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
                Text(text = "Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = "Cancel")
            }
        },
        title = {
            Text(text = "A level with same name already exists")
        },
        text = {
            Text(text = "Renaming will result in merging two levels.")
        }
    )
}

@Composable
private fun LevelsList(
    state: LazyListState,
    levels: Map<String, Level>,
    onLevelClick: (String) -> Unit,
    confirmRename: (String) -> Boolean,
    onRenameClick: (String, String) -> Unit,
    onDeleteClick: (String) -> Unit
) {
    var renameFrom by remember { mutableStateOf(null as String?) }
    var renameTo by remember { mutableStateOf(null as String?) }

    if (renameFrom != null && renameTo != null) {
        ConfirmRenameDialog(
            onDismissRequest = {
                renameFrom = null
                renameTo = null
            },
            onConfirmation = { onRenameClick(checkNotNull(renameFrom), checkNotNull(renameTo)) }
        )
    }

    LazyColumn(state = state) {
        items(levels.toList()) { (name, level) ->
            LevelsListItem(
                name = name,
                level = level,
                onClick = { onLevelClick(name) },
                onRenameClick = { to ->
                    if (confirmRename(to)) {
                        onRenameClick(name, to)
                    } else {
                        renameFrom = name
                        renameTo = to
                    }
                },
                onDeleteClick = { onDeleteClick(name) },
            )
        }
    }
}

@Composable
private fun RenameLevelDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: (String) -> Unit,
    name: String
) {
    TextFieldDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = "Rename",
        confirmButtonEnabled = { input -> input.isNotBlank() && input != name },
        onConfirmation = onConfirmation,
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = "Cancel")
            }
        },
        title = name,
        initialInput = name,
        label = "Level name"
    )
}

@Composable
private fun ConfirmDeleteDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    level: String
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
                Text(text = "Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = "Cancel")
            }
        },
        title = {
            Text(text = "Delete $level?")
        },
        text = {
            Text(text = "This cannot be undone.")
        }
    )
}

@Composable
private fun LevelsListItemContainer(
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
    content: @Composable () -> Unit
) {
    SwipeToRevealBox(
        hiddenContentStart = {
            Icon(
                imageVector = Icons.Filled.Create,
                contentDescription = "Rename"
            )
        },
        onHiddenContentStartClick = onRenameClick,
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
        onHiddenContentEndClick = onDeleteClick,
        content = content
    )
}

@Composable
private fun LevelsListItem(
    name: String,
    level: Level,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRenameClick: (String) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            onDismissRequest = { showDeleteDialog = false },
            onConfirmation = onDeleteClick,
            level = name
        )
    }

    if (showRenameDialog) {
        RenameLevelDialog(
            onDismissRequest = { showRenameDialog = false },
            onConfirmation = onRenameClick,
            name = name
        )
    }

    LevelsListItemContainer(
        onRenameClick = { showRenameDialog = true },
        onDeleteClick = { showDeleteDialog = true }
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            modifier = Modifier
                .clickable(onClick = onClick)
                .height(IntrinsicSize.Max),
            supportingContent = {
                Column {
                    Text(
                        text = "${level.learned}/${level.total} learned",
                        fontSize = 12.sp
                    )

                    Text(
                        text = "${level.toRepeat} to repeat",
                        fontSize = 12.sp
                    )
                }
            },
            leadingContent = {
                Box(
                    modifier = Modifier.fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { (level.learned percentageFrom level.total) / 100 },
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
        )
    }
}