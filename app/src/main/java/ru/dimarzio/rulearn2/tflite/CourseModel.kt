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
            val inputs = mapOf("checkpoint_path" to ckpt.path)
            runCatching {
                interpreter.runSignature(inputs, emptyMap(), "restore")
            }
        }
    }

    override fun predict(contexts: List<Features>): FloatArray? {
        val input = Array(contexts.size) { i -> provider.provide(contexts[i]) }

        val output = Array(contexts.size) { FloatArray(1) }
        runCatching { interpreter.run(input, output) }.onFailure { return null }

        return FloatArray(contexts.size) { i -> output[i][0].takeUnless { it.isNaN() } ?: 1f }
    }

    override fun predict(context: Features): Float? {
        return predict(listOf(context))?.single()
    }

    override fun train(contexts: List<Features>): Boolean {
        val result = runCatching {
            val filtered = contexts.filter { context -> context.repetitions > 0 }
            val batches = filtered.shuffled().chunked(32).map { batch ->
                mapOf(
                    "x" to Array(batch.size) { i -> provider.provide(batch[i]) },
                    "y" to Array(batch.size) { i ->
                        floatArrayOf(batch[i].correctAnswers.toFloat() / batch[i].repetitions)
                    }
                )
            }

            for (epoch in 1..100) {
                for (batchInputs in batches) {
                    interpreter.runSignature(batchInputs, emptyMap(), "train")
                }
            }

            val saveInputs = mapOf("checkpoint_path" to ckpt.path)
            interpreter.runSignature(saveInputs, emptyMap(), "save")
        }

        return result.isSuccess
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