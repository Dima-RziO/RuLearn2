package ru.dimarzio.rulearn2.utils

import org.apache.commons.lang3.StringUtils
import java.text.Normalizer

val String.extension get() = substringAfterLast('.')

infix fun Int.percentageFrom(number: Int) = if (this == 0) {
    0f
} else {
    ((this / number.toFloat()) * 100).coerceIn(0f..100f)
}

fun String.normalized(): String = StringUtils.stripAccents(
    Normalizer
        .normalize(this, Normalizer.Form.NFKD)
        .replace("\\p{Mn}+".toRegex(), "")
        .trim()
        .lowercase()
)

inline fun <T> T.whether(condition: Boolean, block: T.() -> T) = if (condition) {
    block()
} else {
    this
}