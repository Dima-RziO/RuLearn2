package ru.dimarzio.rulearn2.models

import ru.dimarzio.rulearn2.tflite.Features
import ru.dimarzio.rulearn2.utils.normalized
import ru.dimarzio.rulearn2.viewmodels.PreferencesViewModel.Session
import java.io.File
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

data class Word( // Kotlin Prototype
    // Map<K, V> value
    val name: String,
    val translation: String,
    val audios: List<File>?,
    val level: String,
    val accessed: Long,
    val difficult: Boolean,
    val skip: Boolean,
    val rating: Int,
    val repetitions: Int, // n_repeat
    val correctAnswers: Int, // sum_correct
    val secondsLapsed: Long, // s_lapsed
    val typeRepeat: Session?,
    val hintsUsed: Int, // n_hint
    val successRate: Float
) {
    private val Double.months get() = times(30).days

    private val repeatInterval
        get() = if (!skip) {
            val rate = successRate.toDouble()
            when (rating) {
                10 -> (1 * rate).hours
                11 -> (5 * rate).hours
                12 -> (1 * rate).days
                13 -> (5 * rate).days
                14 -> (25 * rate).days
                MAX_RATING -> (4 * rate).months
                else -> Duration.INFINITE
            }
        } else {
            Duration.INFINITE
        }

    val normalizedName = name.normalized()

    val lastlyRepeated
        get() = if (accessed != 0L) {
            (System.currentTimeMillis() - accessed).milliseconds
        } else {
            Duration.INFINITE
        }

    val repeatDuration
        get() = if (repeatInterval.isFinite() || lastlyRepeated.isFinite()) {
            (repeatInterval - lastlyRepeated).coerceAtLeast(Duration.ZERO)
        } else {
            Duration.INFINITE
        }


    val isRepeat get() = repeatDuration == Duration.ZERO
    val isDifficult = difficult && rating >= 10 && !skip
    val learned = rating >= 10 && !skip

    // NaN or POSITIVE_INFINITY cannot be returned
    val ratio = if (repetitions != 0 && correctAnswers != 0) {
        correctAnswers.toDouble() / repetitions
    } else {
        1.0
    }

    companion object {
        const val MAX_RATING = 15
    }

    // New word
    constructor(level: String) : this(
        name = "",
        translation = "",
        audios = null,
        level = level,
        accessed = 0L,
        skip = false,
        difficult = false,
        rating = 0,
        repetitions = 0,
        correctAnswers = 0,
        secondsLapsed = 0L,
        typeRepeat = null,
        hintsUsed = 0,
        successRate = 1f
    )

    // ..,
    constructor(accessed: Long, skip: Boolean, rating: Int, successRate: Float) : this(
        name = "",
        translation = "",
        audios = null,
        level = "",
        accessed = accessed,
        skip = skip,
        difficult = false,
        rating = rating,
        repetitions = 0,
        correctAnswers = 0,
        secondsLapsed = 0L,
        typeRepeat = null,
        hintsUsed = 0,
        successRate = successRate
    )

    fun toFeatures(id: Int) = Features(
        id = id,
        repetitions = repetitions,
        correctAnswers = correctAnswers,
        rating = rating,
        secondsLapsed = secondsLapsed,
        typeRepeat = typeRepeat,
        hintsUsed = hintsUsed
    )
}