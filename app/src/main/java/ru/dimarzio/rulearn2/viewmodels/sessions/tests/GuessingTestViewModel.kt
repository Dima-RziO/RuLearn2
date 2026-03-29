package ru.dimarzio.rulearn2.viewmodels.sessions.tests

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.dimarzio.rulearn2.models.Word
import ru.dimarzio.rulearn2.utils.deviceVolume
import ru.dimarzio.rulearn2.utils.whether
import ru.dimarzio.rulearn2.viewmodels.ErrorHandler
import ru.dimarzio.rulearn2.viewmodels.sessions.tests.media.MediaFacade
import java.util.Locale

// Do NOT use private val
class GuessingTestViewModel(private val handler: ErrorHandler) : ViewModel() {
    var loading by mutableStateOf(false)
        private set
    var reversed by mutableStateOf(listOf(true, false).random())
        private set
    var translations by mutableStateOf(emptyMap<Int, TranslationState>())
        private set

    enum class TranslationState {
        None,
        Correct,
        Wrong
    }

    // Due to bad performance, normalization was removed.
    @Deprecated("Please switch to Collection<T>.sortedBySimilarity2")
    inline fun <T> Collection<T>.sortedBySimilarity(
        similarBy: String,
        crossinline getComparable: (T) -> String
    ) = sortedWith(
        Comparator { a, b ->
            similarBy.foldRight(similarBy) { c, acc ->
                if (acc in getComparable(b)) {
                    return@Comparator 1
                } else if (acc in getComparable(a)) {
                    return@Comparator -1
                }
                acc.substringBeforeLast(c)
            }
            return@Comparator 0
        }
    )

    inline fun <T> Collection<T>.sortedBySimilarity2(
        similarBy: String,
        crossinline getComparable: (T) -> String
    ) = sortedByDescending { item ->
        val comparable = getComparable(item)
        val matching = similarBy.foldRight(similarBy) { c, acc ->
            if (acc in comparable) {
                return@sortedByDescending acc.length
            }
            acc.substringBeforeLast(c)
        }

        return@sortedByDescending matching.length
    }

    // Using templates is impossible because of possible recursion
    private operator fun Int.plus(list: List<Int>) = listOf(this) + list

    private fun generateTranslations(
        correctId: Int,
        correctName: String,
        courseWords: Map<Int, Word>,
        similarWords: Boolean,
        skippedWords: Boolean
    ) = correctId
        .plus(
            courseWords
                .entries
                .shuffled()
                .whether(similarWords) {
                    sortedBySimilarity2(correctName) { (_, word) -> word.name }
                }
                .whether(!skippedWords) {
                    filterNot { (_, word) -> word.skip }
                }
                .map { (id, _) -> id }
        )
        .distinct()
        .take(6)
        .shuffled()

    fun answer(
        context: Context,
        player: MediaPlayer,
        tts: TextToSpeech?,
        locale: Locale,
        correctId: Int,
        correctWord: Word,
        clicked: Int,
        onRefreshRequested: () -> Unit
    ) {
        translations = translations.mapValues { (id, _) ->
            when (correctId) {
                id -> TranslationState.Correct
                clicked -> TranslationState.None
                else -> TranslationState.Wrong
            }
        }

        val onPlayed = {
            if (clicked == correctId) {
                onRefreshRequested.invoke()
            }
        }

        MediaFacade.play(
            player, tts, viewModelScope,
            correctWord.audios, correctWord.name, locale,
            context.deviceVolume, onPlayed
        )
    }

    fun generateTranslations(
        id: Int,
        word: Word,
        courseWords: Map<Int, Word>,
        similarWords: Boolean,
        skippedWords: Boolean
    ) {
        viewModelScope.launch {
            loading = true

            val result = runCatching {
                withContext(Dispatchers.Default) {
                    generateTranslations(
                        id,
                        word.name,
                        courseWords,
                        similarWords,
                        skippedWords
                    )
                }
            }

            result
                .onSuccess { ids -> translations = ids.associateWith { TranslationState.None } }
                .onFailure(handler::onErrorHandled)

            loading = false
        }
    }

    fun reverse() {
        reversed = listOf(true, false).random()
    }
}