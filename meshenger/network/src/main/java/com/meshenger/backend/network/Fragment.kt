package com.meshenger.backend.network

import java.io.ByteArrayOutputStream

object FragmentUtil {
    fun toFragments(rawData: ByteArray): List<ByteArray> {
        //ceil(rawData.size / BLEProtocolConfig.BLE_MAX_SIZE)
        val totalFragments = ((rawData.size + BLEProtocolConfig.MAX_PAYLOAD_LENGTH - 1) / BLEProtocolConfig.MAX_PAYLOAD_LENGTH).toUShort()
        val fragmentLst = mutableListOf<ByteArray>()
        for(i in 0 until totalFragments.toInt()) {
            val start = i * BLEProtocolConfig.MAX_PAYLOAD_LENGTH
            val end = minOf(start + BLEProtocolConfig.MAX_PAYLOAD_LENGTH, rawData.size)
            val chunk = rawData.copyOfRange(start, end)
            fragmentLst.add(chunk)
        }
        return fragmentLst
    }
    fun reassembly(fragments: List<ByteArray>): ByteArray {
        val buffer = ByteArrayOutputStream()
        for (fragment in fragments) {
            buffer.write(fragment)
        }
        return buffer.toByteArray()
    }
}