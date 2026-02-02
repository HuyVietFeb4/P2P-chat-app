package com.meshenger.backend.application

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise

class HelloWorldModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String = "HelloWorldModule"

    @ReactMethod
    fun getMessage(promise: Promise) {
        promise.resolve(HelloWorld.getMessage())
    }
}
