package com.DevCiplak.advdisplay.ui

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.VideoView
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.DevCiplak.advdisplay.R
import com.DevCiplak.advdisplay.data.PlaylistCache
import com.DevCiplak.advdisplay.model.PlaybackMode
import com.DevCiplak.advdisplay.network.HttpMediaDownloader
import com.DevCiplak.advdisplay.viewmodel.PlaybackState
import com.DevCiplak.advdisplay.viewmodel.PlaybackViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class VideoPlayerActivity : PlaybackActivity() {
    private lateinit var viewModel: PlaybackViewModel
    private lateinit var video: VideoView
    private var files = emptyList<String>()
    private var index = 0
    private var resumePosition = 0
    private var failedVideos = 0
    private var started = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!session.isValid()) return
        setContentView(R.layout.activity_video_player)
        viewModel = ViewModelProvider(this)[PlaybackViewModel::class.java]
        video = findViewById(R.id.videoView)
        index = savedInstanceState?.getInt("index") ?: 0
        resumePosition = savedInstanceState?.getInt("position") ?: 0
        video.setOnPreparedListener {
            if (resumePosition > 0) video.seekTo(resumePosition)
            resumePosition = 0
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) video.start()
        }
        video.setOnCompletionListener {
            failedVideos = 0
            index = (index + 1) % files.size
            playCurrent()
        }
        video.setOnErrorListener { _, _, _ ->
            failedVideos++
            if (failedVideos >= files.size) {
                started = false
                showStatus(getString(R.string.video_unplayable), false)
                lifecycleScope.launch {
                    delay(30_000)
                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                        failedVideos = 0
                        playCurrent()
                    }
                }
            } else {
                index = (index + 1) % files.size
                video.post { if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) playCurrent() }
            }
            true
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect { render(it) } }
                launch {
                    while (isActive) {
                        viewModel.refresh(requireNotNull(session.deviceId), requireNotNull(session.code),
                            contentDirectory(), PlaybackMode.VIDEO, isOnline())
                        delay(if (viewModel.state.value.message != null || files.isEmpty()) 30_000 else 300_000)
                    }
                }
            }
        }
    }

    private fun render(state: PlaybackState) {
        val cache = PlaylistCache(contentDirectory(), HttpMediaDownloader())
        val next = state.playlist?.pages?.flatMap { it.media }?.map { cache.fileFor(it).absolutePath }.orEmpty()
        if (next.isNotEmpty()) {
            if (next != files) {
                val current = files.getOrNull(index)
                files = next
                index = current?.let { next.indexOf(it).takeIf { position -> position >= 0 } } ?: index.coerceIn(next.indices)
                if (current != null && current != files[index]) resumePosition = 0
                if (current != files[index] || !started) {
                    failedVideos = 0
                    playCurrent()
                }
            } else if (!started && failedVideos == 0) {
                playCurrent()
            }
            if (started) findViewById<View>(R.id.loadingBack).visibility = View.GONE
        } else {
            showStatus(state.message ?: getString(R.string.downloading), state.syncing)
        }
    }

    private fun playCurrent() {
        if (files.isEmpty()) return
        started = true
        findViewById<View>(R.id.loadingBack).visibility = View.GONE
        video.setVideoURI(java.io.File(files[index]).toUri())
    }

    private fun showStatus(message: String, busy: Boolean) {
        findViewById<View>(R.id.loadingBack).visibility = View.VISIBLE
        findViewById<TextView>(R.id.errorTitle).text = message
        findViewById<ProgressBar>(R.id.progressBar).visibility = if (busy) View.VISIBLE else View.GONE
    }

    override fun onStop() {
        if (::video.isInitialized) {
            resumePosition = video.currentPosition
            video.stopPlayback()
            started = false
            failedVideos = 0
            viewModel.stopSync()
        }
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("index", index)
        outState.putInt("position", if (::video.isInitialized && started) video.currentPosition else resumePosition)
        super.onSaveInstanceState(outState)
    }
}
