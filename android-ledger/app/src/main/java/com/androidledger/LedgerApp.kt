package com.androidledger

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LedgerApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
