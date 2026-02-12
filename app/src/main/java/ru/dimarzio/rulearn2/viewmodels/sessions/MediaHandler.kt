package ru.dimarzio.rulearn2.viewmodels.sessions

import java.io.File

open class MediaHandler(private val successor: MediaHandler? = null) { // Chain of Responsibility
    open fun handle(audio: File?, phrase: String, deviceVolume: Int, onHandled: () -> Unit) {
        successor?.handle(audio, phrase, deviceVolume, onHandled)
    }
}