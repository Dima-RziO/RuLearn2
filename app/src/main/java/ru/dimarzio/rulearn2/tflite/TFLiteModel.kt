package ru.dimarzio.rulearn2.tflite

import java.io.File
import org.tensorflow.lite.Interpreter
import ru.dimarzio.rulearn2.viewmodels.PreferencesViewModel.Session

class TFLiteModel(course: String, folder: File, private val provider: FeaturesProvider) : AutoCloseable { // Flyweight
    private val interpreter = Interpreter(File(folder, "$course.tflite"))

    fun predict(
        id: Int,
        rating: Int,
        secondsLapsed: Long,
        typeRepeat: Session?,
        hintsUsed: Int
    ): Float {
        val features = provider.provide(id, rating, secondsLapsed, typeRepeat, hintsUsed)
        val output = FloatArray(1)

        interpreter.run(features, output)

        return output[0] // success_rate
    }

    override fun close() {
        interpreter.close()
    }
}