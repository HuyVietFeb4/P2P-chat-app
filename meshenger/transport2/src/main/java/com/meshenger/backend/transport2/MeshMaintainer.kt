package com.meshenger.backend.transport2

import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.meshenger.backend.transport2.client.BleClientConnection
import com.meshenger.backend.transport2.client.BleScanner
import com.meshenger.backend.transport2.server.BleAdvertiser
import com.meshenger.backend.transport2.server.BleServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class MeshMaintainer : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    // probability to trigger anti entropy
    private var AntiEntropyProbability = 0.0
    private lateinit var server: BleServer
    private lateinit var appContext: Context
    private lateinit var scanner: BleScanner
    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        server = BleServer(applicationContext)
        scanner = BleScanner()
        if (!server.isServerActive()) server.open()
        globalPacketListener?.let {
            server.setListener(it)
        }
    }
    private fun createNotification(): android.app.Notification {
        val channelId = "Mesh_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "Meshenger Mesh Service",
                android.app.NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Meshenger Active")
            .setContentText("Mesh network is running...")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true) // Makes it harder to dismiss
            .build()
    }
    private fun startMeshLogic() {
        startMeshMaintainerLoop()
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 1. Setup the notification channel and build the notification
        val notification = createNotification()

        // 2. Call startForeground IMMEDIATELY
        // For Android 14+ (SDK 34/36), you MUST specify the foregroundServiceType
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // 3. Start the loop
        if (!isRunning) {
            isRunning = true
            startMeshMaintainerLoop()
        }

        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d("MeshMaintainer", "App swiped away. Stopping service...")

        // Perform any urgent cleanup here
        // Note: You have very limited time before the process is killed
        isRunning = false
        MeshConnectionRegistry.getOutboundMap().forEach { (_, client) -> client.close() }
        MeshConnectionRegistry.updateIsInMesh(false)

        stopSelf() // Tells the system to stop this service
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MeshMaintainer", "Service onDestroy called. Cancelling loop...")
        isRunning = false
        // 1. Stop the Coroutine Loop
        serviceScope.cancel()

        // 2. Shut down hardware resources
        if(server.isServerActive()) server.shutDownServer()
        if(BleAdvertiser.isAdvertisingActive()) BleAdvertiser.stopAdvertising()

        super.onDestroy()
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

    private suspend fun maintainConnections() {
        try {
            val activeCount = MeshConnectionRegistry.getCountConnections()
            val outboundCount = MeshConnectionRegistry.getCountOutbound()
            val inboundCount = MeshConnectionRegistry.getCountInbound()

            Log.d(
                "MeshMaintainer",
                "Outbound connections ($outboundCount)"
            )
            Log.d(
                "MeshMaintainer",
                "Inbound connections ($inboundCount)"
            )
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
                Log.d(
                    "MeshMaintainer",
                    "Satisfy connection limit ($activeCount)."
                )
                handleInMeshTransition()
            }
        } catch (e: Exception) {
            Log.e("MeshMaintainer", "Loop error: ${e.message}")
        }
    }

    // Simple protocol to decide when to trigger anti entropy
    private suspend fun stochasticAntiEntropyScheduler() {
        if (Random.nextDouble() < AntiEntropyProbability) {
            Log.d("MeshMaintainer", "Entropy Triggered! (Prob: $AntiEntropyProbability)")

            // POKE THE NETWORK LAYER
            globalPacketListener?.onTriggerAntiEntropy()

            // Reset probability
            AntiEntropyProbability = 0.05
        } else {
            // Increase probability for the next check (max out at 100%)
            AntiEntropyProbability = (AntiEntropyProbability + 0.05).coerceAtMost(1.0)
        }
    }
    // dual - write version
    private fun startMeshMaintainerLoop() {
        serviceScope.launch {
            while (isRunning) {
                maintainConnections()
                if(MeshConnectionRegistry.getCountConnections() > 0) {
                    stochasticAntiEntropyScheduler()
                }
                delay(5000) // Don't hammer the CPU/Radio
            }
        }
    }

    private fun handleOutMeshTransition() {
        if (MeshConnectionRegistry.isInMesh()) {
            MeshConnectionRegistry.updateIsInMesh(false)
            BleAdvertiser.resetBackgroundAdvertiser()
        }
    }

    private fun handleInMeshTransition() {
        if (!MeshConnectionRegistry.isInMesh()) {
            MeshConnectionRegistry.updateIsInMesh(true)
            BleAdvertiser.resetBackgroundAdvertiser()
        }
    }
    fun startServer() {
        server.open()
    }
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

            initiateConnection(peer)
        }
    }

    private fun handleIncomingMessage(device: BluetoothDevice, message: ByteArray) {
        var messageStr = String(message, Charsets.UTF_8)
        Log.i("MeshMaintainer", "Message from ${device.address}: $messageStr")
    }

    private fun initiateConnection(peer: PhysicalPeer) {
        val address = peer.device.address
        if (MeshConnectionRegistry.isPending(address)) return
        MeshConnectionRegistry.markPending(address)

        val client = BleClientConnection(applicationContext)

//        client.onDataReceived = { sender, bytes ->
//            globalPacketListener?.onReceivePacket(bytes, sender.address)
//        }

        client.onDisconnected = { addr ->
            MeshConnectionRegistry.removeOutbound(addr)
            MeshConnectionRegistry.unmarkPending(addr)
            client.close()
        }

        client.connect(peer.device)
            .retry(2, 1000)
            .useAutoConnect(false)
            .done {
                Log.i("MeshMaintainer", "Connected to $address")
                MeshConnectionRegistry.addOutBound(address, client)
                MeshConnectionRegistry.addPhysicalPeer(peer)
            }
            .fail { _, status ->
                Log.e("MeshMaintainer", "Failed $address: $status")
                MeshConnectionRegistry.unmarkPending(address)
            }
            .enqueue()
    }

    companion object {
        private const val NOTIFICATION_ID = 1 // Define the ID here
        private var globalPacketListener: TransportPacketListener? = null
        private var isRunning = false
        // This is the "Plug" where the Network layer connects
        fun setGlobalPacketListener(listener: TransportPacketListener) {
            globalPacketListener = listener
        }

        // Start up function for the UI to invoke after finish initialize
        fun startMeshService(context: Context) {
            if (isRunning) return

            // 1. Initialize static components
//            BleAdvertiser.init(context)

            // 2. Prepare the intent
            val intent = Intent(context, MeshMaintainer::class.java)

            // 3. Start based on Android Version
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
    override fun onBind(intent: Intent?) = null
}