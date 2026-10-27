package com.example

import android.app.Application
import android.util.Log
import com.example.shizuku.ShizukuManager

class StretchXApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("StretchX", "StretchX Application starting")
        ShizukuManager.init(this)
    }
}
