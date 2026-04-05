package ru.dimarzio.rulearn2.tflite

import ru.dimarzio.rulearn2.viewmodels.PreferencesViewModel.Session
import kotlin.math.ln

class StandardProvider : FeaturesProvider {
    override fun provide(features: Features) = floatArrayOf(
        if (features.typeRepeat == Session.LearnNewWords) 1f else 0f, // enc__type_repeat_0
        if (features.typeRepeat == Session.DifficultWords) 1f else 0f, // enc__type_repeat_1
        if (features.typeRepeat == Session.TypingReview) 1f else 0f, // enc__type_repeat_2
        if (features.typeRepeat == Session.GuessingReview) 1f else 0f, // enc__type_repeat_3
        ln(features.secondsLapsed.toDouble() + 1).toFloat(), // log__s_lapsed
        features.id.toFloat(), // remainder__id
        features.rating.toFloat(), // remainder__cur_rating
        features.hintsFraction // remainder__hint_frac
    )
}