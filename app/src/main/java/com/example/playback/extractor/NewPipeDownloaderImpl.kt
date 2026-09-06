package com.example.playback.extractor

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Production-ready OkHttp Downloader implementation for NewPipe Extractor.
 */
class NewPipeDownloaderImpl private constructor(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
) : Downloader() {

    companion object {
        private const val TAG = "NewPipeDownloader"

        @Volatile
        private var instance: NewPipeDownloaderImpl? = null

        @Volatile
        private var isInitialized = false

        fun getInstance(): NewPipeDownloaderImpl {
            return instance ?: synchronized(this) {
                instance ?: NewPipeDownloaderImpl().also { instance = it }
            }
        }

        /**
         * Initializes NewPipe globally with this downloader instance.
         * Must be called early in Application.onCreate.
         */
        fun initGlobal() {
            if (!isInitialized) {
                synchronized(this) {
                    if (!isInitialized) {
                        try {
                            val downloader = getInstance()
                            NewPipe.init(downloader)
                            isInitialized = true
                            Log.d(TAG, "NewPipe Extractor successfully initialized globally")
                        } catch (e: Throwable) {
                            Log.e(TAG, "Fatal error initializing NewPipe Extractor: ${e.message}", e)
                        }
                    }
                }
            }
        }
    }

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val requestBuilder = okhttp3.Request.Builder().url(url)

        headers.forEach { (name, values) ->
            values.forEach { value ->
                requestBuilder.addHeader(name, value)
            }
        }

        // Add standard user agent and language headers if not present
        if (!headers.containsKey("User-Agent")) {
            requestBuilder.header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
            )
        }
        if (!headers.containsKey("Accept-Language")) {
            requestBuilder.header("Accept-Language", "en-US,en;q=0.9")
        }

        val body: RequestBody? = dataToSend?.toRequestBody(null)
        requestBuilder.method(httpMethod, body)

        val response = client.newCall(requestBuilder.build()).execute()

        if (response.code == 429) {
            response.close()
            Log.w(TAG, "reCAPTCHA / 429 Rate limited on URL: $url")
            throw ReCaptchaException("reCAPTCHA requested or rate limited", url)
        }

        val responseBody = response.body?.string() ?: ""
        val responseHeaders = response.headers.toMultimap()

        return Response(
            response.code,
            response.message,
            responseHeaders,
            responseBody,
            response.request.url.toString()
        )
    }
}
