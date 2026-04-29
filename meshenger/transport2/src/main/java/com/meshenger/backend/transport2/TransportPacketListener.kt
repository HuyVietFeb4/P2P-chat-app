package com.meshenger.backend.transport2
interface TransportPacketListener {
    fun onReceivePacket(packet: ByteArray, sourceMac: String = "DumpAddr")
    fun onTriggerAntiEntropy()
}