package com.meshenger.backend.transport2.server

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.content.Context
import android.util.Log
import com.meshenger.backend.transport2.BleUUIDConstants
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.data.Data
import no.nordicsemi.android.ble.ktx.suspend
import no.nordicsemi.android.ble.data.DataSplitter
import no.nordicsemi.android.ble.observer.ConnectionObserver
//  A server connection by some client to this server
class BleServerConnection(context: Context): BleManager(context), ConnectionObserver {
    private var writeChar: BluetoothGattCharacteristic? = null
    var onDataReceived: ((BluetoothDevice, ByteArray) -> Unit)? = null
    init {
        setConnectionObserver(this)
    }

    override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
        val service = gatt.getService(BleUUIDConstants.MESH_SERVICE_UUID)
        if (service != null) {
            writeChar = service.getCharacteristic(BleUUIDConstants.CHARACTERISTIC_DATA_WRITE_UUID)
        }
        return true
    }

    override fun onServerReady(server: BluetoothGattServer) {
        server.getService(BleUUIDConstants.MESH_SERVICE_UUID)?.let { service ->
            writeChar = service.getCharacteristic(BleUUIDConstants.CHARACTERISTIC_DATA_WRITE_UUID)
        }
    }
    override fun initialize() {
        requestMtu(256).enqueue()
        setWriteCallback(writeChar)
            .with { device, data ->
                val bytes = data.value ?: byteArrayOf()
                onDataReceived?.invoke(device, bytes)
            }
    }

    override fun onServicesInvalidated() {
        this.writeChar = null
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

}