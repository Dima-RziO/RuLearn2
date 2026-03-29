package ru.dimarzio.rulearn2.viewmodels

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import ru.dimarzio.rulearn2.models.Word
import ru.dimarzio.rulearn2.tflite.TFLiteModel
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class WordViewModel(
    private val model: TFLiteModel?,
    courseWords: Map<Int, Word>,
    id: Int?,
    level: String? // Pass null if you are sure that word exists
) : ViewModel() {
    private val LocalDateTime.millis
        get() = atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    private fun LocalDateTime(millis: Long): LocalDateTime = Instant
        .ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()

    val oldWord = courseWords[id]

    val newId = id ?: courseWords.maxOfOrNull { (id, _) -> id + 1 } ?: 1

    var newWord by mutableStateOf(oldWord ?: Word(level.toString()))
        private set

    val modified by derivedStateOf { newWord != oldWord }

    val levelsForName by derivedStateOf {
        courseWords.values
            .distinctBy(Word::level)
            .mapNotNull { word ->
                if (word.normalizedName == newWord.normalizedName && id != newId) {
                    word.level
                } else {
                    null
                }
            }
    }

    val levelsForTranslation by derivedStateOf {
        courseWords.values
            .distinctBy(Word::level)
            .mapNotNull { word ->
                if (word.translation == newWord.translation && id != newId) {
                    word.level
                } else {
                    null
                }
            }
    }

    fun updateWord(word: Word) {
        newWord = word
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

    fun save() = newWord.copy(
        name = newWord.name.trim(),
        translation = newWord.translation.trim(),
        successRate = model?.predict(newWord.toFeatures(newId)) ?: 1f
    )
}