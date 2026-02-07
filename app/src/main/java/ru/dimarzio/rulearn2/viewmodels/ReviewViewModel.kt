package ru.dimarzio.rulearn2.viewmodels

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import ru.dimarzio.rulearn2.models.Word
import ru.dimarzio.rulearn2.utils.percentageFrom

class ReviewViewModel(
    courseWords: Map<Int, Word>,
    level: String?,
    limit: Int
) : ViewModel() {
    private val words =
        (level?.let { courseWords.filter { (_, word) -> word.level == level } } ?: courseWords)
            .filter { (_, word) -> word.isRepeat }
            .entries
            .shuffled()
            .take(limit)
            .associateTo(mutableStateMapOf(), Map.Entry<Int, Word>::toPair)

    private val random
        get() = words.entries
            .shuffled()
            .sortedBy { (_, word) -> word.rating }
            .find { (_, word) -> word.isRepeat }

    var currentId by mutableStateOf(random?.key)
        private set
    var currentWord by mutableStateOf(words[currentId])
        private set
    var correctAnswers by mutableIntStateOf(0)
        private set
    var hidden by mutableStateOf(true) // 14.12.25
        private set

    val repeatedWords by derivedStateOf { words.filterNot { (_, word) -> word.isRepeat } }
    val ended by derivedStateOf { words.none { (_, word) -> word.isRepeat } }
    val progress by derivedStateOf {
        words.count { (_, word) -> !word.isRepeat } percentageFrom words.size
    }

    fun updateWord(id: Int, word: Word) {
        words.replace(id, word)
    }

    fun reselectCurrentWord() {
        random?.let { (id, word) ->
            if (currentId != id || currentWord != word) {
                hidden = true
            }

            currentId = id
            currentWord = word
        }
    }

    fun removeWord(id: Int) {
        words -= id
    }

    fun answer(
        correct: Boolean,
        markDifficult: Boolean,
        typeRepeat: PreferencesViewModel.Session,
        hintsUsed: Int,
        updateDatabase: (Int, Word) -> Unit
    ) {
        if (currentId != null && currentWord != null) {
            val id = checkNotNull(currentId)
            val word = checkNotNull(currentWord)

            val newAccessed = System.currentTimeMillis()
            val newWord = word
                .copy(
                    accessed = newAccessed,
                    rating = if (correct) {
                        word.rating.inc().coerceIn(0..Word.MAX_RATING)
                    } else {
                        10
                    },
                    repetitions = word.repetitions + 1,
                    correctAnswers = if (correct) {
                        word.correctAnswers + 1
                    } else {
                        word.correctAnswers
                    },
                    secondsLapsed = newAccessed - word.accessed,
                    typeRepeat = typeRepeat,
                    hintsUsed = hintsUsed
                )
                .run { copy(difficult = markDifficult && !correct && ratio <= 0.75f) }

            currentWord = newWord

            updateWord(id, newWord)
            updateDatabase(id, newWord)

            if (correct) {
                correctAnswers++
            }
        }
    }

    fun hide(isHidden: Boolean) {
        hidden = isHidden
    }
}