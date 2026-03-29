package ru.dimarzio.rulearn2.viewmodels.sessions.difficult

import ru.dimarzio.rulearn2.tflite.TFLiteModel

object NoneState : DifficultState() {
    override fun getLevel(): Int {
        return 0
    }

    override fun answer(context: DifficultWord, model: TFLiteModel?, correct: Boolean, hints: Int) {
        if (correct) {
            changeState(context, GuessingState)
        }
    }
}