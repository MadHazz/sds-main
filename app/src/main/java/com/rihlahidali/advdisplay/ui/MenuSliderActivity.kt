package com.rihlahidali.advdisplay.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.rihlahidali.advdisplay.R
import com.rihlahidali.advdisplay.adapter.SliderAdapter
import com.rihlahidali.advdisplay.data.PlaylistCache
import com.rihlahidali.advdisplay.model.PlaybackMode
import com.rihlahidali.advdisplay.model.Playlist
import com.rihlahidali.advdisplay.network.HttpMediaDownloader
import com.rihlahidali.advdisplay.viewmodel.PlaybackViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MenuSliderActivity : PlaybackActivity() {
    private lateinit var viewModel: PlaybackViewModel
    private lateinit var slider: ViewPager2
    private var shown: Playlist? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!session.isValid()) return
        setContentView(R.layout.activity_menu_slider)
        viewModel = ViewModelProvider(this)[PlaybackViewModel::class.java]
        slider = findViewById(R.id.slider)
        findViewById<Button>(R.id.okBtn2).apply {
            setText(R.string.retry)
            setOnClickListener { refresh() }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state.collect { state ->
                        val playlist = state.playlist
                        if (playlist != null) {
                            if (shown != playlist) {
                                val position = slider.currentItem.coerceAtMost(playlist.pages.lastIndex)
                                shown = playlist
                                slider.adapter = SliderAdapter(playlist.pages, PlaylistCache(contentDirectory(), HttpMediaDownloader()))
                                slider.setCurrentItem(position, false)
                            }
                            slider.visibility = View.VISIBLE
                            findViewById<View>(R.id.loadingBack).visibility = View.GONE
                        } else {
                            findViewById<View>(R.id.loadingBack).visibility = View.VISIBLE
                            findViewById<TextView>(R.id.messageTitle).text = state.message ?: getString(R.string.downloading)
                            findViewById<View>(R.id.okBtn2).visibility = if (state.syncing) View.GONE else View.VISIBLE
                        }
                    }
                }
                launch {
                    while (isActive) {
                        refresh()
                        delay(if (viewModel.state.value.message != null || shown == null) 30_000 else 300_000)
                    }
                }
                launch {
                    while (isActive) {
                        delay((shown?.refreshSeconds ?: 5) * 1000L)
                        val count = slider.adapter?.itemCount ?: 0
                        if (count > 1 && slider.scrollState == ViewPager2.SCROLL_STATE_IDLE) {
                            slider.setCurrentItem((slider.currentItem + 1) % count, true)
                        }
                    }
                }
            }
        }
    }

    private fun refresh() {
        viewModel.refresh(requireNotNull(session.deviceId), requireNotNull(session.code),
            contentDirectory(), PlaybackMode.SLIDER, isOnline())
    }

    override fun onStop() {
        if (::viewModel.isInitialized) viewModel.stopSync()
        super.onStop()
    }
}
