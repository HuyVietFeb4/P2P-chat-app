import android.bluetooth.BluetoothDevice
import com.meshenger.backend.transport.PhysicalPeer
import com.meshenger.backend.transport.client.BleClientConnection
import com.meshenger.backend.transport.server.BleServer
import com.meshenger.backend.transport.server.BleServerConnection
import kotlin.collections.mutableMapOf

object MeshConnectionRegistry {
    private var isInMesh = false
    private val inboundMap = mutableMapOf<String, BleServerConnection>()
    private val outboundMap = mutableMapOf<String, BleClientConnection>()

    private val physicalPeerList = mutableListOf<PhysicalPeer>()

    fun getAllPeers(): Set<String> = inboundMap.keys + outboundMap.keys
    fun getCountConnections(): Int = inboundMap.size + outboundMap.size

    fun getInboundMap(): Map<String, BleServerConnection> {
        return inboundMap
    }
    fun addInbound(bleAddress: String, serverConnection: BleServerConnection) {
        inboundMap[bleAddress] = serverConnection
    }
    fun removeInbound(bleAddress: String) {
        inboundMap.remove(bleAddress)
    }
    fun getInbound(bleAddress: String): BleServerConnection? {
        return inboundMap[bleAddress]
    }

    fun getOutboundMap(): Map<String, BleClientConnection> {
        return outboundMap
    }
    fun addOutBound(bleAddress: String, serverConnection: BleClientConnection) {
        outboundMap[bleAddress] = serverConnection
    }
    fun removeOutbound(bleAddress: String) {
        outboundMap.remove(bleAddress)
    }
    fun getOutbound(bleAddress: String): BleClientConnection? {
        return outboundMap[bleAddress]
    }

    fun getPhysicalPeerList(): List<PhysicalPeer> {
        return physicalPeerList
    }
    fun addPhysicalPeer(device: BluetoothDevice) {
        physicalPeerList.add(PhysicalPeer(device))
    }
    fun removePhysicalPeer(device: BluetoothDevice) {
        physicalPeerList.removeAll { it.device.address == device.address }
    }

    fun isInMesh(): Boolean {
        return isInMesh
    }

    fun updateIsInMesh(newState: Boolean) {
        isInMesh = newState
    }
}