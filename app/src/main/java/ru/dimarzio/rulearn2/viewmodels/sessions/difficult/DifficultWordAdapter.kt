package ru.dimarzio.rulearn2.viewmodels.sessions.difficult

import ru.dimarzio.rulearn2.models.Word
import ru.dimarzio.rulearn2.viewmodels.sessions.SessionWord

class DifficultWordAdapter(private val difficultWord: DifficultWord) : SessionWord { // GoF Adapter
    override fun getId(): Int {
        return difficultWord.id
    }

    override fun getWord(): Word {
        return difficultWord.word
    }

    override fun adaptee(): Any {
        return difficultWord
    }

    override fun clone(word: Word): SessionWord {
        return DifficultWordAdapter(difficultWord)
    }
}