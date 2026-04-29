package com.meshenger.backend.session

import android.content.Intent
import android.graphics.Mesh
import android.util.Log
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// ONLY FOR TESTING, WILL BE DELETED FOR REAL APP
class SessionModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    override fun getName(): String = "SessionModule"

    init {
        // Observe the _messageBus and send every incoming/outgoing message to JS
        // This is the "Engine" that powers your React Native UI
        Log.d("SessionModule", "Starting Bus Collector...")
        CoroutineScope(Dispatchers.Main).launch {
            GlobalChatSession.getMessageBus().collect { json ->
                val eventMap = Arguments.createMap().apply {
                    putString("PeerID", json["PeerID"].toString().replace("\"", ""))
                    putString("Message", json["Message"].toString().replace("\"", ""))
                    putString("SessionType", json["SessionType"].toString().replace("\"", ""))
                    putString("Action", json["Action"].toString().replace("\"", ""))
                }
                if (reactContext.hasActiveReactInstance()) {
                    Log.d("SessionModule", "Attempting to emit to JS: ${eventMap.toString()}")
                    sendEvent("onNewMessage", eventMap)
                } else {
                    Log.w("SessionModule", "JS instance not ready, dropping message")
                }
            }
        }
    }

    private fun sendEvent(eventName: String, params: WritableMap?) {
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)
    }

    @ReactMethod
    fun globalChatSendMessageStr(msg: String) {
        GlobalChatSession.sendMessageStr(message = msg)
    }
}