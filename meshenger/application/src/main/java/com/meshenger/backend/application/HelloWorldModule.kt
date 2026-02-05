package com.meshenger.backend.application

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise

import com.meshenger.backend.session.HelloWorldBridge

class HelloWorldModuleApplication(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    private val bridge = HelloWorldBridge()

    override fun getName(): String = "HelloWorldModuleApplication"

    @ReactMethod
    fun getMessage(promise: Promise) {
        promise.resolve("Application layer says: ${HelloWorld.getMessage()}")
    }

    @ReactMethod
    fun getMessageSession(promise: Promise) {
        promise.resolve(bridge.getMessage())
    }
}
