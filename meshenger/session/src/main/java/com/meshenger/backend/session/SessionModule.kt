package com.meshenger.backend.session

import android.content.Intent
import android.graphics.Mesh
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule

// ONLY FOR TESTING, WILL BE DELETED FOR REAL APP
class SessionModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    private val context: ReactApplicationContext
    // This is the name you will use in your JavaScript code
    override fun getName(): String = "SessionModule"
    init {
        context = reactContext
    }
    @ReactMethod
    fun globalChatSendMessageStr(msg: String) {
        GlobalChatSession.sendMessageStr(message = msg)
    }
}