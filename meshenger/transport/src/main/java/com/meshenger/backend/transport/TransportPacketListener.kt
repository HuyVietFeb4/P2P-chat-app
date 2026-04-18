package com.meshenger.backend.transport
interface TransportPacketListener {
    fun onRecievePacket(packet: ByteArray, sourceMac: String)
}