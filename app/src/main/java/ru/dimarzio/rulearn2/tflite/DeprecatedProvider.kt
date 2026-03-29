package ru.dimarzio.rulearn2.tflite

import ru.dimarzio.rulearn2.viewmodels.PreferencesViewModel.Session

class DeprecatedProvider : FeaturesProvider {
    override fun provide(
        id: Int,
        repetitions: Int,
        rating: Int,
        secondsLapsed: Long,
        typeRepeat: Session?,
        hintsUsed: Int
    ) = floatArrayOf(
        id.toFloat(),
        rating.toFloat(),
        repetitions.toFloat(),
        secondsLapsed.toFloat()
    )
}