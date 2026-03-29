package ru.dimarzio.rulearn2.viewmodels.sessions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ru.dimarzio.rulearn2.models.Word
import ru.dimarzio.rulearn2.tflite.TFLiteModel
import ru.dimarzio.rulearn2.viewmodels.PreferencesViewModel.Session

class GuessingReviewViewModel(
    model: TFLiteModel?,
    markDifficult: Boolean,
    courseWords: Map<Int, Word>,
    level: String?,
    limit: Int
) : ReviewViewModel(model, Session.GuessingReview, markDifficult, courseWords, level, limit) {
    var hidden by mutableStateOf(false)
        private set

    fun toggleHidden(hidden: Boolean) {
        this.hidden = hidden
    }

    override fun next() {
        if (!iterator.isDone()) {
            iterator.next()

            hidden = true
        }

        currentWord = iterator.current()
    }
}