package com.meshenger.backend.transport

import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.graphics.MeshSpecification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.meshenger.backend.transport.client.BleClientConnection
import com.meshenger.backend.transport.client.BleScanner
import com.meshenger.backend.transport.server.BleAdvertiser
import com.meshenger.backend.transport.server.BleServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI

class MeshMaintainer : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var isRunning = false

    private lateinit var server: BleServer
    private lateinit var appContext: Context
    private lateinit var scanner: BleScanner
    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        server = BleServer(applicationContext)
        scanner = BleScanner()
        globalPacketListener?.let {
            server.setListener(it)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(!isRunning) {
            if(!BleAdvertiser.isAdvertisingActive()) BleAdvertiser.onBackgroundAdvertise()
            setupForeground()
            startMeshMaintainerLoop()
            isRunning = true
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d("MeshMaintainer", "App swiped away. Stopping service...")

        // Perform any urgent cleanup here
        // Note: You have very limited time before the process is killed

        stopSelf() // Tells the system to stop this service
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MeshMaintainer", "Service onDestroy called. Cancelling loop...")

        // 1. Stop the Coroutine Loop
        serviceScope.cancel()

        // 2. Shut down hardware resources
        if(server.isServerActive()) server.shutDownServer()
        if(BleAdvertiser.isAdvertisingActive()) BleAdvertiser.stopAdvertising()

        // 3. Clear all active GATT connections
        MeshConnectionRegistry.getOutboundMap().forEach { (_, client) -> client.close() }
    }

    private fun setupForeground() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val CHANNEL_ID = "Mesh_channel"
        // 1. Create the Channel for Android 8.0+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                CHANNEL_ID,
                "Meshenger Mesh Service",
                android.app.NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        // 2. Build the Notification
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Mesh Maintainer Active")
            .setContentText("Maintaining healthy mesh connections...")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        // 3. Start Foreground (using the service's internal method)
        startForeground(1, notification)
    }

//    private fun startMeshMaintainerLoop() {
//        serviceScope.launch {
//            while(true) {
//                try {
//                    var activeConnectionCount = MeshConnectionRegistry.getCountConnections()
//                    if(activeConnectionCount < BleLimitConstants.MIN_CONNECTIONS_LIMIT) {
//                        scanner.onDemandScanSync(1000)
//                        if(scanner.getInMeshDevices().size > 0) {
//                            Log.d("MeshMaintainer", "Out Mesh Mode")
//                            if(server.isServerActive()) server.shutDownServer()
//                            var oldState = MeshConnectionRegistry.isInMesh()
//                            MeshConnectionRegistry.updateIsInMesh(false)
//                            if(oldState != false) {
//                                BleAdvertiser.resetBackgroundAdvertiser()
//                            }
//                            connectToDiscoveredPeers()
//                            delay(10000)
//                            activeConnectionCount = MeshConnectionRegistry.getCountConnections()
//                            Log.d("MeshMaintainer", "activeConnectionCount: $activeConnectionCount")
//                            while(activeConnectionCount < BleLimitConstants.MIN_CONNECTIONS_LIMIT) {
//                                scanner.onDemandScanSync(1000)
//                                connectToDiscoveredPeers()
//                                delay(10000)
//                                activeConnectionCount = MeshConnectionRegistry.getCountConnections()
//                                Log.d("MeshMaintainer", "activeConnectionCount: $activeConnectionCount")
//                            }
//                            oldState = MeshConnectionRegistry.isInMesh()
//                            MeshConnectionRegistry.updateIsInMesh(true)
//                            if(oldState != true) {
//                                BleAdvertiser.resetBackgroundAdvertiser()
//                            }
//                            if(!server.isServerActive()) server.open()
//                            Log.d("MeshMaintainer", "In Mesh Mode")
//                        } else {
//                            Log.d("MeshMaintainer", "In Mesh Mode")
//                            if(!server.isServerActive()) server.open()
//                            val oldState = MeshConnectionRegistry.isInMesh()
//                            MeshConnectionRegistry.updateIsInMesh(true)
//                            if(oldState != true) {
//                                BleAdvertiser.resetBackgroundAdvertiser()
//                            }
//                        }
//                    } else {
//                        Log.d("MeshMaintainer", "In Mesh Mode")
//                    }
//
//                } catch (e: Exception) {
//                    Log.e("MeshMaintainer", "Loop error: ${e.message}")
//                }
//                delay(1000)
//            }
//        }
//    }

    private fun startMeshMaintainerLoop() {
        serviceScope.launch {
            while (isRunning) {
                try {
                    val activeCount = MeshConnectionRegistry.getCountConnections()

                    if (activeCount < BleLimitConstants.MIN_CONNECTIONS_LIMIT) {
                        Log.d(
                            "MeshMaintainer",
                            "Under connection limit ($activeCount). Scanning..."
                        )

                        // 1. Scan for peers
                        scanner.onDemandScanSync(2000)
                        val discovered = scanner.getInMeshDevices()

                        if (discovered.isNotEmpty()) {
                            // 2. Transition Logic: Only shut down server if we have targets to connect to
                            // Adding a random delay prevents two phones from shutting down servers simultaneously
                            delay((500..2000).random().toLong())

                            handleOutMeshTransition()
                            connectToDiscoveredPeers(discovered)
                        } else {
                            // No one found? Be a Server and wait.
                            handleInMeshTransition()
                        }
                    } else {
                        handleInMeshTransition()
                    }
                } catch (e: Exception) {
                    Log.e("MeshMaintainer", "Loop error: ${e.message}")
                }
                delay(5000) // Don't hammer the CPU/Radio
            }
        }
    }

    private fun handleOutMeshTransition() {
        if (server.isServerActive()) server.shutDownServer()
        if (MeshConnectionRegistry.isInMesh()) {
            MeshConnectionRegistry.updateIsInMesh(false)
            BleAdvertiser.resetBackgroundAdvertiser()
        }
    }

    private fun handleInMeshTransition() {
        if (!server.isServerActive()) server.open()
        if (!MeshConnectionRegistry.isInMesh()) {
            MeshConnectionRegistry.updateIsInMesh(true)
            BleAdvertiser.resetBackgroundAdvertiser()
        }
    }
    fun startServer() {
        server.open()
    }
//    fun connectToDiscoveredPeers() {
//        val connectedToUs = MeshConnectionRegistry.getPhysicalPeerList()
//        val discoveredPeers = scanner.getInMeshDevices()
//        for (peer in discoveredPeers) {
//            val address = peer.device.address
//
//            if (connectedToUs.contains(peer)) continue
//
//            if (MeshConnectionRegistry.getOutboundMap().containsKey(address)) continue
//
//            initiateConnection(peer.device)
//        }
//    }
    private fun connectToDiscoveredPeers(discovered: List<PhysicalPeer>) {
        for (peer in discovered) {
            val addr = peer.device.address

            // Safety Checks:
            // 1. Not already connected as Client
            if (MeshConnectionRegistry.getOutboundMap().containsKey(addr)) continue
            // 2. Not already connected to us as Server (prevents circular loops)
            if (MeshConnectionRegistry.getInboundMap().containsKey(addr)) continue
            // 3. Not already trying to connect
            if (MeshConnectionRegistry.isPending(addr)) continue

            initiateConnection(peer.device)
        }
    }

    private fun handleIncomingMessage(device: BluetoothDevice, message: ByteArray) {
        var messageStr = String(message, Charsets.UTF_8)
        Log.i("MeshMaintainer", "Message from ${device.address}: $messageStr")
    }

//    private fun initiateConnection(device: BluetoothDevice) {
//        val client = BleClientConnection(appContext)
//
//        client.onDataReceived = { sender, packet -> // entry point of receiving incoming packet
//            // Network routing
//            Log.d("BleClientConnection", "Recieve data from ${sender.address}")
//            globalPacketListener?.onRecievePacket(packet, sender.address)
//        }
//
//        client.onDisconnected = { address ->
//            MeshConnectionRegistry.removeOutbound(address)
//            client.close()
//        }
//
//        client.connect(device)
//            .retry(3, 1000)
//            .useAutoConnect(false)
//            .done {
//                Log.i("MeshMaintainer", "Successfully connected to ${device.address}")
//                MeshConnectionRegistry.addOutBound(device.address, client)
//            }
//            .fail { device, status ->
//                Log.e("MeshMaintainer", "Failed to connect to ${device.address}: $status")
//            }
//            .enqueue()
//    }
    private fun initiateConnection(device: BluetoothDevice) {
        val address = device.address
        MeshConnectionRegistry.markPending(address)

        val client = BleClientConnection(applicationContext)
        client.onDataReceived = { sender, packet ->
            // Immediately move the work to a background worker thread
            serviceScope.launch(Dispatchers.Default) {
                Log.d("BleClientConnection", "Processing data from ${sender.address} asynchronously")

                // This is where your heavy lifting (routing, decryption, etc.) happens
                globalPacketListener?.onRecievePacket(packet, sender.address)
            }
        }

        client.onDisconnected = { addr ->
            MeshConnectionRegistry.removeOutbound(addr)
            MeshConnectionRegistry.unmarkPending(addr)
            client.close()
        }

        client.connect(device)
            .retry(2, 1000)
            .useAutoConnect(false)
            .done {
                Log.i("MeshMaintainer", "Connected to $address")
                MeshConnectionRegistry.addOutBound(address, client)
            }
            .fail { _, status ->
                Log.e("MeshMaintainer", "Failed $address: $status")
                MeshConnectionRegistry.unmarkPending(address)
            }
            .enqueue()
    }
    fun testSendMsgClientToServer(msg: String) {
        MeshConnectionRegistry.getOutboundMap().forEach { (address, server) ->
            server.sendMessageToServerStr(msg)
        }
    }
    fun testSendMsgServerToClient(msg: String) {
        MeshConnectionRegistry.getInboundMap().forEach { (address, client) ->
            client.sendMessageToClientStr(msg)
        }
    }

    companion object {
        private var globalPacketListener: TransportPacketListener? = null

        // This is the "Plug" where the Network layer connects
        fun setGlobalPacketListener(listener: TransportPacketListener) {
            globalPacketListener = listener
        }
    }
    override fun onBind(intent: Intent?) = null
}