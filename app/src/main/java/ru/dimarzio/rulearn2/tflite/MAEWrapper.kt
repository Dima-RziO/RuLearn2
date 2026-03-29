package ru.dimarzio.rulearn2.tflite

import kotlin.math.abs

class MAEWrapper(model: TFLiteModel) : ModelWrapper(model) {
    var mae = 0f
        private set

    override fun predict(contexts: List<Features>): FloatArray {
        val predictions = super.predict(contexts)

        val error = FloatArray(contexts.size) { i ->
            abs(predictions[i] - with(contexts[i]) { correctAnswers / repetitions })
        }

        mae = error.average().toFloat()

        return predictions
    }

    override fun predict(context: Features): Float {
        val prediction = super.predict(context)
        val label = context.correctAnswers / context.repetitions

        mae = abs(prediction - label)

        return prediction
    }
}