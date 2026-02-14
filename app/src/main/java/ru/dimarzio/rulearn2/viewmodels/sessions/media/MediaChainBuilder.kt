package ru.dimarzio.rulearn2.viewmodels.sessions.media

import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.CoroutineScope
import java.io.File
import java.util.Locale

class MediaChainBuilder(
    private val player: MediaPlayer,
    private val tts: TextToSpeech?,
    private val scope: CoroutineScope
) { // Builder
    private val handlers = mutableListOf<MediaHandler>()

    fun addAudio(audio: File): MediaChainBuilder {
        handlers.add(AudioHandler(player = player, audio = audio))
        return this
    }

    fun addAudios(audios: List<File>): MediaChainBuilder {
        audios.forEach { audio ->
            handlers.add(AudioHandler(player = player, audio = audio))
        }
        return this
    }

    fun addTTS(phrase: String, locale: Locale): MediaChainBuilder {
        handlers.add(TTSHandler(tts = tts, phrase = phrase, locale = locale))
        return this
    }

    fun addFallback(): MediaChainBuilder {
        handlers.add(FallbackHandler(scope))
        return this
    }

    fun addNext(handler: MediaHandler): MediaChainBuilder {
        handlers.add(handler)
        return this
    }

    fun build(): MediaHandler {
        return if (handlers.isNotEmpty()) {
            handlers.reduceRight { current, next ->
                when (current) {
                    is AudioHandler -> AudioHandler(next, current.audio, player)
                    is TTSHandler -> TTSHandler(next, current.phrase, current.locale, tts)
                    else -> current
                }
            }
        } else {
            MediaHandler()
        }
    }
}