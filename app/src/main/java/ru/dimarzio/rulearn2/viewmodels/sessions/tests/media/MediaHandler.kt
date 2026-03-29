package ru.dimarzio.rulearn2.viewmodels.sessions.tests.media

open class MediaHandler(private val successor: MediaHandler? = null) { // GoF Chain of Responsibility
    open fun handle(deviceVolume: Int, onHandled: () -> Unit = {}) {
        successor?.handle(deviceVolume, onHandled)
    }
}