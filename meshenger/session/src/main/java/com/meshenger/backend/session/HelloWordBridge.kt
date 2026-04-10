package com.meshenger.backend.session

import android.util.Log

class HelloWorldBridge {
    companion object {
        private var isLibLoaded = false
        init {
            try {
                System.loadLibrary("session")
                isLibLoaded = true
            } catch (e: UnsatisfiedLinkError) {
                Log.e("HelloWorldBridge", "Native library 'session' not found. getMessage() will return a fallback.")
            }
        }
    }

    fun getMessage(): String {
        return if (isLibLoaded) {
            getMessageNative()
        } else {
            "Native session library not available"
        }
    }

    private external fun getMessageNative(): String
}
