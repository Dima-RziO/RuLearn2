package ru.dimarzio.rulearn2.tflite

import ru.dimarzio.rulearn2.viewmodels.PreferencesViewModel
import java.io.File

object ModelFactory { // Kotlin Singleton, Flyweight factory
    private val models: MutableMap<String, TFLiteModel> = mutableMapOf()

    fun getModel(course: String, folder: File): TFLiteModel? {
        if (course !in models) {
            val provider = if (!PreferencesViewModel.settings.deprecatedProvider) {
                StandardProvider()
            } else {
                DeprecatedProvider()
            }

            models[course] = if (!PreferencesViewModel.settings.calculateSuccessRate) {
                try {
                    CourseModel(course, provider, folder)
                } catch (_: Throwable) {
                    return null
                }
            } else {
                CourseModelProxy(course, provider, folder)
            }
        }

        return models[course]
    }

    fun removeModel(course: String) {
        models.remove(course)?.close()
    }

    fun isLoaded(course: String): Boolean {
        return models[course]?.isLoaded() == true
    }
}