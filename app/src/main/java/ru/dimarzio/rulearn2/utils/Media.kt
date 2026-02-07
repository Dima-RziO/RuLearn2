package ru.dimarzio.rulearn2.utils

import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.io.File
import java.util.Locale

fun MediaPlayer.play(file: File, onCompletion: () -> Unit = {}) {
    reset()
    setDataSource(file.path)
    prepare()
    start()
    setOnCompletionListener { onCompletion() }
}

fun TextToSpeech.say(text: String, locale: Locale?, onCompletion: (Boolean) -> Unit = {}) {
    setOnUtteranceProgressListener(
        object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {

            }

            override fun onDone(utteranceId: String?) {
                onCompletion(true)
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                onCompletion(false)
            }
        }
    )

    language = locale

    if (speak(text, TextToSpeech.QUEUE_FLUSH, null, "") == TextToSpeech.ERROR) {
        onCompletion(false)
    }
}