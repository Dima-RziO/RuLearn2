package ru.dimarzio.rulearn2.tflite

open class ModelWrapper(private val model: TFLiteModel) : TFLiteModel { // Decorator
    override fun predict(contexts: List<Features>): FloatArray {
        return model.predict(contexts)
    }

    override fun predict(context: Features): Float {
        return model.predict(context)
    }

    override fun getName(): String {
        return model.getName()
    }

    override fun close() {
        model.close()
    }
}