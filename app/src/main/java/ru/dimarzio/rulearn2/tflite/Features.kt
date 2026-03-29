package ru.dimarzio.rulearn2.tflite

import ru.dimarzio.rulearn2.viewmodels.PreferencesViewModel.Session

data class Features( // Flyweight context
    val id: Int,
    val repetitions: Int,
    val correctAnswers: Int, // Not used for predictions.
    val rating: Int,
    val secondsLapsed: Long,
    val typeRepeat: Session?,
    val hintsUsed: Int
)