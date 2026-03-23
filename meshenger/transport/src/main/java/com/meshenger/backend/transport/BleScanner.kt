package com.meshenger.backend.transport


import android.bluetooth.BluetoothDevice
import no.nordicsemi.android.support.v18.scanner.ScanSettings
import no.nordicsemi.android.support.v18.scanner.ScanFilter
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat
import no.nordicsemi.android.support.v18.scanner.ScanCallback

import android.os.ParcelUuid
import android.util.Log
import no.nordicsemi.android.support.v18.scanner.ScanResult
import android.os.Handler
import android.os.Looper

object BleScanner {
    private const val SCAN_PEROID : Long = 10000
    private var isScanning : Boolean  = false

    val scanner = BluetoothLeScannerCompat.getScanner()
    private val handler = Handler(Looper.getMainLooper())

    val discoveredDevices = mutableListOf<BluetoothDevice>()

    val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            result?.let {
                val device = result.device
                val deviceName = device.name
                val deviceAddress = device.address
                val isExtended = !it.isLegacy
                if (!discoveredDevices.contains(device)) {
                    discoveredDevices.add(device)
                    Log.d("BleScanner", "Found ${if(isExtended) "Extended" else "Legacy"} Device: $deviceName ($deviceAddress). Address: ${result.device.address}")
                    if(isExtended) {
                        Log.d("BleScanner", "Filler data: ${result.scanRecord?.getManufacturerSpecificData(0xFFFF)}")
                    }
                }


            }

        }

        override fun onBatchScanResults(results: MutableList<ScanResult?>) {
            for(result in results) {
                Log.d("BleScaner", "Batch Device: ${result?.device?.address}")
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.d("BleScaner", "Scanned failed: errorCode")
        }
    }

    fun onDemandScan(scanPeroid: Long = SCAN_PEROID) {
        if(isScanning) return
        isScanning = true
        handler.postDelayed({
            stopScan()
        }, scanPeroid)
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
            .setLegacy(false)
            .setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED)
            .build()

        val filters = listOf(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(BleConstants.MESH_SERVICE_UUID))
                .build()
        )

        scanner.startScan(filters, settings, scanCallback)
    }

    fun onBackgroundScan() {
        if(isScanning) return
        isScanning = true
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .setReportDelay(0)
            .build()

        val filters = listOf(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(BleConstants.MESH_SERVICE_UUID))
                .build()
        )
        scanner.startScan(filters, settings, scanCallback)
    }
    fun stopScan() {
        if(isScanning) {
            isScanning = false
            scanner.stopScan(scanCallback)
            Log.d("BleScanner", "Scan stopped")
        }
    }
}