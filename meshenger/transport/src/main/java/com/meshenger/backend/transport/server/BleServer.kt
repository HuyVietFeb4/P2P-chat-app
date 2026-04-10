package com.meshenger.backend.transport.server

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.util.Log
import com.facebook.infer.annotation.FalseOnNull
import com.meshenger.backend.transport.PhysicalPeer
import com.meshenger.backend.transport.client.BleScanner
import no.nordicsemi.android.ble.BleServerManager
import no.nordicsemi.android.ble.data.Data
import no.nordicsemi.android.ble.observer.ServerObserver
import kotlin.collections.set

class BleServer(context: Context): BleServerManager(context), ServerObserver {
    private var appContext: Context = context
    private var writeChar: BluetoothGattCharacteristic? = null
    private var notifyChar: BluetoothGattCharacteristic? = null
    private var isRunning: Boolean = false
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
            BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ,
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
            MeshConnectionRegistry.addPhysicalPeer(device)
            val client = BleServerConnection(appContext)
            client.useServer(this)
            client.connect(device).enqueue()
            MeshConnectionRegistry.addInbound(device.address, client)
            Log.d("BleServer", "Device ${device.address} has connected")
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
}