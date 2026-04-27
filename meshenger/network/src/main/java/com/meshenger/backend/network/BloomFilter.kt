package com.meshenger.backend.network

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.security.Signature
import java.util.BitSet
import kotlin.math.absoluteValue
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

open class BloomFilter(val capacity: Int = 600, val errorRate: Double = 0.05) {
    protected val size: Int = (- (capacity * ln(errorRate)) / ln(2.0).pow(2.0)).toInt()
    protected val hashCount: Int = max(1, ( (size.toDouble() / capacity) * ln(2.0) ).toInt())
    protected var bitArray = BitSet(size)
    protected var count: Int = 0
    open fun getHashes(signature: ByteArray): Sequence<Int> = sequence {
        val md = MessageDigest.getInstance("SHA-256")

        for (i in 0 until hashCount) {
            md.update(signature)
            md.update(i.toString().encodeToByteArray())
            val hash = md.digest().toPositiveBigInt()
            yield((hash % size.toLong()).toInt())
        }
    }

    fun add(signature: ByteArray) {
        getHashes(signature).forEach { bitArray.set(it) }
        count++
        if(count == capacity) {
            count = 0;
            bitArray.clear()
        }
    }

    open fun isAvailable(signature: ByteArray): Boolean {
        return getHashes(signature).all { bitArray.get(it) }
    }

    fun resetBitArray() {
        bitArray.clear()
    }

    // Helper for large hashes
    protected fun ByteArray.toPositiveBigInt(): Long {
        var result = 0L
        for (i in 0 until min(8, this.size)) {
            result = (result shl 8) or (this[i].toLong() and 0xFF)
        }
        return result and Long.MAX_VALUE
    }
}

class KMBloomFilter(capacity: Int, errorRate: Double = 0.01) : BloomFilter(capacity, errorRate) {
    override fun getHashes(signature: ByteArray): Sequence<Int> = sequence {
        val h1 = MessageDigest.getInstance("MD5").digest(signature).toPositiveBigInt()
        val h2 = MessageDigest.getInstance("SHA-1").digest(signature).toPositiveBigInt()

        for (i in 0 until hashCount) {
            val pos = (h1 + i.toLong() * h2) % size.toLong()
            yield(pos.toInt().absoluteValue)
        }
    }
}

open class CompactRefinedBloomFilter(capacity: Int, errorRate: Double = 0.01) : BloomFilter(capacity, errorRate) {
    fun toCompactedBinary(): ByteArray {
        val blockSize = size / hashCount
        val totalBits = blockSize * hashCount
        val byteArray = ByteArray((totalBits + 7) / 8)

        var globalBitCursor = 0

        for (i in 0 until blockSize) {
            // 1. Extract the k-bit pattern for this index
            var patternVal = 0
            for (j in 0 until hashCount) {
                if (bitArray.get(j * blockSize + i)) {
                    patternVal = patternVal or (1 shl j)
                }
            }

            // 2. Pack the k bits of patternVal into the ByteArray
            for (bitIdx in 0 until hashCount) {
                val isSet = (patternVal and (1 shl bitIdx)) != 0
                if (isSet) {
                    val bytePos = globalBitCursor / 8
                    val bitPos = globalBitCursor % 8
                    // Set the specific bit in the byte array
                    byteArray[bytePos] = (byteArray[bytePos].toInt() or (1 shl bitPos)).toByte()
                }
                globalBitCursor++
            }
        }
        return byteArray
    }

    fun fromCompactedBinary(data: ByteArray) {
        val blockSize = size / hashCount
        bitArray.clear()

        var globalBitCursor = 0

        for (i in 0 until blockSize) {
            var patternVal = 0
            // 1. Read k bits from the byte array to reconstruct the pattern
            for (bitIdx in 0 until hashCount) {
                val bytePos = globalBitCursor / 8
                val bitPos = globalBitCursor % 8

                val isSet = (data[bytePos].toInt() and (1 shl bitPos)) != 0
                if (isSet) {
                    patternVal = patternVal or (1 shl bitIdx)
                }
                globalBitCursor++
            }

            // 2. Map the pattern back to the blocks in the BitSet
            for (j in 0 until hashCount) {
                if ((patternVal and (1 shl j)) != 0) {
                    bitArray.set(j * blockSize + i)
                }
            }
        }
    }
}

class KMCompactRefinedBloomFilter(
    capacity: Int = 10000,
    errorRate: Double = 0.01
) : CompactRefinedBloomFilter(capacity, errorRate) {
    override fun getHashes(signature: ByteArray): Sequence<Int> = sequence {
        // Use MD5 and SHA-1 to get two independent hash integers (as in your Python code)
        val h1 = MessageDigest.getInstance("MD5").digest(signature).toPositiveLong()
        val h2 = MessageDigest.getInstance("SHA-1").digest(signature).toPositiveLong()

        for (i in 0 until hashCount) {
            // (h1 + i * h2) % size
            val pos = (h1 + i.toLong() * h2) % size.toLong()
            yield(pos.toInt().absoluteValue)
        }
    }

    // Helper to turn byte arrays into positive Longs for the math
    private fun ByteArray.toPositiveLong(): Long {
        var result = 0L
        for (i in 0 until minOf(8, this.size)) {
            result = (result shl 8) or (this[i].toLong() and 0xFF)
        }
        return result and Long.MAX_VALUE
    }
}