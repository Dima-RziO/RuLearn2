package ru.dimarzio.rulearn2.tflite

import org.tensorflow.lite.Interpreter
import java.io.File

class CourseModel(
    private val course: String,
    private val provider: FeaturesProvider,
    folder: File
) : TFLiteModel {
    private val interpreter = Interpreter(File(folder, "$course.tflite"))
    private val ckpt = File(folder, "$course.ckpt")

    init {
        if (ckpt.exists()) { // Restoring weights.
            val input = mapOf("checkpoint_path" to ckpt.path)
            interpreter.runSignature(input, emptyMap(), "restore")
        }
    }

    override fun predict(contexts: List<Features>): FloatArray {
        val input = Array(contexts.size) { i -> provider.provide(contexts[i]) }

        val output = Array(contexts.size) { FloatArray(1) }
        interpreter.run(input, output)

        return FloatArray(contexts.size) { i -> output[i][0].takeUnless { it.isNaN() } ?: 1f }
    }

    override fun predict(context: Features): Float {
        return predict(listOf(context)).single()
    }

    override fun train(contexts: List<Features>) {
        val saveInputs = mapOf("checkpoint_path" to ckpt.path)
        val trainInputs = mapOf(
            "x" to Array(contexts.size) { i -> provider.provide(contexts[i]) },
            "y" to Array(contexts.size) { i ->
                floatArrayOf(contexts[i].correctAnswers.toFloat() / contexts[i].repetitions)
            }
        )

        interpreter.runSignature(trainInputs, emptyMap(), "train")
        interpreter.runSignature(saveInputs, emptyMap(), "save")
    }

    override fun getName(): String {
        return "$course.tflite"
    }

    override fun isLoaded(): Boolean {
        return true
    }

    override fun close() {
        interpreter.close()
    }
}