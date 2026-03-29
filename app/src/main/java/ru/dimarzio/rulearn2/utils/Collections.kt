package ru.dimarzio.rulearn2.utils

import androidx.compose.runtime.mutableStateMapOf

fun <K, V> Map<K, V>.toMutableStateMap() =
    mutableStateMapOf(*entries.map { entry -> entry.toPair() }.toTypedArray())

inline fun <K, V> Map<K, V>.replaceValuesCompat(
    with: (Map.Entry<K, V>) -> V,
    predicate: (Map.Entry<K, V>) -> Boolean
) = mapValues { entry ->
    if (predicate(entry)) {
        with(entry)
    } else {
        entry.value
    }
}

inline fun <K, V> Map<K, V>.replaceValues(
    with: V,
    predicate: (Map.Entry<K, V>) -> Boolean
) = mapValues { entry ->
    if (predicate(entry)) {
        with
    } else {
        entry.value
    }
}

inline fun <K, V> Map<K, V>.replaceKeys(
    with: K,
    predicate: (Map.Entry<K, V>) -> Boolean
) = mapKeys { entry ->
    if (predicate(entry)) {
        with
    } else {
        entry.key
    }
}