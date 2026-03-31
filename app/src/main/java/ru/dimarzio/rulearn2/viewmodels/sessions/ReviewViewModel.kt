package ru.dimarzio.rulearn2.viewmodels.sessions

import ru.dimarzio.rulearn2.models.Word
import ru.dimarzio.rulearn2.routes.SessionRoutes
import ru.dimarzio.rulearn2.tflite.Features
import ru.dimarzio.rulearn2.tflite.TFLiteModel
import ru.dimarzio.rulearn2.viewmodels.PreferencesViewModel
import ru.dimarzio.rulearn2.viewmodels.PreferencesViewModel.Session

open class ReviewViewModel(
    private val model: TFLiteModel?,
    private val type: Session,
    courseWords: Map<Int, Word>,
    level: String?,
    limit: Int
) : SessionViewModel(ReviewImp(courseWords, level, limit)) {
    override fun makeRoute(word: SessionWord): SessionRoutes? {
        return if (type == Session.GuessingReview) {
            SessionRoutes.GuessingTest
        } else {
            SessionRoutes.TypingTest
        }
    }

    override fun makeWord(prototype: SessionWord, correct: Boolean, hintsUsed: Int): SessionWord {
        val word = prototype.getWord()

        val newAccessed = System.currentTimeMillis()

        val rating = word.rating
        val repetitions = word.repetitions
        val correctAnswers = word.correctAnswers
        val secondsLapsed = if (word.accessed != 0L) {
            (newAccessed - word.accessed) / 1000
        } else {
            0L
        }

        val ratio = correctAnswers.toFloat() / repetitions

        val features = Features(
            id = prototype.getId(),
            repetitions = repetitions,
            correctAnswers = correctAnswers,
            rating = rating,
            secondsLapsed = secondsLapsed,
            typeRepeat = type,
            hintsUsed = hintsUsed
        )

        return prototype.clone(
            prototype.getWord().copy(
                accessed = newAccessed,
                difficult = PreferencesViewModel.settings.markDifficult && !correct && ratio <= 0.75f,
                rating = if (correct) {
                    rating.inc().coerceIn(0..Word.MAX_RATING)
                } else {
                    10
                },
                repetitions = repetitions + 1,
                correctAnswers = if (correct) {
                    correctAnswers + 1
                } else {
                    correctAnswers
                },
                secondsLapsed = secondsLapsed,
                typeRepeat = type,
                hintsUsed = hintsUsed,
                successRate = model?.predict(features) ?: 1f
            )
        )
    }
}