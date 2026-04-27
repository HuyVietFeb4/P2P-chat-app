package com.meshenger.backend.transport2
import android.bluetooth.BluetoothDevice

data class PhysicalPeer (
    val device: BluetoothDevice,
    var MPAddress: ByteArray,
    var isInMesh: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        // 1. Referential Equality (Check if the same memory address)
        if (this === other) return true

        // 2. Type Check
        if (other !is PhysicalPeer) return false
        return this.device.address == other.device.address
    }

    override fun hashCode(): Int {
        return device.address.hashCode()
    }
}