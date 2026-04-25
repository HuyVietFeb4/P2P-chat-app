package com.meshenger.backend.transport2
interface TransportPacketListener {
    fun onRecievePacket(packet: ByteArray, sourceMac: String)
    fun onTriggerAntiEntropy()
}