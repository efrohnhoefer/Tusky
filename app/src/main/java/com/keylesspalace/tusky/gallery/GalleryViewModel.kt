package com.keylesspalace.tusky.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class GalleryViewModel @Inject constructor() : ViewModel() {
    sealed class Event {
        object ToggleChrome: Event()
        object Dismiss: Event()
        object Reset: Event()
        data class Drag(val dy: Float): Event()
    }

    val events: Flow<Event> get() = _events
    private val _events = MutableSharedFlow<Event>()

    fun toggleChrome() {
        viewModelScope.launch {
            _events.emit(Event.ToggleChrome)
        }
    }

    fun dismiss() {
        viewModelScope.launch {
            _events.emit(Event.Dismiss)
        }
    }

    fun drag(dy: Float) {
        viewModelScope.launch {
            _events.emit(Event.Drag(dy))
        }
    }

    fun reset() {
        viewModelScope.launch {
            _events.emit(Event.Reset)
        }
    }
}
