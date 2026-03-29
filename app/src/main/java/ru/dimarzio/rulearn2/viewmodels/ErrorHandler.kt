package ru.dimarzio.rulearn2.viewmodels

interface ErrorHandler {
    fun onErrorHandled(exception: Throwable?)
    fun onMessageReceived(message: String)
}