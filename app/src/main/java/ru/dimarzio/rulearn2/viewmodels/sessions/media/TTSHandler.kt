package ru.dimarzio.rulearn2.viewmodels.sessions.media

import android.speech.tts.TextToSpeech
import ru.dimarzio.rulearn2.utils.say
import java.util.Locale

class TTSHandler(
    successor: MediaHandler? = null,
    val phrase: String,
    val locale: Locale,
    private val tts: TextToSpeech?,
) : MediaHandler(successor) {
    override fun handle(deviceVolume: Int, onHandled: () -> Unit) {
        if (deviceVolume > 0 && tts != null) {
            tts.say(phrase, locale) { success ->
                if (!success) {
                    super.handle(deviceVolume, onHandled)
                }
            }
        } else {
            super.handle(deviceVolume, onHandled)
        }
    }
}