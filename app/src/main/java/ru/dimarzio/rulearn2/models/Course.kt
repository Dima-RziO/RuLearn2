package ru.dimarzio.rulearn2.models

import java.io.File

data class Course( // Prototype
    val icon: File?,
    val repeat: Int,
    val learned: Int,
    val total: Int
)