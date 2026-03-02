package com.meshenger.backend.network

import java.io.ByteArrayOutputStream

object FragmentUtil {
    fun toFragments(rawData: ByteArray): List<ByteArray> {
        //ceil(rawData.size / bleMaxSize)
        val totalFragments = ((rawData.size + maxPayloadLength - 1) / maxPayloadLength).toUShort()
        val fragmentLst = mutableListOf<ByteArray>()
        for(i in 0 until totalFragments.toInt()) {
            val start = i * maxPayloadLength
            val end = minOf(start + maxPayloadLength, rawData.size)
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