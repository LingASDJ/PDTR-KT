package com.example.pdtranslator

import android.app.Application
import cat.ereza.customactivityoncrash.config.CaocConfig

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        CaocConfig.Builder.create()
            .backgroundMode(CaocConfig.BACKGROUND_MODE_SILENT) //default: CaocConfig.BACKGROUND_MODE_SHOW_CUSTOM
            .enabled(true) //default: true
            .showErrorDetails(true) //default: true
            .showRestartButton(true) //default: true
            .logErrorOnRestart(true) //default: true
            .trackActivities(true) //default: false
            .minTimeBetweenCrashesInMillis(2000) //default: 3000
            .apply()
    }
}
