package com.meshenger.backend.network

import java.io.ByteArrayOutputStream

object FragmentUtil {
    fun toFragments(rawData: ByteArray): List<ByteArray> {
        //ceil(rawData.size / PacketLimitConfig.BLE_MAX_SIZE)
        val totalFragments = ((rawData.size + PacketLimitConfig.MAX_PAYLOAD_LENGTH - 1) / PacketLimitConfig.MAX_PAYLOAD_LENGTH).toUShort()
        val fragmentLst = mutableListOf<ByteArray>()
        for(i in 0 until totalFragments.toInt()) {
            val start = i * PacketLimitConfig.MAX_PAYLOAD_LENGTH
            val end = minOf(start + PacketLimitConfig.MAX_PAYLOAD_LENGTH, rawData.size)
            val chunk = rawData.copyOfRange(start, end)
            fragmentLst.add(chunk)
        }
        return fragmentLst
    }
    
    fun reassembly(fragments: List<ByteArray>): ByteArray {
        // 1. Calculate total size once to avoid multiple re-allocations
        val totalSize = fragments.sumOf { it.size }
        val result = ByteArray(totalSize)

        // 2. Copy chunks into the final array
        var currentOffset = 0
        for (fragment in fragments) {
            // System.arraycopy (via copyInto) is extremely fast in Kotlin/JVM
            fragment.copyInto(result, destinationOffset = currentOffset)
            currentOffset += fragment.size
        }

        return result
    }
}