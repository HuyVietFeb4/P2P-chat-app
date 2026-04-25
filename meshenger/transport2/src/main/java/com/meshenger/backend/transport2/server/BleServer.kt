package com.meshenger.backend.transport2.server

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.util.Log
import com.facebook.infer.annotation.FalseOnNull
import com.meshenger.backend.transport2.BleLimitConstants
import com.meshenger.backend.transport2.BleUUIDConstants
import com.meshenger.backend.transport2.PhysicalPeer
import com.meshenger.backend.transport2.TransportPacketListener
import com.meshenger.backend.transport2.MeshConnectionRegistry
import com.meshenger.backend.transport2.client.BleClientConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.nordicsemi.android.ble.BleServerManager
import no.nordicsemi.android.ble.data.Data
import no.nordicsemi.android.ble.observer.ServerObserver
import kotlin.collections.set

class BleServer(context: Context): BleServerManager(context), ServerObserver {
    private var appContext: Context = context
    private var writeChar: BluetoothGattCharacteristic? = null
    private var notifyChar: BluetoothGattCharacteristic? = null
    private var isRunning: Boolean = false
    private var packetListener: TransportPacketListener? = null
    fun setListener(listener: TransportPacketListener) {
        this.packetListener = listener
    }

    override fun log(priority: Int, message: String) {
        Log.println(priority, "BleServer", message)
    }

    override fun initializeServer(): List<BluetoothGattService> {
        writeChar = characteristic(
            BleUUIDConstants.CHARACTERISTIC_DATA_WRITE_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        notifyChar = characteristic(
            BleUUIDConstants.CHARACTERISTIC_DATA_NOTIFY_UUID,
            BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ,
            // Explicitly define the CCCD with both Read and Write permissions
            cccd(),
            description("Server to Client Push Pipe", false)
        )

        val chatService = BluetoothGattService(
            BleUUIDConstants.MESH_SERVICE_UUID,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )
        chatService.addCharacteristic(writeChar)
        chatService.addCharacteristic(notifyChar)
        Log.d("BleServer", "GATT Services built for MESH_SERVICE_UUID")
        setServerObserver(this)
        isRunning = true
        return listOf(chatService)
    }

    override fun onServerReady() {
        Log.d("BleServer", "Server is ready to accept connections")
    }

    override fun onDeviceConnectedToServer(device: BluetoothDevice) {
        if(!MeshConnectionRegistry.getInboundMap().containsKey(device.address)
            && MeshConnectionRegistry.getCountConnections() < BleLimitConstants.MAX_CONNECTIONS_LIMIT) {
            // For recieving packet
            val server = BleServerConnection(appContext)
            server.onDataReceived = { sender, packet -> // entry point of receiving incoming packet
                CoroutineScope(Dispatchers.Default).launch {
                    Log.d("BleServer", "Received packet from ${sender.address} (Inbound)")
                    packetListener?.onRecievePacket(packet, sender.address)
                }
            }
            server.useServer(this)
            server.connect(device)
                .retry(3, 100)
                .useAutoConnect(false)
                .done { device ->
                    // 3. Only add to Registry when the Handshake (MTU, etc.) is DONE
                    MeshConnectionRegistry.addInbound(device.address, server)
                    Log.d("BleServer", "Device $device.address is now READY and REGISTERED")
                }
                .fail { _, status ->
                    Log.e("BleServer", "Failed to 'attach' to $device.address: status $status")
                    cancelConnection(device) // Hard disconnect if handshake fails
                }
                .enqueue()
            Log.d("BleServer", "Device ${device.address} has connected")

            // Reconnect back for dual write
            val address = device.address
            MeshConnectionRegistry.markPending(address)

            val client = BleClientConnection(appContext)

            client.onDisconnected = { addr ->
                MeshConnectionRegistry.removeOutbound(addr)
                MeshConnectionRegistry.unmarkPending(addr)
                client.close()
            }

            client.connect(device)
                .retry(2, 1000)
                .useAutoConnect(false)
                .done {
                    Log.i("BleServer", "Connected to $address")
                    MeshConnectionRegistry.addOutBound(address, client)
                }
                .fail { _, status ->
                    Log.e("BleServer", "Failed $address: $status")
                    MeshConnectionRegistry.unmarkPending(address)
                }
                .enqueue()
        } else {
            Log.w("BleServer", "Rejecting connection from ${device.address}. Criteria not met.")
            cancelConnection(device)
        }

    }

    override fun onDeviceDisconnectedFromServer(device: BluetoothDevice) {
        Log.e("BleServer", "Device ${device.address} disconnected.")
        val connection = MeshConnectionRegistry.getInbound(device.address)
        connection?.close()
        MeshConnectionRegistry.removeInbound(device.address)
        MeshConnectionRegistry.removePhysicalPeer(device)
        Log.i("BleServer", "Server resources for ${device.address} released.")
    }

    fun shutDownServer() {
        val inboundConnections = MeshConnectionRegistry.getInboundMap()
        inboundConnections.forEach { (address, connection) ->
            Log.d("BleServer", "Closing connection for $address")
            connection.close()
            MeshConnectionRegistry.removeInbound(address)
        }
        close()
        isRunning = false
        Log.i("BleServer", "Server has been fully shut down and resources released.")
    }
    fun isServerActive(): Boolean {
        return isRunning
    }
    fun getWriteChar(): BluetoothGattCharacteristic? {
        return this.writeChar
    }
    fun getNotifyChar(): BluetoothGattCharacteristic? {
        return this.notifyChar
    }
}