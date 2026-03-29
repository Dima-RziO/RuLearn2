package ru.dimarzio.rulearn2.viewmodels

open class Subject {
    private val observers = mutableListOf<Observer>()

    open fun attach(o: Observer) {
        observers.add(o)
    }

    open fun detach(o: Observer) {
        observers.remove(o)
    }

    open fun copy(to: Subject) {
        observers.forEach { observer ->
            to.attach(observer)
        }
    }

    open fun gofnotify() {
        observers.forEach { observer ->
            observer.update(this)
        }
    }
}