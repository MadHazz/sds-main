package com.DevCiplak.advdisplay.ui

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.DevCiplak.advdisplay.R
import com.DevCiplak.advdisplay.analytics.GrowthTracker
import com.DevCiplak.advdisplay.data.SessionStore
import com.DevCiplak.advdisplay.model.PlaybackMode
import com.DevCiplak.advdisplay.security.AdminGate
import com.DevCiplak.advdisplay.util.IdentifierCode
import com.DevCiplak.advdisplay.viewmodel.IdentificationState
import com.DevCiplak.advdisplay.viewmodel.MainIdentifierViewModel
import kotlinx.coroutines.launch

class MainIdentifierActivity : AppCompatActivity() {
    private lateinit var viewModel: MainIdentifierViewModel
    private lateinit var sessionStore: SessionStore
    private lateinit var input: EditText
    private lateinit var identifyButton: Button
    private lateinit var shareButton: Button
    private lateinit var deviceId: String

    @SuppressLint("HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        setContentView(R.layout.activity_main_identifier)
        sessionStore = SessionStore(this)
        deviceId = sessionStore.getOrCreateDeviceId(Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID))
        viewModel = ViewModelProvider(this)[MainIdentifierViewModel::class.java]
        input = findViewById(R.id.editIdentify)
        identifyButton = findViewById(R.id.IdentifyAction)
        shareButton = findViewById(R.id.shareInviteAction)
        identifyButton.setOnClickListener { viewModel.identify(deviceId, input.text.toString()) }
        input.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_DONE) {
                if (identifyButton.isEnabled) identifyButton.performClick()
                true
            } else false
        }
        shareButton.setOnClickListener { shareInvite() }
        findViewById<Button>(R.id.okBtn).setOnClickListener { viewModel.resetState() }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    when (state) {
                        IdentificationState.Idle -> showIdle()
                        IdentificationState.Loading -> {
                            setControlsEnabled(false)
                            findViewById<View>(R.id.loadingBackground).visibility = View.VISIBLE
                            findViewById<View>(R.id.progressBar).visibility = View.VISIBLE
                            findViewById<View>(R.id.messageBox).visibility = View.GONE
                        }
                        is IdentificationState.Error -> showError(state.message)
                        is IdentificationState.Ready -> {
                            sessionStore.saveSession(state.code, state.deviceId, state.mode.sessionValue)
                            if (state.fromInvite) GrowthTracker.trackEvent(this@MainIdentifierActivity, "invite_accepted")
                            viewModel.resetState()
                            openPlayer(state.mode)
                        }
                    }
                }
            }
        }
        if (savedInstanceState == null || (sessionStore.getSession().isValid() && viewModel.state.value == IdentificationState.Idle)) {
            processIntent(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.resetState()
        processIntent(intent)
    }

    private fun processIntent(intent: Intent) {
        val saved = sessionStore.getSession()
        val link = intent.data
        if (link != null) {
            val code = runCatching {
                require(link.isHierarchical && link.scheme == "sds" && link.host == "join") { "Invalid SDS invite link." }
                IdentifierCode.normalize(link.getQueryParameter("c").orEmpty())
            }.getOrElse {
                if (saved.isValid()) {
                    openPlayer(requireNotNull(PlaybackMode.fromSession(saved.menuType)))
                } else {
                    viewModel.showError(it.message ?: getString(R.string.invalid_invite))
                }
                return
            }
            val accept = {
                input.setText(code)
                GrowthTracker.trackEvent(this, "invite_link_opened")
                viewModel.identify(deviceId, code, fromInvite = true)
            }
            if (saved.isValid()) {
                // External invites cannot replace an existing screen without the operator PIN.
                AdminGate.confirm(this, onCancel = { openPlayer(requireNotNull(PlaybackMode.fromSession(saved.menuType))) }) {
                    sessionStore.clearSession()
                    accept()
                }
            } else accept()
        } else if (saved.isValid()) {
            openPlayer(requireNotNull(PlaybackMode.fromSession(saved.menuType)))
        }
    }

    private fun openPlayer(mode: PlaybackMode) {
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(input.windowToken, 0)
        input.clearFocus()
        val target = when (mode) {
            PlaybackMode.WEB -> WebViewActivity::class.java
            PlaybackMode.SLIDER -> MenuSliderActivity::class.java
            PlaybackMode.VIDEO -> VideoPlayerActivity::class.java
        }
        startActivity(Intent(this, target))
        finish()
    }

    private fun showIdle() {
        setControlsEnabled(true)
        findViewById<View>(R.id.loadingBackground).visibility = View.GONE
    }

    private fun showError(message: String) {
        setControlsEnabled(true)
        findViewById<View>(R.id.loadingBackground).visibility = View.VISIBLE
        findViewById<View>(R.id.progressBar).visibility = View.GONE
        findViewById<View>(R.id.messageBox).visibility = View.VISIBLE
        findViewById<TextView>(R.id.errorTitle).text = message
    }

    private fun setControlsEnabled(enabled: Boolean) {
        input.isEnabled = enabled
        identifyButton.isEnabled = enabled
        shareButton.isEnabled = enabled
    }

    private fun shareInvite() {
        val code = runCatching { IdentifierCode.normalize(input.text.toString()) }.getOrElse {
            input.error = it.message
            return
        }
        val link = Uri.Builder().scheme("sds").authority("join").appendQueryParameter("c", code).build().toString()
        val text = getString(R.string.share_invite_message, code, link)
        GrowthTracker.trackEvent(this, "share_clicked")
        try {
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }, getString(R.string.share_invite_chooser_title)))
        } catch (_: ActivityNotFoundException) {
            (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("SDS invite", text))
            Toast.makeText(this, R.string.invite_copied, Toast.LENGTH_LONG).show()
        }
    }
}
