package ru.dimarzio.rulearn2.viewmodels

import ru.dimarzio.rulearn2.viewmodels.Subject

interface Observer { // GoF Observer
    fun update(subject: Subject)
}