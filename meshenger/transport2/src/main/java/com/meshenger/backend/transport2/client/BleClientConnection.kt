package com.meshenger.backend.transport2.client

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.util.Log
import com.meshenger.backend.transport2.BleUUIDConstants
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.data.Data
import no.nordicsemi.android.ble.ktx.suspend
import no.nordicsemi.android.ble.observer.ConnectionObserver
//  A client connection initiated that connect to some server
class BleClientConnection(context: Context): BleManager(context), ConnectionObserver {
    private var writeChar: BluetoothGattCharacteristic? = null
    private val writeMutex = Mutex()
    var onDataReceived: ((device: BluetoothDevice, data: ByteArray) -> Unit)? = null
    init {
        setConnectionObserver(this)
    }
    override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
        val service = gatt.getService(BleUUIDConstants.MESH_SERVICE_UUID)
        writeChar = service?.getCharacteristic(BleUUIDConstants.CHARACTERISTIC_DATA_WRITE_UUID)
        Log.d("BleClientConnection", "Service Discovery: WriteChar is ${if (writeChar != null) "FOUND" else "MISSING"}")
        return writeChar != null
    }
    override fun initialize() {
        requestMtu(517)
            .done { Log.d("BleClientConnection", "MTU Negotiated") }
            .fail { _, status -> Log.w("BleClientConnection", "MTU Failed: $status") }
            .enqueue()
    }
    override fun onServicesInvalidated() {
        writeChar = null
    }

    override fun onDeviceConnecting(device: BluetoothDevice) {
        Log.d("BleClientConnection", "Connecting to ${device.address}...")
    }

    var onDisconnected: ((address: String) -> Unit)? = null
    override fun onDeviceConnected(device: BluetoothDevice) {
        Log.i("BleClientConnection", "GATT link established with ${device.address}")
    }

    override fun onDeviceFailedToConnect(device: BluetoothDevice, reason: Int) {
        Log.e("BleClientConnection", "Failed to connect to ${device.address}. Code: $reason")
    }

    override fun onDeviceReady(device: BluetoothDevice) {
        Log.i("BleClientConnection", "Device ${device.address} is READY for Mesh communication")
    }

    override fun onDeviceDisconnecting(device: BluetoothDevice) {
        Log.d("BleClientConnection", "Disconnecting from ${device.address}...")
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
        Log.e("BleClientConnection", "Disconnected from ${device.address}. Reason: $reasonString")
        onDisconnected?.invoke(device.address)
    }

    override fun log(priority: Int, message: String) {
        Log.println(priority, "BleClientConnection", message)
    }

    fun sendMessageToServerStr(message: String) {
        val data = Data.from(message)
        if(writeChar != null) {
            writeCharacteristic(
                writeChar,
                data,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            )
                .split()
                .done { device ->
                    Log.d("BleClientConnection", "Data: ${message} successfully written to: ${device.name}")
                }
                .fail { device, status ->
                    Log.d("BleClientConnection", "Failed to write characteristic: ${status}")
                }
                .enqueue()

        } else {
            Log.d("BleClientConnection", "Write characteristic not available")
        }
    }

    fun sendPacketToServer(packet: ByteArray) {
        if(writeChar != null) {
            writeCharacteristic(
                writeChar,
                packet,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            )
                .split()
                .done { device ->
                    Log.d("BleClientConnection", "Data successfully written to: ${device.name}")
                }
                .fail { device, status ->
                    Log.d("BleClientConnection", "Failed to write characteristic: ${status}")
                }
                .enqueue()

        } else {
            Log.d("BleClientConnection", "Write characteristic not available")
        }
    }
    suspend fun sendPacketToServerSuspending(packet: ByteArray) {
        if (writeChar == null) {
            Log.w("BleClientConnection", "Write characteristic not available")
            return
        }

        writeMutex.withLock {
            try {
                writeCharacteristic(
                    writeChar,
                    packet,
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                )
                    .split() // Ensures MTU limits are respected automatically
                    .suspend()

                Log.d("BleClientConnection", "Data successfully written via Suspend")
            } catch (e: Exception) {
                Log.e("BleClientConnection", "Failed to write: ${e.message}")
            }
        }
    }
}