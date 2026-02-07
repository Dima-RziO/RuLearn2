package ru.dimarzio.rulearn2.viewmodels

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.ViewModel
import ru.dimarzio.rulearn2.models.Word
import ru.dimarzio.rulearn2.utils.percentageFrom
import kotlin.collections.get

// Yeah, this one is kinda sophisticated..
class DifficultWordsViewModel(
    courseWords: Map<Int, Word>,
    level: String?,
    limit: Int
) : ViewModel() {
    private val words: SnapshotStateMap<Int, Pair<Word, DifficultState>> =
        (level?.let { courseWords.filter { (_, word) -> word.level == level } } ?: courseWords)
            .filter { (_, word) -> word.isDifficult }
            .entries // There is no take(limit) in map
            .take(limit)
            .associateTo(mutableStateMapOf()) { (id, word) ->
                Pair(id, word to DifficultState.None)
            }

    // Do NOT make currentId, currentWord, currentState derived.
    var currentId by mutableStateOf(words.keys.randomOrNull())
    var currentWord by mutableStateOf(words[currentId]?.first)
    var currentState by mutableStateOf(words[currentId]?.second)
    var hidden by mutableStateOf(true)

    val ended by derivedStateOf { words.none { (_, pair) -> pair.first.isDifficult } }
    val memorizedWords by derivedStateOf {
        words.mapValues { (_, pair) -> pair.first }.filterNot { (_, word) -> word.isDifficult }
    }
    val progress by derivedStateOf {
        words.values
            .sumOf { (_, state) -> state.ordinal }
            .percentageFrom(words.size * DifficultState.Memorized.ordinal)
    }

    enum class DifficultState {
        None,
        Guessing,
        Typing,
        Memorized
    }

    fun updateWord(id: Int, word: Word, state: DifficultState) {
        words.replace(id, word to state)
    }

    fun randomizeCurrentWord() {
        val lambda: (Map.Entry<Int, Pair<Word, DifficultState>>) -> Unit = { (id, pair) ->
            val (word, state) = pair

            if (currentId != id || currentWord != word || currentState != state) {
                hidden = true
            }

            currentId = id
            currentWord = word
            currentState = state
        }

        words
            .filter { (id, pair) -> pair.first.isDifficult && id != currentId }
            .ifEmpty { words.filter { (_, pair) -> pair.first.isDifficult } }
            .entries
            .randomOrNull()
            ?.let(lambda)
    }

    fun removeWord(id: Int) {
        words -= id
    }

    fun answer(correct: Boolean, hintsUsed: Int, updateDatabase: (Int, Word) -> Unit) {
        if (currentId != null && currentWord != null && currentState != null) {
            val id = checkNotNull(currentId)
            val word = checkNotNull(currentWord)
            val state = checkNotNull(currentState)

            val newOrdinal = if (correct) state.ordinal.inc() else state.ordinal.dec()

            if (state == DifficultState.Typing && correct) {
                val newAccessed = System.currentTimeMillis()
                val newWord = word.copy(
                    accessed = if (word.isRepeat) {
                        newAccessed
                    } else {
                        word.accessed
                    },
                    difficult = false,
                    rating = if (word.isRepeat) {
                        word.rating.inc().coerceIn(0..Word.MAX_RATING)
                    } else {
                        word.rating
                    },
                    repetitions = if (word.isRepeat) {
                        word.repetitions + 1
                    } else {
                        word.repetitions
                    },
                    correctAnswers = if (word.isRepeat) {
                        word.correctAnswers + 1
                    } else {
                        word.correctAnswers
                    },
                    secondsLapsed = if (word.isRepeat) {
                        newAccessed - word.accessed
                    } else {
                        word.secondsLapsed
                    },
                    typeRepeat = if (word.isRepeat) {
                        PreferencesViewModel.Session.DifficultWords
                    } else {
                        word.typeRepeat
                    },
                    hintsUsed = if (word.isRepeat) {
                        hintsUsed
                    } else {
                        word.hintsUsed
                    }
                )

                updateWord(id, newWord, enumValues<DifficultState>()[newOrdinal.coerceAtLeast(0)])
                updateDatabase(id, newWord)
            } else {
                updateWord(id, word, enumValues<DifficultState>()[newOrdinal.coerceAtLeast(0)])
            }
        }
    }

    fun hide(isHidden: Boolean) {
        hidden = isHidden
    }
}