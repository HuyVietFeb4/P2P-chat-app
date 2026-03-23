package com.meshenger.backend.transport

import android.bluetooth.BluetoothAdapter
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


object BleAdvertiser {
    private lateinit var appContext: Context
    private const val ADVERTISE_PEROID : Long = 10000
    private val handler = Handler(Looper.getMainLooper())
    fun init(context: Context) {
        this.appContext = context
    }
    private var isAdvertising = false
    private var advertiser: BluetoothLeAdvertiser? = null

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
            Log.d("BleAdvertiser", "Extended Advertising started with status: $status")
        }

        override fun onAdvertisingSetStopped(set: AdvertisingSet?) {
            Log.d("BleAdvertiser", "Extended Advertising stopped")
        }
    }

//    fun onDemandAdvertise(advertisePeroid: Long = ADVERTISE_PEROID) {
//        if (isAdvertising) return
//        handler.postDelayed({
//            stopAdvertising()
//        }, advertisePeroid)
//
//    }
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

            val advertiseData = AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .addServiceUuid(ParcelUuid(BleConstants.MESH_SERVICE_UUID))
                .addManufacturerData(0xFFFF, "FILLER_DATA_FOR_MESH_NETWORK_STABILITY_2026".toByteArray())
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
                .addServiceUuid(ParcelUuid(BleConstants.MESH_SERVICE_UUID))
                .build()

            val scanResponseData = AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .build()

            advertiser?.startAdvertising(settings, advertiseData, scanResponseData, advertiseCallback)
        }
    }
    fun stopAdvertising() {
        if (isAdvertising) {
            advertiser?.stopAdvertising(advertiseCallback)
            isAdvertising = false
            advertiser = null
            Log.d("BleAdvertiser", "Advertising stopped")
        }
    }
}