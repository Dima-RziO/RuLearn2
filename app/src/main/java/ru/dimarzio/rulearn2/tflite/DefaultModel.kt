package ru.dimarzio.rulearn2.tflite

import android.content.res.AssetManager
import org.tensorflow.lite.Interpreter
import ru.dimarzio.rulearn2.viewmodels.PreferencesViewModel
import java.io.File
import java.io.FileInputStream
import java.nio.channels.FileChannel

class DefaultModel(private val provider: FeaturesProvider, assets: AssetManager) : TFLiteModel {
    private val interpreter: Interpreter

    init {
        val fileDescriptor = assets.openFd(getName())

        FileInputStream(fileDescriptor.fileDescriptor).use { inputStream ->
            val fileChannel = inputStream.channel
            val buffer = fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                fileDescriptor.startOffset,
                fileDescriptor.declaredLength
            )

            interpreter = Interpreter(buffer)
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
        }

        return result.isSuccess
    }

    fun save(to: File): Boolean {
        val result = runCatching {
            val inputs = mapOf("checkpoint_path" to to.path)
            interpreter.runSignature(inputs, emptyMap(), "save")
        }

        return result.isSuccess
    }

    override fun getName(): String {
        return if (!PreferencesViewModel.settings.deprecatedProvider) {
            "default.tflite"
        } else {
            "default_deprecated.tflite"
        }
    }

    override fun isLoaded(): Boolean {
        return true
    }

    override fun close() {
        interpreter.close()
    }
}