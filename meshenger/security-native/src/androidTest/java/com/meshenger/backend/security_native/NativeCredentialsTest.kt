package com.meshenger.backend.security_native

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import android.util.Log

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class NativeCredentialsTest {
    @Test
    fun testNativeKey() {
        val TAG = "NativeCredentialsTest"
        val nativeCredentialsString = NativeCredentials.getAppSecretKey()
        val appSecretKeyStrLit = "Meshenger_Secret_All_Chat_Key"

        val mask = 0x42
        val encryptedKey = appSecretKeyStrLit.map { char ->
            (char.code xor mask).toChar()
        }.joinToString("")
        Log.d(TAG, "Expected Key: $encryptedKey")
        Log.d(TAG, "Actual Key from Native: $nativeCredentialsString")
        assertEquals(nativeCredentialsString, encryptedKey)
    }
}