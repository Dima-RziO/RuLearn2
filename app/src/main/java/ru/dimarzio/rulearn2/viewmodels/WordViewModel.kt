package ru.dimarzio.rulearn2.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import ru.dimarzio.rulearn2.models.Word
import ru.dimarzio.rulearn2.utils.LocalDateTime
import ru.dimarzio.rulearn2.utils.maxOfOrDefault
import ru.dimarzio.rulearn2.utils.millis

class WordViewModel(
    courseWords: Map<Int, Word>,
    id: Int?,
    level: String? // Pass null if you are sure that word exists
) : ViewModel() {
    var newId by mutableIntStateOf(
        id ?: courseWords.maxOfOrDefault(0, Map.Entry<Int, Word>::key).inc()
    )
        private set

    var newWord by mutableStateOf(courseWords.getOrDefault(id, Word(level.toString())))
        private set

    var modified by mutableStateOf(false)
        private set

    fun updateWord(word: Word) {
        modified = true
        newWord = word
    }

    fun updateId(id: Int) {
        modified = false
        newId = id
    }

    fun pickAccessed(dateMillis: Long, minutes: Int, hours: Int) {
        /*
        val hours = (hour - 3).hours // 3 is subtracted to compensate dateMillis.
        newWord = newWord.copy(
            accessed = (dateMillis.milliseconds + hours + minute.minutes).inWholeMilliseconds
        )
         */

        newWord = newWord.copy(
            accessed = LocalDateTime(dateMillis)
                .withHour(hours) // Do not replace with plusHours
                .plusMinutes(minutes.toLong())
                .millis
        )
    }

    fun getLevelsForName(courseWords: Map<Int, Word>) = courseWords
        .filter { (id, word) -> word.normalizedName == newWord.normalizedName && id != newId }
        .values
        .map(Word::level)
        .toSet()

    fun getLevelsForTranslation(courseWords: Map<Int, Word>) = courseWords
        .filter { (id, word) ->
            word.normalizedTranslation == newWord.normalizedTranslation && id != newId
        }
        .values
        .map(Word::level)
        .toSet()

    fun save() = newWord.copy(name = newWord.name.trim(), translation = newWord.translation.trim())
}