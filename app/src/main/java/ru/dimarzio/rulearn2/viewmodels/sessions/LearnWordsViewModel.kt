package ru.dimarzio.rulearn2.viewmodels.sessions

import ru.dimarzio.rulearn2.models.Word
import ru.dimarzio.rulearn2.routes.SessionRoutes
import ru.dimarzio.rulearn2.viewmodels.PreferencesViewModel.Session

class LearnWordsViewModel(
    courseWords: Map<Int, Word>,
    level: String?,
    limit: Int
) : SessionViewModel(LearnNewWordsImp(courseWords, level, limit)) {
    init {
        val word = imp.current()
        if (word != null) {
            makeRoute(word)?.let { route -> _navigationEvents.trySend(route.route to word) }
        }
    }

    override fun makeRoute(word: SessionWord): SessionRoutes? {
        return when (word.getWord().rating) {
            0 -> SessionRoutes.NewWord
            in 1..8 -> SessionRoutes.GuessingTest
            9 -> SessionRoutes.TypingTest
            else -> null
        }
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
                hintsFraction = hintsUsed.toFloat() / word.name.length
            )
        )
    }
}