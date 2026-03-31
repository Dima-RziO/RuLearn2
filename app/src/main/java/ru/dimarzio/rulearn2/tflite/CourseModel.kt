package ru.dimarzio.rulearn2.tflite

import org.tensorflow.lite.Interpreter
import java.io.File

class CourseModel(
    private val course: String,
    private val provider: FeaturesProvider,
    folder: File
) : TFLiteModel {
    private val interpreter = Interpreter(File(folder, "$course.tflite"))

    override fun predict(contexts: List<Features>): FloatArray {
        val input = Array(contexts.size) { i -> provider.provide(contexts[i]) }

        val output = Array(contexts.size) { FloatArray(1) }
        interpreter.run(input, output)

        return FloatArray(contexts.size) { i -> output[i][0] }
    }

    override fun predict(context: Features): Float {
        return predict(listOf(context)).single()
    }

    override fun getName(): String {
        return "$course.tflite"
    }

    override fun close() {
        interpreter.close()
    }
}