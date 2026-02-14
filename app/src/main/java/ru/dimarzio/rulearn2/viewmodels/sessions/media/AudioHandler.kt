package ru.dimarzio.rulearn2.viewmodels.sessions.media

import android.media.MediaPlayer
import java.io.File
import ru.dimarzio.rulearn2.utils.play

class AudioHandler(successor: MediaHandler? = null, val audio: File, val player: MediaPlayer) :
    MediaHandler(successor) {
    override fun handle(deviceVolume: Int, onHandled: () -> Unit) {
        if (deviceVolume > 0 && audio.canRead() && audio.isFile) {
            player.play(audio, onHandled)
        } else {
            super.handle(deviceVolume, onHandled)
        }
    }
}