package ru.dimarzio.rulearn2.viewmodels.sessions

interface Iterator<out T> { // GoF Iterator
    fun first()
    fun next()
    fun isDone(): Boolean
    fun current(): T?
}