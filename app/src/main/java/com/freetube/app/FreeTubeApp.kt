package com.freetube.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.freetube.app.data.extractor.ExtractorHelper

@HiltAndroidApp
class FreeTubeApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize NewPipe Extractor
        ExtractorHelper.init(this)
    }
}
