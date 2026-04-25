package com.meshenger.backend.transport2

import android.bluetooth.BluetoothDevice
import com.meshenger.backend.transport2.PhysicalPeer
import com.meshenger.backend.transport2.client.BleClientConnection
import com.meshenger.backend.transport2.server.BleServerConnection
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.mutableMapOf

import java.util.concurrent.CopyOnWriteArrayList
object MeshConnectionRegistry {
    @Volatile private var isInMesh = false

    private val inboundMap = ConcurrentHashMap<String, BleServerConnection>()
    private val outboundMap = ConcurrentHashMap<String, BleClientConnection>()
    private val pendingConnections = ConcurrentHashMap.newKeySet<String>() // Track "in-flight" attempts

    private val physicalPeerList = CopyOnWriteArrayList<PhysicalPeer>()

    fun getAllPeers(): Set<String> = inboundMap.keys + outboundMap.keys
    fun getCountConnections(): Int = inboundMap.size + outboundMap.size
    fun isPending(address: String): Boolean = pendingConnections.contains(address)

    fun markPending(address: String) { pendingConnections.add(address) }
    fun unmarkPending(address: String) { pendingConnections.remove(address) }

    fun addInbound(bleAddress: String, conn: BleServerConnection) { inboundMap[bleAddress] = conn }
    fun removeInbound(bleAddress: String) { inboundMap.remove(bleAddress) }

    fun addOutBound(bleAddress: String, conn: BleClientConnection) {
        outboundMap[bleAddress] = conn
        unmarkPending(bleAddress)
    }
    fun removeOutbound(bleAddress: String) { outboundMap.remove(bleAddress) }

    fun getInboundMap() = inboundMap
    fun getOutboundMap() = outboundMap

    fun getInbound(address: String) = inboundMap[address]
    fun getOutbound(address: String) = outboundMap[address]

    fun inMeshDevices(device: BluetoothDevice) {
        if (physicalPeerList.none { it.device.address == device.address }) {
            physicalPeerList.add(PhysicalPeer(device))
        }
    }
    fun addPhysicalPeer(peer: PhysicalPeer) {
        if (physicalPeerList.none { it.device.address == peer.device.address }) {
            physicalPeerList.add(peer)
        }
    }
    fun removePhysicalPeer(device: BluetoothDevice) {
        physicalPeerList.removeAll { it.device.address == device.address }
    }
    fun getPhysicalPeerList(): List<PhysicalPeer> = physicalPeerList

    fun isInMesh() = isInMesh
    fun updateIsInMesh(newState: Boolean) { isInMesh = newState }
}