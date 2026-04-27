package com.meshenger.backend.network

import android.util.Log
import com.meshenger.backend.security_native.NativeCredentials
import com.meshenger.backend.transport2.MPAddress
import com.meshenger.backend.transport2.MeshConnectionRegistry
import com.meshenger.backend.transport2.TransportPacketListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object EpidemicFlooding : TransportPacketListener {
    private val epidemicJob = SupervisorJob()

    // 2. Define the scope
    // Dispatchers.IO is best for networking/BLE as it doesn't block the UI
    private val epidemicScope = CoroutineScope(Dispatchers.IO + epidemicJob)
    private var messageListener: NetworkMessageListener? = null
    private val globalKeySpec by lazy {
        SecretKeySpec(NativeCredentials.getGlobalChatKey().encodeToByteArray(), "HmacSHA512")
    }
    private val directKeySpec by lazy {
        SecretKeySpec(NativeCredentials.getTwoPartyChatKey().encodeToByteArray(), "HmacSHA512")
    }
    private val summaryVector: KMCompactRefinedBloomFilter = KMCompactRefinedBloomFilter()
    private fun verifyGlobalChatKey(packet: Packet): Boolean {
        val sha512HMAC = Mac.getInstance("HmacSHA512")
        sha512HMAC.init(globalKeySpec)

        val buffer = ByteBuffer.allocate(36 + packet.payload.size)
        buffer.order(ByteOrder.BIG_ENDIAN)
        buffer.putShort(packet.header.version.toShort()) // version
        buffer.putShort(packet.header.flags.toShort())
        buffer.putInt(packet.header.type.toInt())
        buffer.putShort(packet.payload.size.toShort())
        buffer.putShort(packet.header.fragmentID.toShort())
        buffer.putShort(packet.header.totalFragments.toShort())
        buffer.putLong(packet.header.timeStamp.toLong())
        buffer.putLong(packet.header.senderID.toLong())

        buffer.put(packet.payload)
        return MessageDigest.isEqual(sha512HMAC.doFinal(buffer.array()), packet.signature)
    }
    private fun verifyDirectChatKey(packet: Packet): Boolean {
        val sha512HMAC = Mac.getInstance("HmacSHA512")
        sha512HMAC.init(directKeySpec)

        val buffer = ByteBuffer.allocate(36 + packet.payload.size)
        buffer.order(ByteOrder.BIG_ENDIAN)
        buffer.order(ByteOrder.BIG_ENDIAN)
        buffer.putShort(packet.header.version.toShort()) // version
        buffer.putShort(packet.header.flags.toShort())
        buffer.putInt(packet.header.type.toInt())
        buffer.putShort(packet.payload.size.toShort())
        buffer.putShort(packet.header.fragmentID.toShort())
        buffer.putShort(packet.header.totalFragments.toShort())
        buffer.putLong(packet.header.timeStamp.toLong())
        buffer.putLong(packet.header.senderID.toLong())
        buffer.putLong(packet.header.recieverID.toLong())

        buffer.put(packet.payload)
        return MessageDigest.isEqual(sha512HMAC.doFinal(buffer.array()), packet.signature)
    }
    // Optional: Clean up when the mesh stops
    fun stopMesh() {
        epidemicJob.cancel()
    }

    fun setListener(listener: NetworkMessageListener) {
        this.messageListener = listener
    }
    fun onGlobalChatMessageSend(msg: ByteArray, timeStamp: Long) {
        epidemicScope.launch {
            val type = MessageType.USER_MESSAGE_ALL.value
            val senderID = MPAddress.getMyMPAddressULong()
            val packetLst = PacketFactory.createPackets(type, senderID = senderID,
                payload = msg, inputTimeStamp = timeStamp.toULong())

            packetLst.forEach { packet ->
                val packetEncoded = Packet.encode(packet) ?: return@forEach
                UserPacketCache.addToCache(packet.signature, packet)

                // Parallel flood to all neighbors
                MeshConnectionRegistry.getOutboundMap().values.forEach {
                    launch { it.sendPacketToServerSuspending(packetEncoded) }
                }
            }
        }
    }
    fun onTwoPartyMessageSend(msg: ByteArray, timeStamp: Long, receiverId: ULong, messageType: MessageType) {
        epidemicScope.launch {
            val type = messageType.value
            val receiverID = receiverId
            val packetLst = PacketFactory.createPackets(type, senderID = MPAddress.getMyMPAddressULong(), recieverID = receiverID,
                payload = msg, inputTimeStamp = timeStamp.toULong())

            packetLst.forEach { packet ->
                val packetEncoded = Packet.encode(packet) ?: return@forEach
                UserPacketCache.addToCache(packet.signature, packet)

                // Parallel flood to all neighbors
                MeshConnectionRegistry.getOutboundMap().values.forEach {
                    launch { it.sendPacketToServerSuspending(packetEncoded) }
                }
            }
        }
    }

    override fun onRecievePacket(packet: ByteArray, sourceMac: String) {
        val decodedPacket = Packet.decode(packet) ?: return
        var packetDropCondition = UserPacketCache.checkMembership(decodedPacket.signature)
                || ProtocolPacketCache.checkMembership(decodedPacket.signature)
                || decodedPacket.header.TTL <= 0u
        when(decodedPacket.header.type) {
            MessageType.USER_MESSAGE_ALL.value,
            MessageType.BOOTSTRAP.value -> {
                packetDropCondition = packetDropCondition || !verifyGlobalChatKey(decodedPacket)
            }
            MessageType.USER_MESSAGE_ONE_TO_ONE.value,
            MessageType.NOISE_HANDSHAKE.value,
            MessageType.ANTI_ENTROPY_REQUEST.value,
            MessageType.ANTI_ENTROPY_RESPOND.value,-> {
                // signature = getSignatureOneToOne(...)
                packetDropCondition = packetDropCondition || !verifyDirectChatKey(decodedPacket)
            }

            else -> {
                Log.e("EpidemicFlooding", "Unhandled message type: ${decodedPacket.header.type}")
                return
            }
        }
        // Quick Exit: Drop if seen before or TTL expired or the signature does not match (packetDropCondition is false)
        if (packetDropCondition) {
            return
        }

        Log.d("EpidemicFlooding", "Received packet from: ${decodedPacket.header.senderID}")
        // add to cache
        when(decodedPacket.header.type) {
            MessageType.ANTI_ENTROPY_REQUEST.value,
            MessageType.ANTI_ENTROPY_RESPOND.value,
            MessageType.NOISE_HANDSHAKE.value,
            MessageType.BOOTSTRAP.value -> {
                ProtocolPacketCache.addToCache(decodedPacket.signature, decodedPacket)
            }
            MessageType.USER_MESSAGE_ALL.value,
            MessageType.USER_MESSAGE_ONE_TO_ONE.value,
            MessageType.USER_MESSAGE_GROUP.value-> {
                // signature = getSignatureOneToOne(...)
                UserPacketCache.addToCache(decodedPacket.signature, decodedPacket)
            }
            else -> {
                Log.e("EpidemicFlooding", "Unhandled message type: ${decodedPacket.header.type}")
                return
            }
        }

        if(decodedPacket.header.type != MessageType.ANTI_ENTROPY_REQUEST.value &&
            decodedPacket.header.type != MessageType.ANTI_ENTROPY_RESPOND.value) {
            summaryVector.add(decodedPacket.signature)
        }


        val myAddress = MPAddress.getMyMPAddressULong()
        val isForMe = decodedPacket.header.recieverID == myAddress
        val isBroadcast = decodedPacket.header.recieverID == SpecialRecipients.BROADCAST

        // 1. Process local delivery if applicable
        if (isForMe || isBroadcast) {
            if(decodedPacket.header.totalFragments > 1.toUShort()) {
                val completePayload = ReassemblyQueue.addToQueue(
                    ReassemblyQueue.getKeyFragment(decodedPacket),
                    decodedPacket.payload,
                    decodedPacket.header.totalFragments.toInt(),
                    decodedPacket.header.fragmentID.toInt()
                )
                if (completePayload != null) {
                    handleLocalDelivery(decodedPacket, completePayload)
                } else {
                    Log.d("Reassembly", "Fragment ${decodedPacket.header.fragmentID} stored. Waiting for more...")
                }
            } else {
                handleLocalDelivery(decodedPacket, decodedPacket.payload)
            }
        }

        // 2. Forwarding logic (Re-flood)
        // Only forward if it was a broadcast or if it wasn't for me personally
        if (isBroadcast || !isForMe) {
            val newHeader = decodedPacket.header.copy(TTL = (decodedPacket.header.TTL - 1u).toUShort())
            val updatedPacket = decodedPacket.copy(header = newHeader)
            Log.d("EpidemicFlooding", "Flooding packet to all neighbors. New TTL: ${newHeader.TTL}")
            forwardPacket(updatedPacket, sourceMac)
        }
    }

    override fun onTriggerAntiEntropy() {
        epidemicScope.launch {
            val type = MessageType.ANTI_ENTROPY_REQUEST.value
            val physicalPeerList = MeshConnectionRegistry.getPhysicalPeerList()
            val compactVector = summaryVector.toCompactedBinary()
            for(peer in physicalPeerList) {
                val senderID = MPAddress.getMyMPAddressULong()
                val recieverID = MPAddress.MPAddressByteArrayToULong(peer.MPAddress)
                val packetLst = PacketFactory.createPackets(type, senderID = senderID, recieverID = recieverID,
                    payload = compactVector, inputTimeStamp = System.currentTimeMillis().toULong())
                packetLst.forEach { packet ->
                    forwardPacket(packet)
                }
            }
        }
    }

    /**
     * Handles passing the message up to the Session layer
     */
    private fun handleLocalDelivery(packet: Packet, completePayload: ByteArray) {
        when (packet.header.type) {
            MessageType.USER_MESSAGE_ALL.value -> {
                messageListener?.onGlobalMessageReceived(
                    packet.header.senderID,
                    completePayload,
                    packet.header.timeStamp
                )
            }
            MessageType.ANTI_ENTROPY_REQUEST.value -> {
                val peerSummaryVector = KMCompactRefinedBloomFilter()
                peerSummaryVector.fromCompactedBinary(completePayload)
                val allEntries = UserPacketCache.getAllEntries()
                var sentCount = 0
                val MAX_AE_RESPONSES = 15
                for(entry in allEntries) {
                    if (sentCount >= MAX_AE_RESPONSES) break
                    if(!peerSummaryVector.isAvailable(entry.key.toByteArray())
                        && entry.value.header.type != MessageType.ANTI_ENTROPY_RESPOND.value
                        && entry.value.header.type != MessageType.ANTI_ENTROPY_REQUEST.value
                        ) {
                        // Create new packet of ANTI_ENTROPY_RESPOND
                        // Wrap the packet to payload (which will guaranteed to be fragment => special treatment of reassembly for ANTI_ENTROPY_RESPOND)
                        // Send to the one who request
                        sentCount++
                        val type = MessageType.ANTI_ENTROPY_RESPOND.value
                        val recieverID = packet.header.senderID
                        val senderID = MPAddress.getMyMPAddressULong()
                        val wrapPayload = Packet.encode(entry.value) ?: continue
                        val packetLst = PacketFactory.createPackets(type, senderID = senderID, recieverID = recieverID,
                            payload = wrapPayload, inputTimeStamp = System.currentTimeMillis().toULong())
                        for(packet in packetLst) {
                            forwardPacket(packet)
                        }
                    }
                }
            }
            MessageType.ANTI_ENTROPY_RESPOND.value -> {
                onRecievePacket(completePayload)
            }
            MessageType.NOISE_HANDSHAKE.value -> {
                messageListener?.onRecieveMessageHandShake(
                    packet.header.senderID,
                    completePayload,
                )
            }
            MessageType.USER_MESSAGE_ONE_TO_ONE.value -> {
                messageListener?.onDirectMessageReceived(
                    packet.header.senderID,
                    completePayload,
                    packet.header.timeStamp
                )
            }
            MessageType.USER_MESSAGE_GROUP.value -> { /* TODO */ }
            else -> Log.e("Basic Flooding", "Unknown packet type: ${packet.header.type}")
        }
    }

    /**
     * Pushes to all neighbors
     */
    private fun forwardPacket(packet: Packet, excludeMac: String = "DumpAddr") {
        val encoded = Packet.encode(packet) ?: return
        // Forward to everyone except (ideally) the person who sent it to you
        epidemicScope.launch(Dispatchers.IO) {
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
                        } catch (e: Exception) {
                            Log.e("EpidemicFlooding", "Can not forward packet! Error: ${e.message}")
                        }
                    }
                }
        }
    }

}