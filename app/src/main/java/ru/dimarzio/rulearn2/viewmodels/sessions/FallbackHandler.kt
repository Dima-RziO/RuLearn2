package ru.dimarzio.rulearn2.viewmodels.sessions

import kotlinx.coroutines.CoroutineScope
import kotlin.time.Duration
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.io.File
import kotlin.time.Duration.Companion.seconds

class FallbackHandler(private val scope: CoroutineScope) : MediaHandler() {
    private fun timer(duration: Duration, onEnd: () -> Unit) = scope.launch {
        delay(duration)
        onEnd()
    }

    override fun handle(audio: File?, phrase: String, deviceVolume: Int, onHandled: () -> Unit) {
        timer(1.5.seconds, onHandled)
    }
}