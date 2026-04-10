package com.meshenger.backend.transport

import android.content.Intent
import android.graphics.Mesh
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.WritableArray
import com.meshenger.backend.transport.client.BleScanner
import com.meshenger.backend.transport.server.BleAdvertiser

class BleModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    private val context: ReactApplicationContext
    // This is the name you will use in your JavaScript code
    override fun getName(): String = "BleModule"
    init {
        context = reactContext
        BleAdvertiser.init(reactContext.applicationContext)
    }
//    @ReactMethod
//    fun onDemandScan(scanPeriodMs: Int = 10000) {
//        // This calls the object you just built!
//        BleScanner.onDemandScan(scanPeriodMs.toLong())
//    }

//    @ReactMethod
//    fun onDemandAdvertise(advertisePeriodMs: Int = 10000) {
//        BleAdvertiser.onDemandAdvertise(advertisePeriodMs)
//    }

    @ReactMethod
    fun onBackgroundAdvertise() {
        BleAdvertiser.onBackgroundAdvertise()
    }

//    @ReactMethod
//    fun getDiscoveredDevices(): WritableArray {
//        val returnList = BleScanner.getDiscoveredDevices()
//        val array = Arguments.createArray()
//        for (peer in returnList) {
//            val map = Arguments.createMap()
//            map.putString("deviceName", peer.device.name?: "Unknown device")
//            map.putString("userName", peer.userName?: "Unknown user")
//            map.putString("MPAddress", peer.MPAddress?: "Unknown address")
//
//            array.pushMap(map)
//        }
//        return array
//    }
//    @ReactMethod
//    fun testSendMsgClientToServer(msg: String) {
//        MeshManager.testSendMsgClientToServer(msg)
//    }
//    @ReactMethod
//    fun testSendMsgServerToClient(msg: String) {
//        MeshManager.testSendMsgServerToClient(msg)
//    }
//
//    @ReactMethod
//    fun startServer() {
//        MeshManager.startServer()
//    }

    // Self healing service
    @ReactMethod
    fun startMeshService() {
        val intent = Intent(context.applicationContext, MeshMaintainer::class.java)

        // For Android 8.0 (Oreo) and above, you must use startForegroundService
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    @ReactMethod
    fun stopMeshService() {
        val intent = Intent(context.applicationContext, MeshMaintainer::class.java)
        context.stopService(intent)
    }
}