package ru.dimarzio.rulearn2.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration

suspend fun <R> context(context: CoroutineContext = Dispatchers.Default, block: () -> R) =
    withContext(context) {
        runCatching<R> { block.invoke() }
    }

fun CoroutineScope.timer(duration: Duration, onEnd: () -> Unit) = launch {
    delay(duration)
    onEnd()
}