package com.meshenger.backend.transport2

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StaticKeyManagerIntegrationTest {

    @Test
    fun x25519KeyPair_wrapUnwrap_roundTrip() {
        val (_, privateKey) = StaticKeyManager.generateX25519KeyPair()
        val (ciphertext, iv) = StaticKeyManager.wrapKey(privateKey)
        val unwrapped = StaticKeyManager.unwrapKey(ciphertext, iv)
        assertArrayEquals(privateKey, unwrapped)
    }

    @Test
    fun x25519Key_base64RoundTrip() {
        val (publicKey, _) = StaticKeyManager.generateX25519RandomKeyPair()
        val encoded = StaticKeyManager.X25519KeyByteArrayToString(publicKey)
        val decoded = StaticKeyManager.X25519KeyStringToByteArray(encoded)
        assertArrayEquals(publicKey, decoded)
    }

    @Test
    fun identityKey_isStableAcrossCalls() {
        val first = StaticKeyManager.getOrCreateIdentityKey()
        val second = StaticKeyManager.getOrCreateIdentityKey()
        assertArrayEquals(first.public.encoded, second.public.encoded)
        assertNotNull(first.private)
    }

    @Test
    fun rawIdentityPublicKey_decodeAndExtract() {
        val keyPair = StaticKeyManager.getOrCreateIdentityKey()
        val raw = StaticKeyManager.getRawPublicIdentityKey(keyPair.public)
        val decoded = StaticKeyManager.decodeRawIdentityPublicKey(raw)
        assertEquals(
            StaticKeyManager.getStringPublicIdentityKey(raw),
            StaticKeyManager.getStringPublicIdentityKey(
                StaticKeyManager.getRawPublicIdentityKey(decoded),
            ),
        )
    }
}
