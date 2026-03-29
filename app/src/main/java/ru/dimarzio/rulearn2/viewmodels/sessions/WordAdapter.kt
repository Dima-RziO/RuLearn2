package ru.dimarzio.rulearn2.viewmodels.sessions

import ru.dimarzio.rulearn2.models.Word

class WordAdapter(private val id: Int, private val word: Word) : SessionWord { // GoF Adapter
    override fun getId(): Int {
        return id
    }

    override fun getWord(): Word {
        return word
    }

    override fun clone(word: Word): SessionWord {
        return WordAdapter(id, word)
    }
}