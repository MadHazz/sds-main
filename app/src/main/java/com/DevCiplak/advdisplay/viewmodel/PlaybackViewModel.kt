package com.DevCiplak.advdisplay.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.DevCiplak.advdisplay.data.PlaylistCache
import com.DevCiplak.advdisplay.data.MediaDownloader
import com.DevCiplak.advdisplay.model.PlaybackMode
import com.DevCiplak.advdisplay.model.Playlist
import com.DevCiplak.advdisplay.network.HttpMediaDownloader
import com.DevCiplak.advdisplay.repository.ContentRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class PlaybackState(
    val playlist: Playlist? = null,
    val syncing: Boolean = false,
    val message: String? = null
)

class PlaybackViewModel(
    private val repository: ContentRepository = ContentRepository(),
    private val downloader: MediaDownloader = HttpMediaDownloader(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    private val mutableState = MutableStateFlow(PlaybackState())
    val state = mutableState.asStateFlow()
    private var syncJob: Job? = null

    fun refresh(uid: String, code: String, directory: File, mode: PlaybackMode, online: Boolean) {
        if (syncJob?.isCompleted == false) return
        syncJob = viewModelScope.launch {
            val cache = PlaylistCache(directory, downloader)
            // A cancelled sync may have committed to disk before publishing its UI state.
            val cached = withContext(ioDispatcher) { cache.load(mode) }
            mutableState.value = PlaybackState(cached, online)
            if (!online) {
                mutableState.value = PlaybackState(cached, message = "Offline. Connect to download content; cached content will keep playing.")
                return@launch
            }
            try {
                val remote = repository.getPlaylist(uid, code, mode)
                val complete = withContext(ioDispatcher) { cache.replace(remote) }
                mutableState.value = PlaybackState(complete)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.value = PlaybackState(cached, message = error.message ?: "Content update failed. Retrying shortly.")
            }
        }
    }

    fun stopSync() {
        syncJob?.cancel()
    }
}
