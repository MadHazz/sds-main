package com.DevCiplak.advdisplay.network

import com.DevCiplak.advdisplay.data.MediaDownloader
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class HttpMediaDownloader(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .addNetworkInterceptor(SecureTransport())
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.MINUTES)
        .followSslRedirects(false)
        .build()
) : MediaDownloader {
    override suspend fun download(url: String, destination: File) {
        withContext(Dispatchers.IO) {
            val context = currentCoroutineContext()
            val call = client.newCall(Request.Builder().url(url).build())
            // Interrupt socket reads immediately on cancellation, before cache rollback removes files.
            val cancellation = launch(start = CoroutineStart.UNDISPATCHED) {
                try { awaitCancellation() } finally { call.cancel() }
            }
            try {
                call.execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Media download failed (HTTP ${response.code}).")
                    val body = response.body ?: throw IOException("Empty download response.")
                    val contentType = body.contentType()?.toString().orEmpty()
                    if (contentType.startsWith("text/") || contentType.contains("json")) {
                        throw IOException("The server returned a document instead of media.")
                    }
                    val written = destination.outputStream().use { output ->
                        body.byteStream().use { input ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            var total = 0L
                            while (true) {
                                context.ensureActive()
                                val count = input.read(buffer)
                                if (count == -1) break
                                output.write(buffer, 0, count)
                                total += count
                            }
                            output.fd.sync()
                            total
                        }
                    }
                    if (written == 0L || (body.contentLength() >= 0 && written != body.contentLength())) {
                        throw IOException("Media download was incomplete.")
                    }
                }
            } catch (error: IOException) {
                context.ensureActive()
                throw error
            } finally {
                cancellation.cancel()
            }
        }
    }
}
