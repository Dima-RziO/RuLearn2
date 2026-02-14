package ru.dimarzio.rulearn2.viewmodels.sessions.media

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class FallbackHandler(private val scope: CoroutineScope) : MediaHandler() {
    private fun timer(duration: Duration, onEnd: () -> Unit) = scope.launch {
        delay(duration)
        onEnd()
    }

    override fun handle(deviceVolume: Int, onHandled: () -> Unit) {
        timer(1.5.seconds, onHandled)
    }
}