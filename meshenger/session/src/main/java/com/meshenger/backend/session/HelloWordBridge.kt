package com.meshenger.backend.session

class HelloWorldBridge {
    companion object {
        init {
            System.loadLibrary("session")
        }
    }

    external fun getMessage(): String
}