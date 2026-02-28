package com.meshenger.backend.network
import java.security.SecureRandom
import kotlin.math.min

// Could be use to prevent traffic analysis. Will be tested to see if the benefit of hiding traffic analysis out-weight network optimization

object PaddingUtil {
    fun pad(rawData: ByteArray, targetSize: Int): ByteArray { // Randomized padding to full targetSize (could be maxPayloadLengh: 396)
        if(rawData.size >= targetSize) return rawData
        val paddedData = ByteArray(targetSize)
        System.arraycopy(rawData, 0, paddedData, 0, rawData.size)
        val randomPadding = SecureRandom().generateSeed(maxPayloadLength - rawData.size)
        System.arraycopy(randomPadding, 0, paddedData, rawData.size, randomPadding.size)
        return paddedData
    }
    
    fun prepareAlignedPayload(rawData: ByteArray): ByteArray {
        val rawSize = rawData.size
        val alignedSize = (rawSize + 3) and 3.inv() // bit manipulation trick
        val finalSize = min(alignedSize, maxPayloadLength)
        if (finalSize == rawSize) return rawData
        val alignedArray = ByteArray(finalSize)
        System.arraycopy(rawData, 0, alignedArray, 0, currentSize)
        return alignedArray
    }

    fun unpad(data: ByteArray): ByteArray {

    }
}