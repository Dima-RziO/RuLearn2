package ru.dimarzio.rulearn2.tflite

class DeprecatedProvider : FeaturesProvider {
    override fun provide(features: Features) = floatArrayOf(
        features.id.toFloat(),
        features.rating.toFloat(),
        features.repetitions.toFloat(),
        features.secondsLapsed.toFloat()
    )
}