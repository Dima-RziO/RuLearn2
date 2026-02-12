package ru.dimarzio.rulearn2.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration

fun CoroutineScope.timer(duration: Duration, onEnd: () -> Unit) = launch {
    delay(duration)
    onEnd()
}