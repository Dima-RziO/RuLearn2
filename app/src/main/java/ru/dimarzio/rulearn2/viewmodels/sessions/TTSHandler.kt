package ru.dimarzio.rulearn2.viewmodels.sessions

import android.speech.tts.TextToSpeech
import java.util.Locale
import java.io.File
import ru.dimarzio.rulearn2.utils.say

class TTSHandler(
    successor: MediaHandler? = null,
    private val tts: TextToSpeech,
    private val locale: Locale
) : MediaHandler(successor) {
    override fun handle(audio: File?, phrase: String, deviceVolume: Int, onHandled: () -> Unit) {
        if (deviceVolume > 0) {
            tts.say(phrase, locale) { success ->
                if (!success) {
                    super.handle(audio, phrase, deviceVolume, onHandled)
                }
            }
        } else {
            super.handle(audio, phrase, deviceVolume, onHandled)
        }
    }
}