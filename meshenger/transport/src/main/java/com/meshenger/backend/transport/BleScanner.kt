package com.meshenger.backend.transport


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
    val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            Log.d("BleScaner", "Device found: ${result.device.address}")
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