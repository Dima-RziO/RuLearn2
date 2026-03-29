package ru.dimarzio.rulearn2.viewmodels.sessions

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import ru.dimarzio.rulearn2.models.Word
import ru.dimarzio.rulearn2.viewmodels.Observer
import ru.dimarzio.rulearn2.viewmodels.Subject

// Abstraction
open class SessionViewModel(protected val iterator: SessionViewModelImp) : ViewModel(), Observer {
    protected var version by mutableIntStateOf(0)

    open var currentWord by mutableStateOf(iterator.current())
        protected set

    open val progress by derivedStateOf { iterator.getProgress().also { version } }
    open val rote by derivedStateOf { iterator.getTraversed().also { version } }
    open val ended by derivedStateOf { iterator.isDone().also { version } }

    init {
        iterator.first()
        currentWord = iterator.current()

        iterator.attach(this)
    }

    open fun next() {
        if (!iterator.isDone()) {
            iterator.next()
        }

        currentWord = iterator.current()
    }

    open fun makeWord(prototype: SessionWord, correct: Boolean, hintsUsed: Int): SessionWord { // GoF Factory Method
        return prototype.clone(prototype.getWord())
    }

    open fun answer(correct: Boolean, hintsUsed: Int): Word? { // GoF Template Method
        val word = iterator.current()
        if (word != null) {
            val word = makeWord(word, correct, hintsUsed)

            iterator.emend(word)
            return word.getWord()
        }

        return null
    }

    open fun removeWord(id: Int) {
        iterator.neglect(id)
    }

    open fun updateWord(word: SessionWord) {
        iterator.emend(word)
    }

    override fun update(subject: Subject) {
        if (subject === iterator) {
            version++
        }
    }
}