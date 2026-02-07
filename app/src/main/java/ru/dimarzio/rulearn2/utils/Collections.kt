package ru.dimarzio.rulearn2.utils

import androidx.compose.runtime.mutableStateMapOf

fun <K, V> Map<K, V>.toMutableStateMap() =
    mutableStateMapOf(*entries.map { entry -> entry.toPair() }.toTypedArray())

// Due to bad performance, normalization was removed.
inline fun <T> Collection<T>.sortedBySimilarity(
    similarBy: String,
    crossinline getComparable: (T) -> String
) = sortedWith(
    Comparator { a, b ->
        similarBy.foldRight(similarBy) { c, acc ->
            if (acc in getComparable(b)) {
                return@Comparator 1
            } else if (acc in getComparable(a)) {
                return@Comparator -1
            }
            acc.substringBeforeLast(c)
        }
        return@Comparator 0
    }
)

inline fun <T> Collection<T>.sortedBySimilarity2(
    similarBy: String,
    crossinline getComparable: (T) -> String
) = sortedByDescending { item ->
    val comparable = getComparable(item)
    val matching = similarBy.foldRight(similarBy) { c, acc ->
        if (acc in comparable) {
            return@sortedByDescending acc.length
        }
        acc.substringBeforeLast(c)
    }

    return@sortedByDescending matching.length
}

inline fun <K, V> Map<K, V>.maxOfOrDefault(default: Int, selector: (Map.Entry<K, V>) -> Int): Int {
    return maxOfOrNull(selector) ?: default
}

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

fun <K, V, T> Map<K, V>.groupBy(keySelector: (Map.Entry<K, V>) -> T) = entries
    .groupBy(keySelector)
    .mapValues { (_, list) -> list.associate(Map.Entry<K, V>::toPair) }
    .toMap()