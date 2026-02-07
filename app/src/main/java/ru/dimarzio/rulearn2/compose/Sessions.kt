package ru.dimarzio.rulearn2.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ru.dimarzio.rulearn2.R
import ru.dimarzio.rulearn2.viewmodels.PreferencesViewModel.Session

@Composable
private fun SessionsDialog(
    onDismissRequest: () -> Unit,
    onLearnNewWordsClick: () -> Unit,
    onDifficultWordsClick: () -> Unit,
    onTypingReviewClick: () -> Unit,
    onGuessingReviewClick: () -> Unit,
    toLearnNumber: Int,
    difficultNumber: Int,
    toRepeatNumber: Int,
    selectedMode: Session
) {
    SingleChoiceDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = "Choose",
        onConfirmation = { item ->
            when (item) {
                Session.LearnNewWords -> onLearnNewWordsClick()
                Session.DifficultWords -> onDifficultWordsClick()
                Session.TypingReview -> onTypingReviewClick()
                Session.GuessingReview -> onGuessingReviewClick()
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = "Cancel")
            }
        },
        title = "Choose more modes",
        items = enumValues<Session>().toSet(),
        selected = selectedMode,
        getLabel = { item ->
            when (item) {
                Session.LearnNewWords -> item.name + " ($toLearnNumber)"
                Session.DifficultWords -> item.name + " ($difficultNumber)"
                Session.TypingReview -> item.name + " ($toRepeatNumber)"
                Session.GuessingReview -> item.name + " ($toRepeatNumber)"
            }
        }
    )
}

@Composable
fun Sessions(
    selectedMode: Session,
    onLearnNewWordsClick: () -> Unit,
    onDifficultWordsClick: () -> Unit,
    onTypingReviewClick: () -> Unit,
    onGuessingReviewClick: () -> Unit,
    toLearnNumber: Int,
    difficultNumber: Int,
    toRepeatNumber: Int,
    scrollButtonVisible: Boolean,
    onScrollClick: () -> Unit,
    scrollUp: Boolean
) {
    var showMoreDialog by remember { mutableStateOf(false) }

    if (showMoreDialog) {
        SessionsDialog(
            onDismissRequest = { showMoreDialog = false },
            onLearnNewWordsClick = onLearnNewWordsClick,
            onDifficultWordsClick = onDifficultWordsClick,
            onTypingReviewClick = onTypingReviewClick,
            onGuessingReviewClick = onGuessingReviewClick,
            toLearnNumber = toLearnNumber,
            difficultNumber = difficultNumber,
            toRepeatNumber = toRepeatNumber,
            selectedMode = selectedMode
        )
    }

    Column {
        AnimatedVisibility(
            visible = scrollButtonVisible,
            modifier = Modifier.align(Alignment.End),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            SmallFloatingActionButton(onClick = onScrollClick) {
                Icon(
                    imageVector = if (scrollUp) {
                        Icons.Filled.KeyboardArrowUp
                    } else {
                        Icons.Filled.KeyboardArrowDown
                    },
                    contentDescription = "Scroll"
                )
            }
        }

        Spacer(modifier = Modifier.size(10.dp))

        Row {
            FloatingActionButton(
                onClick = { showMoreDialog = true },
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_more_24),
                    contentDescription = "More"
                )
            }

            Spacer(modifier = Modifier.size(5.dp))

            val number = when (selectedMode) {
                Session.LearnNewWords -> toLearnNumber
                Session.DifficultWords -> difficultNumber
                Session.TypingReview, Session.GuessingReview -> toRepeatNumber
            }
            ExtendedFloatingActionButton(
                text = {
                    Text(text = selectedMode.name + " ($number)")
                },
                icon = {
                    Icon(
                        painter = painterResource(
                            id = when (selectedMode) {
                                Session.LearnNewWords -> R.drawable.baseline_course_24
                                Session.DifficultWords -> R.drawable.baseline_difficult_24
                                Session.TypingReview -> R.drawable.baseline_keyboard_24
                                Session.GuessingReview -> R.drawable.baseline_repeat_24
                            },
                        ),
                        contentDescription = selectedMode.name
                    )
                },
                onClick = when (selectedMode) {
                    Session.LearnNewWords -> onLearnNewWordsClick
                    Session.DifficultWords -> onDifficultWordsClick
                    Session.TypingReview -> onTypingReviewClick
                    Session.GuessingReview -> onGuessingReviewClick
                }
            )
        }
    }
}