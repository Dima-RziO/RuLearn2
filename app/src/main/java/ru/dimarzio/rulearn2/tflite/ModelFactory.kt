package ru.dimarzio.rulearn2.tflite

import android.content.res.AssetManager
import ru.dimarzio.rulearn2.viewmodels.PreferencesViewModel.Companion.settings
import java.io.File

object ModelFactory { // Kotlin Singleton, Flyweight factory
    private val models: MutableMap<String, TFLiteModel> = mutableMapOf()
    private var defaultModel: DefaultModel? = null

    fun getModel(course: String, folder: File, assets: AssetManager): TFLiteModel? {
        if (course !in models) {
            val provider = if (!settings.deprecatedProvider) {
                StandardProvider()
            } else {
                DeprecatedProvider()
            }

            models[course] = if (!settings.calculateSuccessRate) {
                try {
                    CourseModel(course, provider, folder)
                } catch (_: Throwable) {
                    if (settings.defaultModel) {
                        if (defaultModel == null) {
                            defaultModel = DefaultModel(provider, assets)
                        }
                        defaultModel!!
                    } else {
                        return null
                    }
                }
            } else {
                CourseModelProxy(course, provider, folder)
            }
        }

        return models[course]
    }

    fun removeModel(course: String) {
        val model = models.remove(course)
        if (model !is DefaultModel || model !in models.values) {
            model?.close()
        }
    }

    fun isLoaded(course: String): Boolean {
        return models[course]?.isLoaded() == true
    }
}