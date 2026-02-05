package com.meshenger.backend.transport

class BleHello {
    companion object {
        @JvmStatic
        fun sayHello(msg: String): String {
            return "BLE layer received: $msg"
        }
    }
}
