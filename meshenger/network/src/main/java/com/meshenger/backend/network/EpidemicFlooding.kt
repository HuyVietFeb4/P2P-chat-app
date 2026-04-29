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

        val buffer = ByteBuffer.allocate(44 + packet.payload.size)
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
        buffer.putLong(packet.header.receiverID.toLong())

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
    fun onBootstrapSend(payload: ByteArray, timeStamp: Long) {
        epidemicScope.launch {
            val type = MessageType.BOOTSTRAP.value
            val senderID = MPAddress.getMyMPAddressULong()
            val packetLst = PacketFactory.createPackets(type, senderID = senderID,
                payload = payload, inputTimeStamp = timeStamp.toULong())

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
            val packetLst = PacketFactory.createPackets(type, senderID = MPAddress.getMyMPAddressULong(), receiverID = receiverID,
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

    override fun onReceivePacket(packet: ByteArray, sourceMac: String) {
        val decodedPacket = Packet.decode(packet) ?: return
        val sig = decodedPacket.signature

        // 1. ATOMIC CHECK & ADD (The "Guard")
        // Check membership and add immediately. If it was already there, stop.
        val isNew = when(decodedPacket.header.type) {
            MessageType.ANTI_ENTROPY_REQUEST.value,
            MessageType.ANTI_ENTROPY_RESPOND.value,
            MessageType.NOISE_HANDSHAKE.value,
            MessageType.BOOTSTRAP.value -> ProtocolPacketCache.addIfNew(sig, decodedPacket)
            else -> UserPacketCache.addIfNew(sig, decodedPacket)
        }

        if (!isNew) return // Already seen and being processed or finished. Stop the storm.

        // 2. TTL Check
        if (decodedPacket.header.TTL <= 0u) return

        // 3. Signature Verification (Now it's safe to do this "slow" work)
        val isValid = when(decodedPacket.header.type) {
            MessageType.USER_MESSAGE_ALL.value, MessageType.BOOTSTRAP.value -> verifyGlobalChatKey(decodedPacket)
            else -> verifyDirectChatKey(decodedPacket)
        }

        if (!isValid) {
            // If it was fake, remove it from cache so we can receive a valid one later
            UserPacketCache.removeFromCache(sig)
            ProtocolPacketCache.removeFromCache(sig)
            return
        }

        Log.d("EpidemicFlooding", "Received packet from: ${decodedPacket.header.senderID} with type: ${decodedPacket.header.type}")
        Log.d("EpidemicFlooding", "Received packet for: ${decodedPacket.header.receiverID}. My MPAddress: ${MPAddress.getMyMPAddressULong()}")
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
        val isForMe = decodedPacket.header.receiverID == myAddress
        val isBroadcast = decodedPacket.header.receiverID == SpecialRecipients.BROADCAST

        // 1. Process local delivery if applicable
        if (isForMe || isBroadcast) {
            Log.d("EpidemicFlooding", "My packet with ${decodedPacket.header.fragmentID} and total ${decodedPacket.header.totalFragments} arrived.")
            if(decodedPacket.header.totalFragments > 1.toUShort()) {
                Log.d("Reassembly", "Fragment ${decodedPacket.header.fragmentID} with total ${decodedPacket.header.totalFragments} stored. Waiting for more...")
                val completePayload = ReassemblyQueue.addToQueue(
                    ReassemblyQueue.getKeyFragment(decodedPacket),
                    decodedPacket.payload,
                    decodedPacket.header.totalFragments.toInt(),
                    decodedPacket.header.fragmentID.toInt()
                )
                if (completePayload != null) {
                    handleLocalDelivery(decodedPacket, completePayload, sourceMac)
                } else {
                    Log.d("Reassembly", "Fragment ${decodedPacket.header.fragmentID} stored. Waiting for more...")
                }
            } else {
                handleLocalDelivery(decodedPacket, decodedPacket.payload, sourceMac)
            }
        }

        // 2. Forwarding logic (Re-flood)
        // Only forward if it was a broadcast or if it wasn't for me personally
        if (isBroadcast || (!isForMe && decodedPacket.header.receiverID != SpecialRecipients.BROADCAST)) {
            val newTTL = (decodedPacket.header.TTL.toInt() - 1)
            if (newTTL > 0) {
                val updatedPacket = decodedPacket.copy(header = decodedPacket.header.copy(TTL = newTTL.toUShort()))
                Log.d("Epidemic Flooding", "Flooding packet to all neighbours with TTL: ${updatedPacket.header.TTL}")
                forwardPacket(updatedPacket, sourceMac)
            }
        }
    }

    override fun onTriggerAntiEntropy() {
        epidemicScope.launch {
            val type = MessageType.ANTI_ENTROPY_REQUEST.value
            val physicalPeerList = MeshConnectionRegistry.getPhysicalPeerList()
            val compactVector = summaryVector.toCompactedBinary()
            Log.d("Epidemic Flooding", "Compact vector size: ${compactVector.size}")
            for(peer in physicalPeerList) {
                val senderID = MPAddress.getMyMPAddressULong()
                val peerMPAddress = peer.MPAddress?: continue
                val receiverID = MPAddress.MPAddressByteArrayToULong(peerMPAddress)
                val packetLst = PacketFactory.createPackets(type, senderID = senderID, receiverID = receiverID,
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
    private fun handleLocalDelivery(packet: Packet, completePayload: ByteArray, sourceMac: String) {
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
                        val receiverID = packet.header.senderID
                        val senderID = MPAddress.getMyMPAddressULong()
                        val wrapPayload = Packet.encode(entry.value) ?: continue
                        val packetLst = PacketFactory.createPackets(type, senderID = senderID, receiverID = receiverID,
                            payload = wrapPayload, inputTimeStamp = System.currentTimeMillis().toULong())
                        for(packet in packetLst) {
                            forwardPacket(packet, sourceMac)
                        }
                    }
                }
            }
            MessageType.ANTI_ENTROPY_RESPOND.value -> {
                onReceivePacket(completePayload)
            }
            MessageType.NOISE_HANDSHAKE.value -> {
                messageListener?.onReceiveMessageHandShake(
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
            MessageType.BOOTSTRAP.value -> {
                messageListener?.onBootStrapReceived(
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