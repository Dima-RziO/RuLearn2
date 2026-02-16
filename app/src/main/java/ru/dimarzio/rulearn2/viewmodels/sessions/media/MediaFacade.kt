package ru.dimarzio.rulearn2.viewmodels.sessions.media

import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.CoroutineScope
import java.io.File
import java.util.Locale

object MediaFacade { // Facade
    fun play(
        player: MediaPlayer,
        tts: TextToSpeech?,
        scope: CoroutineScope,
        audios: List<File>?,
        phrase: String,
        locale: Locale,
        deviceVolume: Int,
        onPlayed: () -> Unit = {}
    ) {
        val builder = MediaChainBuilder(player, tts, scope)
            .addAudios(audios?.shuffled() ?: emptyList())
            .addTTS(phrase, locale)
            .addFallback()
        val handler = builder.build()

        handler.handle(deviceVolume, onPlayed)
    }
}