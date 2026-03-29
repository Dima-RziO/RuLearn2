package ru.dimarzio.rulearn2.viewmodels.sessions

import ru.dimarzio.rulearn2.models.Word
import ru.dimarzio.rulearn2.utils.percentageFrom

class ReviewImp(courseWords: Map<Int, Word>, level: String?, limit: Int) : SessionViewModelImp() {
    private val words: MutableMap<Int, Word> =
        (level?.let { courseWords.filter { (_, word) -> word.level == level } } ?: courseWords)
            .filter { (_, word) -> word.isRepeat }.entries
            .shuffled()
            .sortedBy { (_, word) -> word.rating }
            .take(limit)
            .associateTo(mutableMapOf(), Map.Entry<Int, Word>::toPair)

    private var currentWord = null as SessionWord?

    override fun getProgress(): Float {
        return words.count { (_, word) -> !word.isRepeat } percentageFrom words.size
    }

    override fun getTraversed(): Map<Int, Word> {
        return words.filterNot { (_, word) -> word.isRepeat }
    }

    override fun neglect(id: Int) {
        words.remove(id)
        gofnotify()
    }

    override fun emend(word: SessionWord) {
        words[word.getId()] = word.getWord()
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
        val entry = words.entries
            .shuffled()
            .sortedBy { (_, word) -> word.rating }
            .find { (_, word) -> word.isRepeat }

        if (entry != null) {
            val (id, word) = entry
            currentWord = WordAdapter(id, word)
        }
    }

    override fun isDone(): Boolean {
        return words.none { (_, word) -> word.isRepeat }
    }

    override fun current(): SessionWord? {
        return currentWord
    }
}