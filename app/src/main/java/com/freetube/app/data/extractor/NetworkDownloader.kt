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
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(false) // Let NewPipe handle redirects
        .followSslRedirects(false)
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
        
        // Standard browser User-Agent that NewPipe expects
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; rv:102.0) Gecko/20100101 Firefox/102.0"
    }
    
    override fun execute(request: ExtractorRequest): Response {
        val url = request.url()
        val httpMethod = request.httpMethod()
        val dataToSend = request.dataToSend()
        val headers = request.headers()
        
        val requestBuilder = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.5")
        
        // Add custom headers from the request (NewPipe sets these)
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
            android.util.Log.e("NetworkDownloader", "Request failed: $url - ${e.message}")
            throw e
        }
    }
}
