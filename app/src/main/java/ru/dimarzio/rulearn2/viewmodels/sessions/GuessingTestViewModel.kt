package ru.dimarzio.rulearn2.viewmodels.sessions

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import ru.dimarzio.rulearn2.models.Word
import ru.dimarzio.rulearn2.utils.deviceVolume
import ru.dimarzio.rulearn2.utils.play
import ru.dimarzio.rulearn2.utils.say
import ru.dimarzio.rulearn2.utils.sortedBySimilarity2
import ru.dimarzio.rulearn2.utils.timer
import ru.dimarzio.rulearn2.utils.whether
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

// Do NOT use private val
class GuessingTestViewModel : ViewModel() {
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

        val audio = correctWord.randomAudio
        if (correctId == clicked) {
            if (audio != null && context.deviceVolume > 0) {
                player.play(audio, onRefreshRequested)
            } else if (tts != null && context.deviceVolume > 0) {
                tts.say(correctWord.name, locale) { success ->
                    if (success) {
                        viewModelScope.timer(0.5.seconds, onRefreshRequested)
                    } else {
                        viewModelScope.timer(1.5.seconds, onRefreshRequested)
                    }
                }
            } else {
                viewModelScope.timer(1.5.seconds, onRefreshRequested)
            }
        } else { // Incorrect
            audio?.let { player.play(audio) } ?: tts?.say(correctWord.name, locale)
        }
    }

    fun regenerateTranslations(
        scope: CoroutineScope,
        id: Int,
        word: Word,
        courseWords: Map<Int, Word>,
        similarWords: Boolean,
        skippedWords: Boolean
    ) = scope.async(Dispatchers.Default) {
        loading = true

        runCatching { generateTranslations(id, word.name, courseWords, similarWords, skippedWords) }
            .onSuccess { ids -> translations = ids.associateWith { TranslationState.None } }
            .also { _ -> loading = false }
            .exceptionOrNull()
    }

    fun reverse() {
        reversed = listOf(true, false).random()
    }
}