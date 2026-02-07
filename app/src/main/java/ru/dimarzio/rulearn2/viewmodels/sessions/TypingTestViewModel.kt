package ru.dimarzio.rulearn2.viewmodels.sessions

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ru.dimarzio.rulearn2.models.Word
import ru.dimarzio.rulearn2.utils.deviceVolume
import ru.dimarzio.rulearn2.utils.normalized
import ru.dimarzio.rulearn2.utils.play
import ru.dimarzio.rulearn2.utils.say
import ru.dimarzio.rulearn2.utils.timer
import vladis.luv.rulearn.Utils
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

class TypingTestViewModel() : ViewModel() {
    var inputEnabled by mutableStateOf(true)
        private set
    var error by mutableStateOf(false)
        private set
    var inputValue by mutableStateOf(TextFieldValue())
        private set
    var hintsUsed by mutableIntStateOf(0)
        private set

    private fun String.takeHint(correct: String): String {
        val match = foldRight(this) { c, acc ->
            if (correct.normalized().startsWith(acc.normalized())) {
                return@foldRight acc
            }
            acc.substringBeforeLast(c)
        }

        return runCatching { correct.take(match.length + 1) }.getOrDefault(match)
    }

    fun type(
        context: Context,
        player: MediaPlayer,
        tts: TextToSpeech?,
        locale: Locale,
        correct: Word,
        value: TextFieldValue,
        onRefreshRequested: () -> Unit
    ): Boolean {
        inputValue = value

        if (value.text.normalized() == correct.normalizedName) {
            answer(context, player, tts, locale, correct, true, onRefreshRequested)
            return true
        }
        return false
    }

    fun takeHint(
        context: Context,
        player: MediaPlayer,
        tts: TextToSpeech?,
        locale: Locale,
        papasHints: Boolean,
        correct: Word,
        onRefreshRequested: () -> Unit
    ): Boolean {
        val text = if (!papasHints) {
            inputValue.text.takeHint(correct.name)
        } else {
            Utils.getHint(inputValue.text, correct.name)
        }

        hintsUsed++

        return type(
            context,
            player,
            tts,
            locale,
            correct,
            TextFieldValue(text, TextRange(text.length)),
            onRefreshRequested
        )
    }

    fun answer(
        context: Context,
        player: MediaPlayer,
        tts: TextToSpeech?,
        locale: Locale,
        word: Word,
        correct: Boolean,
        onRefreshRequested: () -> Unit
    ) {
        inputEnabled = false
        if (!correct) {
            error = true
        } else {
            inputValue = TextFieldValue(word.name)
        }

        val audio = word.randomAudio
        if (correct) {
            if (audio != null && context.deviceVolume > 0) {
                player.play(audio, onRefreshRequested)
            } else if (tts != null && context.deviceVolume > 0) {
                tts.say(word.name, locale) { success ->
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
            audio?.let { player.play(audio) } ?: tts?.say(word.name, locale)
        }
    }

    fun reset() {
        inputEnabled = true
        error = false
        inputValue = TextFieldValue()
        hintsUsed = 0
    }
}