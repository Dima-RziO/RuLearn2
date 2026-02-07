package ru.dimarzio.rulearn2.models

data class Level(
    val total: Int,
    val learned: Int,
    val toRepeat: Int,
    val difficult: Int
) {
    operator fun plus(level: Level) = copy(
        total = level.total + total,
        learned = level.learned + learned,
        toRepeat = level.toRepeat + toRepeat,
        difficult = level.difficult + difficult
    )
}