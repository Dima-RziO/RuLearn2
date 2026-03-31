package ru.dimarzio.rulearn2.viewmodels.sessions

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import ru.dimarzio.rulearn2.models.Word
import ru.dimarzio.rulearn2.routes.SessionRoutes
import ru.dimarzio.rulearn2.viewmodels.Observer
import ru.dimarzio.rulearn2.viewmodels.Subject

// Abstraction
open class SessionViewModel(protected val imp: SessionViewModelImp) : ViewModel(), Observer {
    protected var version by mutableIntStateOf(0)
    protected val _navigationEvents = Channel<Pair<String, SessionWord>>(Channel.BUFFERED)

    val navigationEvents = _navigationEvents.receiveAsFlow()

    open var currentWord by mutableStateOf(imp.current())
        protected set

    open val progress by derivedStateOf { imp.getProgress().also { version } }
    open val rote by derivedStateOf { imp.getTraversed().also { version } }
    open val ended by derivedStateOf { imp.isDone().also { version } }

    init {
        imp.first()
        currentWord = imp.current()

        imp.attach(this)
    }

    open fun next() {
        if (!imp.isDone()) {
            imp.next()
        }

        val word = imp.current()
        currentWord = word

        if (word != null) {
            makeRoute(word)?.route?.let { route -> _navigationEvents.trySend(route to word) }
        }
    }

    open fun makeRoute(word: SessionWord): SessionRoutes? {
        return null
    }

    open fun makeWord(
        prototype: SessionWord,
        correct: Boolean,
        hintsUsed: Int
    ): SessionWord { // GoF Factory Method
        return prototype.clone(prototype.getWord())
    }

    open fun answer(correct: Boolean, hintsUsed: Int): Word? { // GoF Template Method
        val word = imp.current()
        if (word != null) {
            val word = makeWord(word, correct, hintsUsed)

            imp.emend(word)
            return word.getWord()
        }

        return null
    }

    open fun removeWord(id: Int) {
        imp.neglect(id)
    }

    open fun updateWord(word: SessionWord) {
        imp.emend(word)
    }

    override fun update(subject: Subject) {
        if (subject === imp) {
            version++
        }
    }
}