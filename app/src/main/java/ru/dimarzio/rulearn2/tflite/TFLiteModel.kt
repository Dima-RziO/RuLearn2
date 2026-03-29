package ru.dimarzio.rulearn2.tflite

interface TFLiteModel : AutoCloseable { // GoF Flyweight
    fun predict(contexts: List<Features>): FloatArray
    fun predict(context: Features): Float
    fun getName(): String
}