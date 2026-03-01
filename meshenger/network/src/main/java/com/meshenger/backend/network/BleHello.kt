package com.meshenger.backend.network

class BleHello {
    companion object {
        @JvmStatic
        fun sayHello(msg: String): String {
            return "BLE layer received: $msg"
        }
    }
}
