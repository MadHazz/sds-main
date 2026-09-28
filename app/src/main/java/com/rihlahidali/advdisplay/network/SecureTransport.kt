package com.rihlahidali.advdisplay.network

import com.rihlahidali.advdisplay.BuildConfig
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

// Network Security Config is unavailable on Android 5. Enforce the same policy in OkHttp.
class SecureTransport(private val allowLocalHttp: Boolean = BuildConfig.DEBUG) : Interceptor {
    fun allows(url: HttpUrl): Boolean = url.username.isEmpty() && url.password.isEmpty() &&
        (url.isHttps || (allowLocalHttp && url.host in setOf("localhost", "127.0.0.1", "10.0.2.2")))

    override fun intercept(chain: Interceptor.Chain): Response {
        if (!allows(chain.request().url)) throw IOException("Content must use HTTPS without URL credentials.")
        return chain.proceed(chain.request())
    }
}
