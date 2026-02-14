package ru.dimarzio.rulearn2.models

import ru.dimarzio.rulearn2.utils.LocalTime
import ru.dimarzio.rulearn2.utils.months
import ru.dimarzio.rulearn2.utils.normalized
import ru.dimarzio.rulearn2.viewmodels.PreferencesViewModel
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

data class Word( // Prototype
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
    val secondsLapsed: Long,
    val typeRepeat: PreferencesViewModel.Session?,
    val hintsUsed: Int
) {
    private val repeatInterval
        get() = if (!skip) {
            when (rating) {
                10 -> (1 * ratio).hours
                11 -> (5 * ratio).hours
                12 -> (1 * ratio).days
                13 -> (5 * ratio).days
                14 -> (25 * ratio).days
                MAX_RATING -> (4 * ratio).months
                else -> Duration.INFINITE
            }
        } else {
            Duration.INFINITE
        }

    val normalizedName = name.normalized()
    val normalizedTranslation = translation.normalized()

    val randomAudio get() = audios?.filter { it.canRead() && it.isFile }?.randomOrNull()

    val accessedMinutes = LocalTime(accessed).minute
    val accessedHours = LocalTime(accessed).hour

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

    val learned = rating >= 10

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
        hintsUsed = 0
    )
}