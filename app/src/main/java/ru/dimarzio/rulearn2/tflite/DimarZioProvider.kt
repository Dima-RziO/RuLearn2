package ru.dimarzio.rulearn2.tflite

import ru.dimarzio.rulearn2.viewmodels.PreferencesViewModel.Session
import kotlin.math.ln

class DimarZioProvider : FeaturesProvider {
    override fun provide(
        id: Int,
        rating: Int,
        secondsLapsed: Long,
        typeRepeat: Session?,
        hintsUsed: Int
    ) = floatArrayOf(
        if (typeRepeat == Session.LearnNewWords) 1f else 0f, // enc__type_repeat_0
        if (typeRepeat == Session.DifficultWords) 1f else 0f, // enc__type_repeat_1
        if (typeRepeat == Session.TypingReview) 1f else 0f, // enc__type_repeat_2
        if (typeRepeat == Session.GuessingReview) 1f else 0f, // enc__type_repeat_3
        ln(secondsLapsed.toDouble() + 1).toFloat(), // log__s_lapsed
        id.toFloat(), // remainder__id
        rating.toFloat(), // remainder__cur_rating
        hintsUsed.toFloat() // remainder__n_hint
    )
}