package ru.dimarzio.rulearn2.viewmodels.sessions.difficult

import ru.dimarzio.rulearn2.tflite.TFLiteModel

object MemorizedState : DifficultState() {
    override fun getLevel(): Int {
        return 3
    }

    override fun answer(context: DifficultWord, model: TFLiteModel?, correct: Boolean, hints: Int) {
        if (!correct) { // Should not happen tho
            context.changeState(TypingState)
        }
    }
}