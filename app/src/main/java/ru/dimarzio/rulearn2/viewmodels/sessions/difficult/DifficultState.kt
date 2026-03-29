package ru.dimarzio.rulearn2.viewmodels.sessions.difficult

import ru.dimarzio.rulearn2.tflite.TFLiteModel

open class DifficultState { // GoF State
    protected fun changeState(context: DifficultWord, state: DifficultState) {
        context.changeState(state)
    }

    open fun getLevel(): Int {
        return -1
    }

    open fun answer(context: DifficultWord, model: TFLiteModel?, correct: Boolean, hints: Int) {

    }
}