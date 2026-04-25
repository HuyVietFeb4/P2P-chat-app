package com.meshenger.backend.transport2.server

import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.bluetooth.le.AdvertisingSetParameters
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import com.meshenger.backend.transport2.BleUUIDConstants
import com.meshenger.backend.transport2.MPAddress
import com.meshenger.backend.transport2.MeshConnectionRegistry
import kotlinx.coroutines.delay

object BleAdvertiser {
    private lateinit var appContext: Context
    private const val ADVERTISE_PEROID : Long = 10000
    private val handler = Handler(Looper.getMainLooper())
    fun init(context: Context) {
        this.appContext = context
    }
    private var isAdvertising = false
    private var advertiser: BluetoothLeAdvertiser? = null
    private var currentAdvertisingSet: AdvertisingSet? = null
    val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            super.onStartSuccess(settingsInEffect)
            isAdvertising = true
            Log.d("BleAdvertiser", "Advertising started!")
        }

        override fun onStartFailure(errorCode: Int) {
            val errorMsg = when (errorCode) {
                ADVERTISE_FAILED_DATA_TOO_LARGE -> "DATA_TOO_LARGE (Over 31 bytes)"
                ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "TOO_MANY_ADVERTISERS (Hardware limit)"
                ADVERTISE_FAILED_ALREADY_STARTED -> "ALREADY_STARTED"
                ADVERTISE_FAILED_INTERNAL_ERROR -> "INTERNAL_ERROR"
                ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "FEATURE_UNSUPPORTED"
                else -> "UNKNOWN_ERROR"
            }
            Log.e("BleAdvertiser", "Advertising failed: $errorMsg ($errorCode)")
        }
    }

    private val advertisingSetCallback = object : AdvertisingSetCallback() {
        override fun onAdvertisingSetStarted(set: AdvertisingSet?, txPower: Int, status: Int) {
            if (status == AdvertisingSetCallback.ADVERTISE_SUCCESS) {
                currentAdvertisingSet = set // STORE THIS REFERENCE
                isAdvertising = true
                Log.d("BleAdvertiser", "Extended Advertising started successfully")
            } else {
                Log.e("BleAdvertiser", "Extended Advertising failed with status: $status")
            }
        }

        override fun onAdvertisingSetStopped(set: AdvertisingSet?) {
            isAdvertising = false
            currentAdvertisingSet = null
            Log.d("BleAdvertiser", "Extended Advertising stopped")
        }
    }

    fun onBackgroundAdvertise() {
        if (isAdvertising) return

        val bluetoothManager =
            appContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter

        if (adapter == null) {
            Log.e("BleAdvertiser", "Bluetooth is disabled or not supported")
            return
        }

        advertiser = adapter.bluetoothLeAdvertiser
        if (adapter.isLeExtendedAdvertisingSupported) {
            val parameters = AdvertisingSetParameters.Builder()
                .setLegacyMode(false)
                .setInterval(AdvertisingSetParameters.INTERVAL_MEDIUM)
                .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_MEDIUM)
                .setConnectable(true)
                .build()

            val advertiseMeshAddr = MPAddress.getMyMPAddressString()
            val isInMesh = MeshConnectionRegistry.isInMesh()
            val combinedData = "$advertiseMeshAddr|$isInMesh"
            Log.d("BleAdvertiser", "Raw payload to sent: $combinedData")
            val advertiseData = AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .addServiceUuid(ParcelUuid(BleUUIDConstants.MESH_SERVICE_UUID))
                .addManufacturerData(0xFFFF, combinedData.encodeToByteArray()) // Should only use 1 field to not waste space for more id
                .build()
                advertiser?.startAdvertisingSet(parameters, advertiseData, null, null, null, advertisingSetCallback)
        } else {
            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build()

            val advertiseData = AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .addServiceUuid(ParcelUuid(BleUUIDConstants.MESH_SERVICE_UUID))
                .build()

            val scanResponseData = AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .build()

            advertiser?.startAdvertising(settings, advertiseData, scanResponseData, advertiseCallback)
        }
    }
    fun stopAdvertising() {
        // 1. Stop Extended Advertising if active
        currentAdvertisingSet?.let {
            advertiser?.stopAdvertisingSet(advertisingSetCallback)
            currentAdvertisingSet = null
        }

        // 2. Stop Legacy Advertising
        advertiser?.stopAdvertising(advertiseCallback)

        isAdvertising = false
        Log.d("BleAdvertiser", "All advertising stop commands sent")
    }

    fun isAdvertisingActive(): Boolean {
        return isAdvertising
    }

    // Probably not needed
    fun resetBackgroundAdvertiser() {
        if (isAdvertising) {
            stopAdvertising()
            // Wait for the stack to clear the callback
            handler.postDelayed({
                onBackgroundAdvertise()
            }, 500)
        } else {
            onBackgroundAdvertise()
        }
    }
}