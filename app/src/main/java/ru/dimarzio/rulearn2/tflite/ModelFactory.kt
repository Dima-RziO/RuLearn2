package ru.dimarzio.rulearn2.tflite

import java.io.File

object ModelFactory { // Kotlin Singleton, Flyweight factory
    private val models: MutableMap<String, CourseModel> = mutableMapOf()

    fun getModel(course: String, folder: File): TFLiteModel? {
        if (course !in models) {
            models[course] = try {
                CourseModel(course, StandardProvider(), folder)
            } catch (_: Throwable) {
                return null
            }
        }

        return models[course]
    }

    fun removeModel(course: String) {
        models.remove(course)?.close()
    }
    
    fun isLoaded(course: String): Boolean {
        return course in models
    }
}