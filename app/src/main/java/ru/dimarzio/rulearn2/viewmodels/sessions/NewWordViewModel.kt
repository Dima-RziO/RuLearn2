package ru.dimarzio.rulearn2.viewmodels.sessions

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ru.dimarzio.rulearn2.models.Word
import ru.dimarzio.rulearn2.utils.deviceVolume
import ru.dimarzio.rulearn2.viewmodels.sessions.media.MediaChainBuilder
import java.util.Locale

class NewWordViewModel : ViewModel() {
    fun next(
        context: Context,
        player: MediaPlayer,
        tts: TextToSpeech?,
        locale: Locale,
        word: Word
    ) {
        val builder = MediaChainBuilder(player, tts, viewModelScope)
            .addAudios(word.audios ?: emptyList())
            .addTTS(word.name, locale)
            .addFallback()
        val handler = builder.build()

        handler.handle(context.deviceVolume)
    }
}