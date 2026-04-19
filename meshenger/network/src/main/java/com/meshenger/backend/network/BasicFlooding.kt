package com.meshenger.backend.network

import android.util.Log
import com.meshenger.backend.transport2.MeshConnectionRegistry
import com.meshenger.backend.transport2.MeshMaintainer
import com.meshenger.backend.transport2.TransportPacketListener
import com.meshenger.backend.transport2.server.BleServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object BasicFlooding : TransportPacketListener {
    private val meshJob = SupervisorJob()

    // 2. Define the scope
    // Dispatchers.IO is best for networking/BLE as it doesn't block the UI
    private val meshScope = CoroutineScope(Dispatchers.IO + meshJob)

    // Optional: Clean up when the mesh stops
    fun stopMesh() {
        meshJob.cancel()
    }
    private var messageListener: NetworkMessageListener? = null
    fun setListener(listener: NetworkMessageListener) {
        this.messageListener = listener
    }
    fun onUserMessageSend(msg: ByteArray, timeStamp: Long) {
        meshScope.launch {
            val type = MessageType.USER_MESSAGE_ALL.value
            val senderID = MPAddress.getMyMPAddressULong()
            val packetLst = PacketFactory.createBroadcastPackets(type, senderID, msg, timeStamp.toULong())

            packetLst.forEach { packet ->
                val packetEncoded = Packet.encode(packet) ?: return@forEach
                PacketCache.addToCache(packet.signature, packet)

                // Parallel flood to all neighbors
                MeshConnectionRegistry.getOutboundMap().values.forEach {
                    launch { it.sendPacketToServerSuspending(packetEncoded) }
                }
            }
        }
    }

    override fun onRecievePacket(packet: ByteArray, sourceMac: String) {
        val decodedPacket = Packet.decode(packet) ?: return

        // Quick Exit: Drop if seen before or TTL expired
        if (PacketCache.checkMembership(decodedPacket.signature) || decodedPacket.header.TTL <= 0u) {
            return
        }

        Log.d("BasicFlooding", "Received packet from: ${decodedPacket.header.senderID}")
        PacketCache.addToCache(decodedPacket.signature, decodedPacket)

        val myAddress = MPAddress.getMyMPAddressULong()
        val isForMe = decodedPacket.header.recieverID == myAddress
        val isBroadcast = decodedPacket.header.recieverID == SpecialRecipients.BROADCAST

        // 1. Process local delivery if applicable
        if (isForMe || isBroadcast) {
            handleLocalDelivery(decodedPacket)
        }

        // 2. Forwarding logic (Re-flood)
        // Only forward if it was a broadcast or if it wasn't for me personally
        if (isBroadcast || !isForMe) {
            forwardPacket(decodedPacket, sourceMac)
        }
    }

    /**
     * Handles passing the message up to the UI/Session layer
     */
    private fun handleLocalDelivery(packet: Packet) {
        when (packet.header.type) {
            MessageType.USER_MESSAGE_ALL.value -> {
                messageListener?.onUserMessageReceived(
                    packet.header.senderID,
                    packet.payload,
                    packet.header.timeStamp
                )
            }
            MessageType.USER_MESSAGE_ONE_TO_ONE.value -> { /* TODO */ }
            MessageType.USER_MESSAGE_GROUP.value -> { /* TODO */ }
            else -> Log.e("Basic Flooding", "Unknown packet type: ${packet.header.type}")
        }
    }

    /**
     * Decrements TTL and pushes to all neighbors
     */
    private fun forwardPacket(packet: Packet, excludeMac: String) {
        val newHeader = packet.header.copy(TTL = (packet.header.TTL - 1u).toUShort())
        val updatedPacket = packet.copy(header = newHeader)
        val encoded = Packet.encode(updatedPacket) ?: return

        Log.d("BasicFlooding", "Flooding packet to all neighbors. New TTL: ${newHeader.TTL}")

        // Forward to everyone except (ideally) the person who sent it to you
        meshScope.launch(Dispatchers.IO) {
            // Parallel send: Launch each peer in its own job
            // so one broken peer doesn't hang the whole mesh
            // 2. Forward to Outbound Servers (Client Mode)
            MeshConnectionRegistry.getOutboundMap()
                .filterKeys { it != excludeMac }
                .values
                .forEach { connection ->
                    launch {
                        try {
                            connection.sendPacketToServerSuspending(encoded)
                        } catch (e: Exception) { /* Log error */ }
                    }
                }
        }
    }
}

// 1. Implement the logic of basic flooding
    // Downward data: Create packet => save to packet cache => push to all neighbour
    // Upward data:
        // Minus packet TTL
        // If dest address == my address || dest address == broacast address
            // Send up ward
        // else
            // store in packet cache
            // flooding to neighbours that is not the one that send the packet to this device

// 2. Update the onDataRecieved logic from BleClientConnection and BleServerConnection