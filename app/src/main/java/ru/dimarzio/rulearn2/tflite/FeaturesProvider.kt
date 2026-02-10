package ru.dimarzio.rulearn2.tflite

import ru.dimarzio.rulearn2.viewmodels.PreferencesViewModel.Session

interface FeaturesProvider { // Strategy
    fun provide(
        id: Int,
        rating: Int,
        secondsLapsed: Long,
        typeRepeat: Session?,
        hintsUsed: Int
    ): FloatArray
}