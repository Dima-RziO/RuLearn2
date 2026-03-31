package ru.dimarzio.rulearn2.viewmodels.sessions

import ru.dimarzio.rulearn2.models.Word
import ru.dimarzio.rulearn2.routes.SessionRoutes
import ru.dimarzio.rulearn2.tflite.TFLiteModel
import ru.dimarzio.rulearn2.viewmodels.sessions.difficult.DifficultWord
import ru.dimarzio.rulearn2.viewmodels.sessions.difficult.DifficultWordAdapter
import ru.dimarzio.rulearn2.viewmodels.sessions.difficult.DifficultWordsImp
import ru.dimarzio.rulearn2.viewmodels.sessions.difficult.GuessingState
import ru.dimarzio.rulearn2.viewmodels.sessions.difficult.NoneState
import ru.dimarzio.rulearn2.viewmodels.sessions.difficult.TypingState

class DifficultWordsViewModel(
    private val model: TFLiteModel?,
    courseWords: Map<Int, Word>,
    level: String?,
    limit: Int
) : SessionViewModel(DifficultWordsImp(courseWords, level, limit)) {
    override fun makeRoute(word: SessionWord): SessionRoutes? {
        val difficultWord = word.adaptee() as DifficultWord
        return when (difficultWord.state) {
            NoneState, TypingState -> SessionRoutes.TypingTest
            GuessingState -> SessionRoutes.GuessingTest
            else -> null
        }
    }

    override fun answer(correct: Boolean, hintsUsed: Int): Word? {
        val word = imp.current()?.adaptee() as? DifficultWord

        if (word != null) {
            word.answer(model, correct, hintsUsed)
            imp.emend(DifficultWordAdapter(word))

            return word.word
        }

        return null
    }
}