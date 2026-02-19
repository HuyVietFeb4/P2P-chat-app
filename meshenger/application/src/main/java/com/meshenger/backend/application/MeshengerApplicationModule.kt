package com.meshenger.backend.application

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise

import com.meshenger.backend.session.HelloWorldBridge

/**
 * React Native native module for the Application layer.
 * Exposes APIs to the JS frontend (e.g. getMessage, getMessageSession).
 */
class MeshengerApplicationModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    private val bridge = HelloWorldBridge()

    override fun getName(): String = "MeshengerApplicationModule"

    @ReactMethod
    fun getMessage(promise: Promise) {
        promise.resolve("Application layer says: ${AppInfo.getMessage()}")
    }

    @ReactMethod
    fun getMessageSession(promise: Promise) {
        promise.resolve(bridge.getMessage())
    }
}
