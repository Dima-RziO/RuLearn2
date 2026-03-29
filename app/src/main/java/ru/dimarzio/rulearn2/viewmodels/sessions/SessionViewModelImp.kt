package ru.dimarzio.rulearn2.viewmodels.sessions

import ru.dimarzio.rulearn2.models.Word
import ru.dimarzio.rulearn2.viewmodels.Subject

abstract class SessionViewModelImp : Iterator<SessionWord>, Subject() { // GoF Bridge
    abstract fun getTraversed(): Map<Int, Word>
    abstract fun getProgress(): Float

    abstract fun neglect(id: Int)
    abstract fun emend(word: SessionWord)
}