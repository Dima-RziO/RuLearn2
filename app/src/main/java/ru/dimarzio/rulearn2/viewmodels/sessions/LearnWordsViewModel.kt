package ru.dimarzio.rulearn2.viewmodels.sessions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ru.dimarzio.rulearn2.models.Word
import ru.dimarzio.rulearn2.viewmodels.PreferencesViewModel.Session

class LearnWordsViewModel(
    courseWords: Map<Int, Word>,
    level: String?,
    limit: Int
) : SessionViewModel(LearnNewWordsImp(courseWords, level, limit)) {
    var hidden by mutableStateOf(false)
        private set

    override fun next() {
        if (!iterator.isDone()) {
            iterator.next()

            hidden = true
        }

        currentWord = iterator.current()
    }

    override fun makeWord(prototype: SessionWord, correct: Boolean, hintsUsed: Int): SessionWord {
        val word = prototype.getWord()
        val newAccessed = System.currentTimeMillis()

        return prototype.clone(
            word.copy(
                accessed = newAccessed,
                rating = if (correct) {
                    word.rating + 1
                } else {
                    word.rating - 1
                },
                secondsLapsed = if (word.accessed != 0L) {
                    (newAccessed - word.accessed) / 1000
                } else {
                    0L
                },
                typeRepeat = Session.LearnNewWords,
                hintsUsed = hintsUsed
            )
        )
    }

    fun toggleHidden(hidden: Boolean) {
        this.hidden = hidden
    }
}