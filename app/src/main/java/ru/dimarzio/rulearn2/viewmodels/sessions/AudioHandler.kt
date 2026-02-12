package ru.dimarzio.rulearn2.viewmodels.sessions

import android.media.MediaPlayer
import java.io.File
import ru.dimarzio.rulearn2.utils.play

class AudioHandler(successor: MediaHandler? = null, private val player: MediaPlayer) :
    MediaHandler(successor) {
    override fun handle(audio: File?, phrase: String, deviceVolume: Int, onHandled: () -> Unit) {
        if (audio != null && deviceVolume > 0) {
            player.play(audio, onHandled)
        } else {
            super.handle(audio, phrase, deviceVolume, onHandled)
        }
    }
}