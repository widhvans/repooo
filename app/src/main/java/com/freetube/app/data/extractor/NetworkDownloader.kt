package com.freetube.app.data.extractor

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request as ExtractorRequest
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Custom OkHttp-based downloader for NewPipe Extractor
 * Handles all HTTP requests for YouTube data extraction
 */
class NetworkDownloader private constructor() : Downloader() {
    
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .build()
    
    companion object {
        private var instance: NetworkDownloader? = null
        
        @Synchronized
        fun getInstance(): NetworkDownloader {
            if (instance == null) {
                instance = NetworkDownloader()
            }
            return instance!!
        }
        
        // User agent mimicking Android YouTube app
        private const val USER_AGENT = "com.google.android.youtube/19.02.39 (Linux; U; Android 14; en_US; sdk_gphone64_arm64 Build/UE1A.230829.036.A1) gzip"
        
        // Additional headers for YouTube
        private const val YOUTUBE_CLIENT_VERSION = "19.02.39"
    }
    
    override fun execute(request: ExtractorRequest): Response {
        val url = request.url()
        val httpMethod = request.httpMethod()
        val dataToSend = request.dataToSend()
        val headers = request.headers()
        
        val requestBuilder = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "*/*")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Accept-Encoding", "gzip, deflate")
            .header("X-YouTube-Client-Name", "3")
            .header("X-YouTube-Client-Version", YOUTUBE_CLIENT_VERSION)
        
        // Add custom headers from the request
        headers.forEach { (key, values) ->
            values.forEach { value ->
                requestBuilder.addHeader(key, value)
            }
        }
        
        // Set request method and body
        when (httpMethod) {
            "GET" -> requestBuilder.get()
            "HEAD" -> requestBuilder.head()
            "POST" -> {
                val body = dataToSend?.toRequestBody() ?: "".toRequestBody()
                requestBuilder.post(body)
            }
            "PUT" -> {
                val body = dataToSend?.toRequestBody() ?: "".toRequestBody()
                requestBuilder.put(body)
            }
            "DELETE" -> requestBuilder.delete()
            else -> requestBuilder.get()
        }
        
        try {
            val response = client.newCall(requestBuilder.build()).execute()
            
            // Log for debugging
            android.util.Log.d("NetworkDownloader", "Request: $url -> ${response.code}")
            
            // Check for reCAPTCHA or rate limiting
            if (response.code == 429) {
                response.close()
                throw ReCaptchaException("reCAPTCHA challenge detected", url)
            }
            
            val responseBody = response.body?.string() ?: ""
            val responseHeaders = mutableMapOf<String, List<String>>()
            
            response.headers.forEach { (name, value) ->
                val existing = responseHeaders[name]?.toMutableList() ?: mutableListOf()
                existing.add(value)
                responseHeaders[name] = existing
            }
            
            return Response(
                response.code,
                response.message,
                responseHeaders,
                responseBody,
                url
            )
        } catch (e: IOException) {
            android.util.Log.e("NetworkDownloader", "Request failed: ${e.message}", e)
            throw e
        }
    }
}
