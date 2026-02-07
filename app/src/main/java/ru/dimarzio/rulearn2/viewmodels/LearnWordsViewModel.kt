package ru.dimarzio.rulearn2.viewmodels

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import ru.dimarzio.rulearn2.models.Word
import ru.dimarzio.rulearn2.utils.percentageFrom
import kotlin.collections.get

class LearnWordsViewModel(courseWords: Map<Int, Word>, level: String?, limit: Int) : ViewModel() {
    private val words =
        (level?.let { courseWords.filter { (_, word) -> word.level == level } } ?: courseWords)
            .filter { (_, word) -> !word.learned && !word.skip }
            .asSequence()
            .take(limit)
            .associateTo(mutableStateMapOf(), Map.Entry<Int, Word>::toPair)

    var currentId by mutableStateOf(words.keys.randomOrNull())
        private set
    var currentWord by mutableStateOf(words[currentId])
        private set
    var hidden by mutableStateOf(true)
        private set

    val ended by derivedStateOf { words.all { (_, word) -> word.learned || word.skip } }
    val learnedWords by derivedStateOf { words.filter { (_, word) -> word.learned || word.skip } }
    val progress by derivedStateOf {
        words.values.sumOf(Word::rating) percentageFrom words.size * 10
    }

    fun updateWord(id: Int, word: Word) {
        words.replace(id, word)
    }

    fun randomizeCurrentWord() {
        val lambda: (Map.Entry<Int, Word>) -> Unit = { (id, word) ->
            if (currentId != id || currentWord != word) {
                hidden = true
            }

            currentId = id
            currentWord = word
        }

        words
            .filter { (id, word) -> !word.learned && !word.skip && currentId != id }
            .ifEmpty { words.filter { (_, word) -> !word.learned && !word.skip } }
            .entries
            .randomOrNull()
            ?.let(lambda)
    }

    fun removeWord(id: Int) {
        words -= id
    }

    fun answer(correct: Boolean, hintsUsed: Int, updateDatabase: (Int, Word) -> Unit) {
        if (currentId != null && currentWord != null) {
            val id = checkNotNull(currentId)
            val word = checkNotNull(currentWord)

            val newAccessed = System.currentTimeMillis()
            val newWord = word.copy(
                accessed = newAccessed,
                rating = if (correct) word.rating + 1 else word.rating - 1,
                secondsLapsed = newAccessed - word.accessed,
                typeRepeat = PreferencesViewModel.Session.LearnNewWords,
                hintsUsed = hintsUsed
            )

            updateDatabase(id, newWord)
            updateWord(id, newWord)
        }
    }

    fun hide(isHidden: Boolean) {
        hidden = isHidden
    }
}