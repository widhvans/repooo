package com.freetube.app.data.extractor

import android.content.Context
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import java.util.Locale

/**
 * Helper object to initialize and configure NewPipe Extractor
 */
object ExtractorHelper {
    
    private var isInitialized = false
    
    /**
     * Initialize NewPipe Extractor with custom downloader
     */
    fun init(context: Context) {
        if (isInitialized) return
        
        try {
            // Initialize with custom OkHttp-based downloader
            NewPipe.init(NetworkDownloader.getInstance())
            
            // Set localization based on device settings
            val locale = Locale.getDefault()
            val localization = Localization(locale.language, locale.country)
            val contentCountry = ContentCountry(locale.country)
            
            NewPipe.setPreferredLocalization(localization)
            NewPipe.setPreferredContentCountry(contentCountry)
            
            isInitialized = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Get the YouTube service from NewPipe
     */
    fun getYouTubeService() = ServiceList.YouTube
    
    /**
     * Get the service ID for YouTube
     */
    fun getYouTubeServiceId() = ServiceList.YouTube.serviceId
    
    /**
     * Extract video ID from various YouTube URL formats
     */
    fun extractVideoId(url: String): String? {
        val patterns = listOf(
            "(?:youtube\\.com/watch\\?v=|youtu\\.be/|youtube\\.com/embed/|youtube\\.com/v/|youtube\\.com/shorts/)([a-zA-Z0-9_-]{11})",
            "^([a-zA-Z0-9_-]{11})$"
        )
        
        for (pattern in patterns) {
            val regex = Regex(pattern)
            val match = regex.find(url)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        return null
    }
    
    /**
     * Build a YouTube video URL from ID
     */
    fun buildVideoUrl(videoId: String): String {
        return "https://www.youtube.com/watch?v=$videoId"
    }
    
    /**
     * Build a YouTube channel URL from ID
     */
    fun buildChannelUrl(channelId: String): String {
        return "https://www.youtube.com/channel/$channelId"
    }
    
    /**
     * Build a YouTube shorts URL from ID
     */
    fun buildShortsUrl(videoId: String): String {
        return "https://www.youtube.com/shorts/$videoId"
    }
}
