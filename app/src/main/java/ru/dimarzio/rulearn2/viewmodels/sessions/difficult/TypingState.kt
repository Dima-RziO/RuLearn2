package ru.dimarzio.rulearn2.viewmodels.sessions.difficult

import ru.dimarzio.rulearn2.models.Word
import ru.dimarzio.rulearn2.tflite.Features
import ru.dimarzio.rulearn2.tflite.TFLiteModel
import ru.dimarzio.rulearn2.viewmodels.PreferencesViewModel

object TypingState : DifficultState() {
    override fun getLevel(): Int {
        return 2
    }

    override fun answer(context: DifficultWord, model: TFLiteModel?, correct: Boolean, hints: Int) {
        if (correct) {
            val newAccessed = System.currentTimeMillis()
            val newWord = if (context.word.isRepeat) {
                val repetitions = context.word.repetitions + 1
                val correctAnswers = context.word.correctAnswers
                val rating = context.word.rating.inc().coerceIn(0..Word.MAX_RATING)
                val typeRepeat = PreferencesViewModel.Session.DifficultWords
                val hintsFraction = hints.toFloat() / context.word.name.length
                val secondsLapsed = if (context.word.accessed != 0L) {
                    (newAccessed - context.word.accessed) / 1000
                } else {
                    0L
                }

                val features = Features(
                    id = context.id,
                    repetitions = repetitions,
                    correctAnswers = correctAnswers,
                    rating = rating,
                    secondsLapsed = secondsLapsed,
                    typeRepeat = typeRepeat,
                    hintsFraction = hintsFraction
                )

                context.word.copy(
                    accessed = newAccessed,
                    difficult = false,
                    rating = rating,
                    repetitions = repetitions,
                    correctAnswers = correctAnswers,
                    secondsLapsed = secondsLapsed,
                    typeRepeat = typeRepeat,
                    hintsFraction = hintsFraction,
                    successRate = model?.predict(features) ?: 1f
                )
            } else {
                context.word.copy(difficult = false)
            }

            context.word = newWord

            changeState(context, MemorizedState)
        }
    }
}