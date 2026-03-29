package ru.dimarzio.rulearn2.viewmodels.sessions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ru.dimarzio.rulearn2.models.Word
import ru.dimarzio.rulearn2.tflite.TFLiteModel
import ru.dimarzio.rulearn2.viewmodels.sessions.difficult.DifficultWord
import ru.dimarzio.rulearn2.viewmodels.sessions.difficult.DifficultWordAdapter
import ru.dimarzio.rulearn2.viewmodels.sessions.difficult.DifficultWordsImp

class DifficultWordsViewModel(
    private val model: TFLiteModel?,
    courseWords: Map<Int, Word>,
    level: String?,
    limit: Int
) : SessionViewModel(DifficultWordsImp(courseWords, level, limit)) {
    var hidden by mutableStateOf(false)
        private set

    override fun next() {
        if (!iterator.isDone()) {
            iterator.next()

            hidden = true
        }

        currentWord = iterator.current()
    }

    override fun answer(correct: Boolean, hintsUsed: Int): Word? {
        val word = iterator.current()?.adaptee() as? DifficultWord
        if (word != null) {
            word.answer(model, correct, hintsUsed)
            iterator.emend(DifficultWordAdapter(word))

            return word.word
        }

        return null
    }

    fun toggleHidden(hidden: Boolean) {
        this.hidden = hidden
    }
}