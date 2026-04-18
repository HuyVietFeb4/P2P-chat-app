package com.meshenger.backend.transport.server

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.content.Context
import android.util.Log
import com.meshenger.backend.transport.BleUUIDConstants
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.data.Data
import no.nordicsemi.android.ble.ktx.suspend
import no.nordicsemi.android.ble.data.DataSplitter
import no.nordicsemi.android.ble.observer.ConnectionObserver
//  A server connection by some client to this server
class BleServerConnection(context: Context): BleManager(context), ConnectionObserver {
    private var server: BleServer? = null
    private var writeChar: BluetoothGattCharacteristic? = null
    private var notifyChar: BluetoothGattCharacteristic? = null
    var onDataReceived: ((BluetoothDevice, ByteArray) -> Unit)? = null
    init {
        setConnectionObserver(this)
    }
    fun setLocalServer(server: BleServer) {
        this.server = server
    }
    override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
        val myLocalServer = this.server

        if (myLocalServer != null) {
            this.writeChar = myLocalServer.getWriteChar()
            this.notifyChar = myLocalServer.getNotifyChar()

            // CRITICAL: If we are re-discovering after an invalidation,
            // we need to make sure we are ready to send again.
            if (this.notifyChar != null) {
                // Check if notifications were already enabled previously
                // or let initialize() handle it. For safety, re-arm here:
                Log.d("BleServerConnection", "Re-bound to LOCAL server characteristics")
            }
        } else {
            Log.e("BleServerConnection", "Error: Server reference is null!")
        }

        // Always return true. We are the Server; we define the rules.
        return true
    }

    override fun onServerReady(server: BluetoothGattServer) {
        server.getService(BleUUIDConstants.MESH_SERVICE_UUID)?.let { service ->
            writeChar = service.getCharacteristic(BleUUIDConstants.CHARACTERISTIC_DATA_WRITE_UUID)
            notifyChar = service.getCharacteristic(BleUUIDConstants.CHARACTERISTIC_DATA_NOTIFY_UUID)
        }
    }
    override fun initialize() {
        requestMtu(256).enqueue()
        setWriteCallback(writeChar)
            .with { device, data ->
                val bytes = data.value ?: byteArrayOf()
                onDataReceived?.invoke(device, bytes)
            }
        waitUntilNotificationsEnabled(notifyChar)
            .done { device ->
                Log.d("BleServerConnection", "Client ${device.address} has enabled notifications. Ready to push!")
            }
            .fail { device, status ->
                Log.e("BleServerConnection", "Client failed to enable notifications: $status")
            }
            .enqueue()
    }

    override fun onServicesInvalidated() {
        this.writeChar = null
        this.notifyChar = null
    }

    override fun onDeviceConnecting(device: BluetoothDevice) {
        Log.d("BleServerConnection", "Connecting to ${device.address}...")
    }

    override fun onDeviceConnected(device: BluetoothDevice) {
        Log.i("BleServerConnection", "GATT link established with ${device.address}")
    }

    override fun onDeviceFailedToConnect(device: BluetoothDevice, reason: Int) {
        Log.e("BleServerConnection", "Failed to connect to ${device.address}. Code: $reason")
    }

    override fun onDeviceReady(device: BluetoothDevice) {
        Log.i("BleServerConnection", "Device ${device.address} is READY for Mesh communication")
    }

    override fun onDeviceDisconnecting(device: BluetoothDevice) {
        Log.d("BleServerConnection", "Disconnecting from ${device.address}...")
    }

    override fun onDeviceDisconnected(device: BluetoothDevice, reason: Int) {
        val reasonString = when(reason) {
            -1 -> "REASON_UNKNOWN" // Nordic internal constant
            0 -> "REASON_SUCCESS"
            1 -> "REASON_TERMINATED_BY_USER"
            2 -> "REASON_TERMINATED_BY_REMOTE"
            3 -> "REASON_LINK_LOSS"
            4 -> "REASON_NOT_SUPPORTED"
            5 -> "REASON_TIMEOUT"
            else -> "GATT Error Code: $reason"
        }
        Log.e("BleServerConnection", "Disconnected from ${device.address}. Reason: $reasonString")

    }

    override fun log(priority: Int, message: String) {
        Log.println(priority, "BleServerConnection", message)
    }

    fun sendMessageToClientStr(msg: String) {
        val data = Data.from(msg)
        if(notifyChar != null) {
            sendNotification(notifyChar, data)
                .split()
                .done { device ->
                    Log.d("BleServerConnection", "Data: ${msg} successfully notify to: ${device.name} ")
                }
                .fail { device, status ->
                    Log.d("BleServerConnection", "Failed to notify characteristic: ${status}")
                }
                .enqueue()
        } else {
            Log.d("BleServerConnection", "Notify characteristic not available")
        }
    }
    fun sendPacketToClient(packet: ByteArray) {
        Log.d("BleServerConnection", "Attempting to send packet to client: ${packet.size} bytes")
        if(notifyChar != null) {
            sendNotification(notifyChar, packet)
                .done { device ->
                    Log.d("BleServerConnection", "Packet sent in chunks successfully to ${device.address}!")
                }
                .fail { device, status ->
                    Log.e("BleServerConnection", "Failed to send to ${device.address}. Status: $status")
                }
                .enqueue()
        } else {
            Log.d("BleServerConnection", "Notify characteristic not available")
        }
    }

    suspend fun sendPacketToClientSuspending(packet: ByteArray) {
        if(notifyChar != null) {
            try {
                sendNotification(notifyChar, packet)
                    .split()
                    .suspend()
                Log.d("BleServerConnection", "Successfully sent 512 bytes via Suspend")
            } catch (e: Exception) {
                Log.e("BleServerConnection", "Suspend send failed: ${e}")
            }
        }
    }
}