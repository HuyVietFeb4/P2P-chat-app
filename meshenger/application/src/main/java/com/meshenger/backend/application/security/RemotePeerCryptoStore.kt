package com.meshenger.backend.application.security

import android.util.Base64
import com.meshenger.backend.application.db.MeshengerDbHelper
import com.meshenger.backend.transport2.StaticKeyManager
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores remote peers' raw X25519 / Ed25519 key material at rest:
 * 1) Generate an AES master with [StaticKeyManager.generateSoftwareMasterKey]
 * 2) Encrypt raw key with AES-GCM
 * 3) Persist ciphertext + IV in [MeshengerDbHelper] (`peer_remote_keys`)
 * 4) Derive a stable Keystore alias via SHA-256
 * 5) Import the master bytes with [StaticKeyManager.importSecretKeyToKeystore]
 */
class RemotePeerCryptoStore(private val dbHelper: MeshengerDbHelper) {

    companion object {
        const val KEY_TYPE_ED25519_RAW = "ED25519_RAW"
        const val KEY_TYPE_X25519_RAW = "X25519_RAW"
        /**
         * Peer's long-term Noise static public (32-byte X25519) learned **only** from a QR scan,
         * before any completed handshake. Used to run Noise **XK** (scanner = initiator). After
         * a successful handshake this row is deleted and replaced by [KEY_TYPE_X25519_RAW].
         */
        const val KEY_TYPE_X25519_QR_IMPORT = "X25519_QR_IMPORT"
        // Used for storing this device's own X25519 static *private* half. Encrypted at rest the
        // same way as remote keys (AES-GCM via Keystore), so the table stays a single source of
        // truth for raw key material.
        const val KEY_TYPE_X25519_PRIV = "X25519_PRIV"

        private const val AES_GCM = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128
        private const val ALIAS_SEED = "meshenger.peer_wrap.v1|"

        fun allowedKeyTypes(): Set<String> =
            setOf(
                KEY_TYPE_ED25519_RAW,
                KEY_TYPE_X25519_RAW,
                KEY_TYPE_X25519_PRIV,
                KEY_TYPE_X25519_QR_IMPORT,
            )
    }

    fun buildKeystoreAlias(peerUserId: String, keyType: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(
            "$ALIAS_SEED$peerUserId|$keyType".encodeToByteArray(),
        )
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun saveRemoteRawKey(peerUserId: String, keyType: String, rawKeyMaterial: ByteArray) {
        val alias = buildKeystoreAlias(peerUserId, keyType)
        removeKeystoreAlias(alias)

        val masterKey = StaticKeyManager.generateSoftwareMasterKey()
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.ENCRYPT_MODE, masterKey)
        val ciphertext = cipher.doFinal(rawKeyMaterial)
        val iv = cipher.iv

        StaticKeyManager.importSecretKeyToKeystore(alias, masterKey.encoded)

        dbHelper.upsertPeerRemoteKey(
            peerUserId = peerUserId,
            keyType = keyType,
            keystoreAlias = alias,
            ciphertextBlob = Base64.encodeToString(ciphertext, Base64.NO_WRAP),
            ivBlob = Base64.encodeToString(iv, Base64.NO_WRAP),
        )
    }

    fun loadRemoteRawKey(peerUserId: String, keyType: String): ByteArray? {
        val row = dbHelper.getPeerRemoteKey(peerUserId, keyType) ?: return null
        val masterKey = StaticKeyManager.getSecretKeyFromKeystore(row.keystoreAlias) ?: return null
        val iv = Base64.decode(row.ivBlob, Base64.NO_WRAP)
        val ciphertext = Base64.decode(row.ciphertextBlob, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.DECRYPT_MODE, masterKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    fun deleteRemoteRawKey(peerUserId: String, keyType: String) {
        val row = dbHelper.getPeerRemoteKey(peerUserId, keyType) ?: return
        removeKeystoreAlias(row.keystoreAlias)
        dbHelper.deletePeerRemoteKey(peerUserId, keyType)
    }

    private fun removeKeystoreAlias(alias: String) {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (ks.containsAlias(alias)) {
            ks.deleteEntry(alias)
        }
    }
}
