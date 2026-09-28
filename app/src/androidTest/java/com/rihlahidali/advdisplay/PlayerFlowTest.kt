package com.rihlahidali.advdisplay

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.os.SystemClock
import android.view.View
import android.webkit.WebView
import android.widget.ImageView
import android.widget.VideoView
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.rihlahidali.advdisplay.data.SessionStore
import com.rihlahidali.advdisplay.model.PlaybackMode
import com.rihlahidali.advdisplay.ui.MainIdentifierActivity
import com.rihlahidali.advdisplay.ui.MenuSliderActivity
import com.rihlahidali.advdisplay.ui.VideoPlayerActivity
import com.rihlahidali.advdisplay.ui.WebViewActivity
import com.rihlahidali.advdisplay.viewmodel.PlaybackViewModel
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(AndroidJUnit4::class)
class PlayerFlowTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private var server: MockWebServer? = null
    @Volatile private var failPages = false
    @Volatile private var failWeb = false

    @Before fun setup() {
        assumeTrue("Run with -PADVDISPLAY_BASE_URL=http://127.0.0.1:18080/", BuildConfig.API_BASE_URL == "http://127.0.0.1:18080/")
        SessionStore(context).clearSession()
        listOf("VIDEO_TEST", "PANEL_TEST", "WEB_TEST").forEach {
            File(context.getExternalFilesDir(null), it).deleteRecursively()
        }
        val png = ByteArrayOutputStream().use { stream ->
            Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GREEN) }
                .compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.toByteArray()
        }
        val video = FixtureVideo.create(File(context.cacheDir, "fixture.mp4"))
        server = MockWebServer().apply {
            dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    val url = requireNotNull(request.requestUrl)
                    val code = url.queryParameter("c").orEmpty()
                    val template = if (code.startsWith("PANEL")) "3" else "4"
                    return when (url.encodedPath) {
                        "/Display/GetMenuType" -> json("""{"Status":true,"is_menu":"${if (code.startsWith("WEB")) "0" else "1"}"}""")
                        "/Display/Menu" -> json("""{"Status":true,"page_detail":[{"page_code":"$code","template_id":"$template"}],"channel_data":{"refresh_rate":"5000"}}""")
                        "/Display/GetPage" -> if (failPages) MockResponse().setResponseCode(503) else {
                            val media = if (template == "3") (1..3).joinToString(",") { """{"filename":"image.png","slot":"$it"}""" }
                                else """{"filename":"video.mp4"}"""
                            json("""{"Status":true,"page_data":[$media]}""")
                        }
                        "/assets/contents/image.png" -> MockResponse().setHeader("Content-Type", "image/png").setBody(Buffer().write(png))
                        "/assets/contents/video.mp4" -> MockResponse().setHeader("Content-Type", "video/mp4").setBody(Buffer().write(video))
                        "/display" -> if (failWeb) MockResponse().setResponseCode(503) else MockResponse()
                            .setHeader("Content-Type", "text/html").setBody("<html><head><title>AdvDisplay fixture ready</title></head><body>Display ready</body></html>")
                        else -> MockResponse().setResponseCode(404)
                    }
                }
            }
            start(18080)
        }
    }

    @After fun teardown() {
        instrumentation.runOnMainSync {
            ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).toList().forEach { it.finish() }
        }
        server?.shutdown()
        if (server != null) SessionStore(context).clearSession()
    }

    @Test fun videoInvitePlaysSurvivesFailedSyncAndRequiresPinToExit() {
        launchInvite("VIDEO_TEST")
        waitFor { it is VideoPlayerActivity && it.findViewById<VideoView>(R.id.videoView).isPlaying }
        failPages = true
        instrumentation.runOnMainSync {
            val activity = current()
            ViewModelProvider(activity as VideoPlayerActivity)[PlaybackViewModel::class.java].refresh(
                requireNotNull(SessionStore(context).getDeviceId()), "VIDEO_TEST",
                File(context.getExternalFilesDir(null), "VIDEO_TEST"), PlaybackMode.VIDEO, true)
        }
        waitFor {
            it is VideoPlayerActivity && ViewModelProvider(it)[PlaybackViewModel::class.java].state.value.message != null &&
                it.findViewById<VideoView>(R.id.videoView).isPlaying
        }
        pressBack()
        onView(withId(R.id.passwordEditText)).inRoot(isDialog()).perform(replaceText("0000"), closeSoftKeyboard())
        onView(withText(R.string.continue_action)).inRoot(isDialog()).perform(click())
        assertNotNull(SessionStore(context).getCode())
        onView(withId(R.id.passwordEditText)).inRoot(isDialog()).perform(replaceText(BuildConfig.ADMIN_PIN), closeSoftKeyboard())
        onView(withText(R.string.continue_action)).inRoot(isDialog()).perform(click())
        waitFor { it is MainIdentifierActivity }
        assertNull(SessionStore(context).getCode())
        assertTrue(File(context.getExternalFilesDir(null), "VIDEO_TEST/playlist.json").exists())
    }

    @Test fun threePanelImagesRestoreFromCacheWhenApiFails() {
        launchInvite("PANEL_TEST")
        waitFor { it is MenuSliderActivity && it.findViewById<ImageView>(R.id.leftImg)?.drawable != null &&
            it.findViewById<ImageView>(R.id.topImg)?.drawable != null && it.findViewById<ImageView>(R.id.rightImg)?.drawable != null }
        failPages = true
        context.startActivity(Intent(context, MainIdentifierActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        waitFor { it is MenuSliderActivity && it.findViewById<ImageView>(R.id.leftImg)?.drawable != null &&
            it.findViewById<View>(R.id.loadingBack).visibility == View.GONE }
    }

    @Test fun webDisplayCanRetryAfterHttpError() {
        failWeb = true
        launchInvite("WEB_TEST")
        waitFor { it is WebViewActivity && it.findViewById<View>(R.id.loadingBack).visibility == View.VISIBLE &&
            it.hasWindowFocus() && fullyVisible(it.findViewById(R.id.okBtn2)) }
        failWeb = false
        onView(withId(R.id.okBtn2)).perform(click())
        waitFor { it is WebViewActivity && it.findViewById<WebView>(R.id.webView).title == "AdvDisplay fixture ready" &&
            it.findViewById<View>(R.id.loadingBack).visibility == View.GONE }
    }

    @Test fun invalidInviteShowsRecoverableErrorWithoutSession() {
        launchInvite("../INVALID")
        waitFor { it is MainIdentifierActivity && it.findViewById<View>(R.id.messageBox).visibility == View.VISIBLE }
        assertNull(SessionStore(context).getCode())
        onView(withId(R.id.okBtn)).perform(click())
        waitFor { it is MainIdentifierActivity && it.findViewById<View>(R.id.loadingBackground).visibility == View.GONE }
    }

    @Test fun malformedInviteCannotExposeSetupForAnExistingScreen() {
        launchInvite("VIDEO_TEST")
        waitFor { it is VideoPlayerActivity && it.findViewById<VideoView>(R.id.videoView).isPlaying }
        var original: Activity? = null
        instrumentation.runOnMainSync { original = current() }
        launchInvite("../INVALID")
        waitFor { it !== original && it is VideoPlayerActivity && it.findViewById<VideoView>(R.id.videoView).isPlaying }
        assertEquals("VIDEO_TEST", SessionStore(context).getCode())
    }

    @Test fun replacementInviteRequiresPinAndCancelPreservesTheOriginalScreen() {
        launchInvite("VIDEO_TEST")
        waitFor { it is VideoPlayerActivity && it.findViewById<VideoView>(R.id.videoView).isPlaying }
        launchInvite("PANEL_TEST")
        waitFor { it is MainIdentifierActivity }
        onView(withId(R.id.passwordEditText)).inRoot(isDialog()).perform(replaceText("0000"), closeSoftKeyboard())
        onView(withText(R.string.continue_action)).inRoot(isDialog()).perform(click())
        assertEquals("VIDEO_TEST", SessionStore(context).getCode())
        onView(withText(android.R.string.cancel)).inRoot(isDialog()).perform(click())
        waitFor { it is VideoPlayerActivity && it.findViewById<VideoView>(R.id.videoView).isPlaying }
        assertEquals("VIDEO_TEST", SessionStore(context).getCode())
    }

    private fun launchInvite(code: String) {
        context.startActivity(Intent(context, MainIdentifierActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = "advdisplay://join?c=$code".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
    }

    private fun waitFor(condition: (Activity) -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 30000
        var lastState = "No resumed activity"
        while (SystemClock.elapsedRealtime() < deadline) {
            var matched = false
            instrumentation.runOnMainSync { currentOrNull()?.let {
                matched = condition(it)
                lastState = "${it.javaClass.simpleName}, focused=${it.hasWindowFocus()}, " +
                    "error=${it.findViewById<View>(R.id.loadingBack)?.visibility}, " +
                    "webTitle=${it.findViewById<WebView>(R.id.webView)?.title}"
            } }
            if (matched) return
            SystemClock.sleep(100)
        }
        fail("UI condition did not become true within 30 seconds: $lastState")
    }

    private fun fullyVisible(view: View): Boolean {
        val visible = Rect()
        return view.width > 0 && view.height > 0 && view.getGlobalVisibleRect(visible) &&
            visible.width() >= view.width && visible.height() >= view.height
    }

    private fun currentOrNull() = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).firstOrNull()
    private fun current() = requireNotNull(currentOrNull())
    private fun json(body: String) = MockResponse().setHeader("Content-Type", "application/json").setBody(body)
}
