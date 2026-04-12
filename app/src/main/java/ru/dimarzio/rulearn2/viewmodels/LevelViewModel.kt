package ru.dimarzio.rulearn2.viewmodels

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import ru.dimarzio.rulearn2.models.Word
import ru.dimarzio.rulearn2.utils.toMutableStateMap

class LevelViewModel(private val level: String, courseWords: Map<Int, Word>) : ViewModel() {
    private val words = courseWords.filter { (_, word) -> word.level == level }.toMutableStateMap()

    var sortMethod by mutableStateOf(SortMethod.Id)
    val sortedWords by derivedStateOf {
        words.entries
            .sortedWith(
                compareBy { (id, word) ->
                    when (sortMethod) {
                        SortMethod.Id -> id
                        SortMethod.Name -> word.name
                        SortMethod.Repeat -> !word.isRepeat // descending
                        SortMethod.Rate -> word.successRate
                    }
                }
            )
            .associate(Map.Entry<Int, Word>::toPair)
    }

    enum class SortMethod {
        Id,
        Name,
        Repeat,
        Rate
    }

    fun sort(method: SortMethod) {
        sortMethod = method
    }

    fun updateWord(id: Int, word: Word) {
        if (word.level == level) {
            words[id] = word
        } else {
            words -= id
        }
    }

    fun removeWord(id: Int) {
        words -= id
    }
}