package com.rihlahidali.advdisplay.ui

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.rihlahidali.advdisplay.data.SessionSnapshot
import com.rihlahidali.advdisplay.data.SessionStore
import com.rihlahidali.advdisplay.security.AdminGate
import java.io.File

abstract class PlaybackActivity : AppCompatActivity() {
    protected lateinit var session: SessionSnapshot
    protected lateinit var sessionStore: SessionStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        sessionStore = SessionStore(this)
        session = sessionStore.getSession()
        if (!session.isValid()) {
            returnToSetup()
            return
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                AdminGate.confirm(this@PlaybackActivity) { returnToSetup() }
            }
        })
    }

    protected fun returnToSetup() {
        sessionStore.clearSession()
        startActivity(Intent(this, MainIdentifierActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }

    protected fun contentDirectory(): File = File(getExternalFilesDir(null) ?: filesDir, requireNotNull(session.code))

    protected fun isOnline(): Boolean {
        val manager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            manager.getNetworkCapabilities(manager.activeNetwork)
                ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } else {
            @Suppress("DEPRECATION")
            manager.activeNetworkInfo?.isConnected == true
        }
    }
}
