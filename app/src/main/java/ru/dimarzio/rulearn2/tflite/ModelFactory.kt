package ru.dimarzio.rulearn2.tflite

import java.io.File

object ModelFactory { // Singleton
    private val models: MutableMap<String, TFLiteModel> = mutableMapOf()

    fun getModel(course: String, folder: File): TFLiteModel {
        return models.getOrPut(course) {
            TFLiteModel(course, folder, DimarZioProvider())
        }
    }

    fun removeModel(course: String) {
        models.remove(course)?.close()
    }
}