package ru.dimarzio.rulearn2.tflite

import java.io.File

class CourseModelProxy( // GoF Proxy
    private val course: String,
    private val provider: FeaturesProvider,
    folder: File
) : TFLiteModel {
    val model = runCatching { CourseModel(course, provider, folder) }.getOrNull()

    override fun predict(contexts: List<Features>): FloatArray {
        return if (model != null) {
            model.predict(contexts)
        } else {
            val labels = contexts.map { context ->
                if (context.repetitions != 0 && context.correctAnswers != 0) {
                    context.correctAnswers.toFloat() / context.repetitions
                } else {
                    1f
                }
            }

            labels.toFloatArray()
        }
    }

    override fun predict(context: Features): Float {
        return if (model != null) {
            model.predict(context)
        } else if (context.repetitions != 0 && context.correctAnswers != 0) {
            context.correctAnswers.toFloat() / context.repetitions
        } else {
            1f
        }
    }

    override fun getName(): String {
        return model?.getName().toString()
    }

    override fun close() {
        model?.close()
    }
}