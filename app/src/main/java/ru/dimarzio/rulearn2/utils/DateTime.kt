package ru.dimarzio.rulearn2.utils

import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

val Double.months get() = times(30).days

val LocalDateTime.millis
    get() = atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

fun Duration.format() = toComponents { days, hours, minutes, seconds, _ ->
    when {
        days != 0L -> "$days days"
        hours != 0 -> "$hours hours"
        minutes != 0 -> "$minutes minutes"
        else -> "$seconds seconds"
    }
}

fun LocalTime(millis: Long): LocalTime = Instant
    .ofEpochMilli(millis)
    .atZone(ZoneId.systemDefault())
    .toLocalTime()

fun LocalDateTime(millis: Long): LocalDateTime = Instant
    .ofEpochMilli(millis)
    .atZone(ZoneId.systemDefault())
    .toLocalDateTime()