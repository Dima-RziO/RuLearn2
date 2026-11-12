package ru.dimarzio.rulearn2.viewmodels.sessions.difficult

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ru.dimarzio.rulearn2.models.Word
import ru.dimarzio.rulearn2.utils.percentageFrom
import ru.dimarzio.rulearn2.viewmodels.sessions.SessionViewModelImp
import ru.dimarzio.rulearn2.viewmodels.sessions.SessionWord

class DifficultWordsImp(
    courseWords: Map<Int, Word>,
    level: String?,
    limit: Int
) : SessionViewModelImp() {
    private val words: MutableMap<Int, DifficultWord> =
        (level?.let { courseWords.filter { (_, word) -> word.level == level } } ?: courseWords)
            .entries // There is no take(limit) in map
            .filter { (_, word) -> word.isDifficult }
            .take(limit)
            .associate { (id, word) -> id to DifficultWord(id, word) }
            .toMutableMap()

    private var currentWord by mutableStateOf(null as DifficultWordAdapter?)
    private val traversed = mutableSetOf<Int>()

    override fun getTraversed(): Map<Int, Word> {
        return traversed.associateWith { id -> words[id]?.word!! }
    }

    override fun getProgress(): Float {
        return words.values
            .sumOf(DifficultWord::getLevel)
            .percentageFrom(words.size * MemorizedState.getLevel())
    }

    override fun neglect(id: Int) {
        words.remove(id)
        traversed.remove(id)

        gofnotify()
    }

    override fun emend(word: SessionWord) {
        val realWord = word.getWord()

        words[word.getId()]?.word = realWord
        words[word.getId()]?.changeState((word.adaptee() as DifficultWord).state)

        if (!realWord.isDifficult) {
            traversed.add(word.getId())
        } else {
            traversed.remove(word.getId())
        }

        gofnotify()
    }

    override fun first() {
        val word = words.values.firstOrNull()

        if (word != null) {
            currentWord = DifficultWordAdapter(word)
        }
    }

    override fun next() {
        val word = words.values
            .filter { word -> word.word.isDifficult && word.id != currentWord?.getId() }
            .ifEmpty { words.values.filter { word -> word.word.isDifficult } }
            .randomOrNull()

        if (word != null) {
            currentWord = DifficultWordAdapter(word)
        }
    }

    override fun isDone(): Boolean {
        return words.none { (_, word) -> word.word.isDifficult }
    }

    override fun current(): SessionWord? {
        return currentWord
    }
}