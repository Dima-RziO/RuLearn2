package ru.dimarzio.rulearn2.viewmodels.sessions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ru.dimarzio.rulearn2.models.Word
import ru.dimarzio.rulearn2.utils.percentageFrom

class LearnNewWordsImp(courseWords: Map<Int, Word>, level: String?, limit: Int) : SessionViewModelImp() {
    private val words: MutableMap<Int, Word> =
        (level?.let { courseWords.filter { (_, word) -> word.level == level } } ?: courseWords)
            .filter { (_, word) -> !word.learned && !word.skip }
            .asSequence()
            .take(limit)
            .associateTo(mutableMapOf(), Map.Entry<Int, Word>::toPair)

    private var currentWord by mutableStateOf(null as SessionWord?)
    private val traversed = mutableSetOf<Int>()

    override fun getTraversed(): Map<Int, Word> {
        return traversed.associateWith { id -> words[id]!! }
    }

    override fun getProgress(): Float {
        return words.values.sumOf(Word::rating) percentageFrom words.size * 10
    }

    override fun neglect(id: Int) {
        words.remove(id)
        traversed.remove(id)

        gofnotify()
    }

    override fun emend(word: SessionWord) {
        val realWord = word.getWord()
        words[word.getId()] = realWord

        if (realWord.learned || realWord.skip) {
            traversed.add(word.getId())
        } else {
            traversed.remove(word.getId())
        }

        gofnotify()
    }

    override fun first() {
        val entry = words.entries.firstOrNull()

        if (entry != null) {
            val (id, word) = entry
            currentWord = WordAdapter(id, word)
        }
    }

    override fun next() {
        val candidates = words.filter { (_, word) -> !word.learned && !word.skip }

        val nextEntry = candidates
            .filter { (id, _) -> id != currentWord?.getId() }
            .ifEmpty { candidates }
            .entries.randomOrNull()

        if (nextEntry != null) {
            val (id, word) = nextEntry
            currentWord = WordAdapter(id, word)
        }
    }

    override fun isDone(): Boolean {
        return words.all { (_, word) -> word.learned || word.skip }
    }

    override fun current(): SessionWord? {
        return currentWord
    }
}