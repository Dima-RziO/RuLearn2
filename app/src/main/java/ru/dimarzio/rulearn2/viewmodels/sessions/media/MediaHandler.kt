package ru.dimarzio.rulearn2.viewmodels.sessions.media

open class MediaHandler(private val successor: MediaHandler? = null) { // Chain of Responsibility
    open fun handle(deviceVolume: Int, onHandled: () -> Unit = {}) {
        successor?.handle(deviceVolume, onHandled)
    }
}