package com.meshenger.backend.session

class HelloWorldBridge {
    companion object {
        init {
            System.loadLibrary("session") // name of your .so library
        }
    }

    external fun getMessage(): String
}