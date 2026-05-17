package com.meshenger.backend.application.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.meshenger.backend.application.db.MeshengerDbHelper
import com.meshenger.backend.application.user.UserProfile
import com.meshenger.backend.transport2.StaticKeyManager
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RemotePeerCryptoStoreIntegrationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var dbHelper: MeshengerDbHelper
    private lateinit var store: RemotePeerCryptoStore

    @Before
    fun setUp() {
        context.deleteDatabase("meshenger.db")
        dbHelper = MeshengerDbHelper(context)
        dbHelper.upsertUserProfile(UserProfile("local", "-", "Me"))
        store = RemotePeerCryptoStore(dbHelper)
    }

    @After
    fun tearDown() {
        dbHelper.close()
    }

    @Test
    fun saveAndLoad_x25519RawKey_roundTrip() {
        val peerId = "peer-crypto-1"
        val (publicKey, _) = StaticKeyManager.generateX25519KeyPair()

        store.saveRemoteRawKey(peerId, RemotePeerCryptoStore.KEY_TYPE_X25519_RAW, publicKey)
        val loaded = store.loadRemoteRawKey(peerId, RemotePeerCryptoStore.KEY_TYPE_X25519_RAW)

        assertArrayEquals(publicKey, loaded)
    }

    @Test
    fun deleteRemoteRawKey_removesFromDbAndKeystore() {
        val peerId = "peer-crypto-2"
        val (_, privateKey) = StaticKeyManager.generateX25519KeyPair()
        store.saveRemoteRawKey(peerId, RemotePeerCryptoStore.KEY_TYPE_X25519_PRIV, privateKey)

        store.deleteRemoteRawKey(peerId, RemotePeerCryptoStore.KEY_TYPE_X25519_PRIV)

        assertNull(store.loadRemoteRawKey(peerId, RemotePeerCryptoStore.KEY_TYPE_X25519_PRIV))
        assertNull(dbHelper.getPeerRemoteKey(peerId, RemotePeerCryptoStore.KEY_TYPE_X25519_PRIV))
    }
}
