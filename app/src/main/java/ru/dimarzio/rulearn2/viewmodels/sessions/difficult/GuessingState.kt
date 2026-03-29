package ru.dimarzio.rulearn2.viewmodels.sessions.difficult

import ru.dimarzio.rulearn2.tflite.TFLiteModel

object GuessingState : DifficultState() {
    override fun getLevel(): Int {
        return 1
    }

    override fun answer(context: DifficultWord, model: TFLiteModel?, correct: Boolean, hints: Int) {
        if (correct) {
            changeState(context, TypingState)
        } else {
            changeState(context, NoneState)
        }
    }
}