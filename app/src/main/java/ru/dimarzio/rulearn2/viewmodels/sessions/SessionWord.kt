package ru.dimarzio.rulearn2.viewmodels.sessions

import ru.dimarzio.rulearn2.models.Word

interface SessionWord { // GoF Prototype
    fun getId(): Int
    fun getWord(): Word

    open fun adaptee(): Any {
        throw RuntimeException("Adaptee is absent.")
    }

    fun clone(word: Word): SessionWord
}