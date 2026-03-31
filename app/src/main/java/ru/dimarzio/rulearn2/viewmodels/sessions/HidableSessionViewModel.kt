package ru.dimarzio.rulearn2.viewmodels.sessions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Deprecated("No longer needed.")
open class HidableSessionViewModel(imp: SessionViewModelImp) : SessionViewModel(imp) {
    var hidden by mutableStateOf(false)
        private set

    override fun next() {
        if (!imp.isDone()) {
            imp.next()

            hidden = true
        }

        currentWord = imp.current()
    }

    fun toggleHidden(hidden: Boolean) {
        this.hidden = hidden
    }
}