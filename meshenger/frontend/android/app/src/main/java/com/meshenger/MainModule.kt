package com.meshenger

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.meshenger.backend.transport2.MeshMaintainer

class MainModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String = "MainModule"

    @ReactMethod
    fun ensureServiceStarted() {
        val activity = getCurrentActivity()
        val application = reactApplicationContext.applicationContext

        // 1. Initialize Advertiser with Application Context if needed
        // (This is safe to call multiple times if you have an 'isRunning' check)
        // BleAdvertiser.init(application) // If not already handled in startMeshService

        // 2. Start Service using Activity context if available, otherwise Application
        val startContext = activity ?: application
        MeshMaintainer.startMeshService(startContext)
    }

    @ReactMethod
    fun stopService() {
        val application = reactApplicationContext.applicationContext
        val intent = android.content.Intent(application, MeshMaintainer::class.java)
        application.stopService(intent)
    }
}