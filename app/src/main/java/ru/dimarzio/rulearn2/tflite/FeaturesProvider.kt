package ru.dimarzio.rulearn2.tflite

interface FeaturesProvider { // GoF Strategy
    fun provide(features: Features): FloatArray
}