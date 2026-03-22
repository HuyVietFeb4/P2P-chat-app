package com.meshenger.backend.transport

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod

class BleModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    // This is the name you will use in your JavaScript code
    override fun getName(): String = "BleModule"
    init {
        BleAdvertiser.init(reactContext.applicationContext)
    }
    @ReactMethod
    fun onDemandScan(scanPeriodMs: Int = 10000) {
        // This calls the object you just built!
        BleScanner.onDemandScan(scanPeriodMs.toLong())
    }

    @ReactMethod
    fun onBackgroundScan() {
        BleScanner.onBackgroundScan()
    }

    @ReactMethod
    fun onStartAdvertise() {
        BleAdvertiser.onStartAdvertise()
    }
}