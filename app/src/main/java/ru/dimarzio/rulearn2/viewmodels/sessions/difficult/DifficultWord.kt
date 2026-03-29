package ru.dimarzio.rulearn2.viewmodels.sessions.difficult

import ru.dimarzio.rulearn2.models.Word
import ru.dimarzio.rulearn2.tflite.TFLiteModel

class DifficultWord(val id: Int, initialWord: Word, initialState: DifficultState = NoneState) {
    var word = initialWord
    var state = initialState
        private set

    fun getLevel(): Int {
        return state.getLevel()
    }

    fun answer(model: TFLiteModel?, correct: Boolean, hintsUsed: Int) {
        state.answer(this, model, correct, hintsUsed)
    }

    fun changeState(state: DifficultState) {
        this.state = state
    }
}