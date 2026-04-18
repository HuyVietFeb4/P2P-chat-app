package com.meshenger.backend.transport.client

import android.bluetooth.BluetoothAdapter
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import com.meshenger.backend.transport.BleUUIDConstants
import com.meshenger.backend.transport.PhysicalPeer
import kotlinx.coroutines.delay
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat
import no.nordicsemi.android.support.v18.scanner.ScanCallback
import no.nordicsemi.android.support.v18.scanner.ScanFilter
import no.nordicsemi.android.support.v18.scanner.ScanResult
import no.nordicsemi.android.support.v18.scanner.ScanSettings

class BleScanner {
    private val SCAN_PEROID : Long = 10000
    private var isScanning : Boolean  = false
    val scanner = BluetoothLeScannerCompat.getScanner()
    private val handler = Handler(Looper.getMainLooper())

    private val discoveredDevices = mutableListOf<PhysicalPeer>()
    private val inMeshDevices = mutableListOf<PhysicalPeer>()
    private val outMeshDevices = mutableListOf<PhysicalPeer>()
    val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            result?.let {
                val device = result.device
                val deviceName = device.name
                val deviceAddress = device.address
                val isExtended = !it.isLegacy
                if (!discoveredDevices.contains(PhysicalPeer(device))) {
                    val newPeer = PhysicalPeer(device)
                    Log.d("BleScanner", "Found ${if(isExtended) "Extended" else "Legacy"} Device: $deviceName ($deviceAddress). Address: ${result.device.address}")
                    if(isExtended) {
                        val raw = result.scanRecord?.getManufacturerSpecificData(0xFFFF)
                        raw?.let {
                            val decoded = String(it, Charsets.UTF_8)
                            val parts = decoded.split("|")
                            val userName = parts[0]
                            val mpAddress = parts[1]
                            val isInMesh = parts.getOrNull(2)?.toBoolean() ?: false
                            Log.d("BleScanner", "Username: ${userName}")
                            Log.d("BleScanner", "MPAddress: ${mpAddress}")
                            Log.d("BleScanner", "isInMesh: ${isInMesh}")
                            Log.d("BleScanner", "raw payload: ${decoded}")
                            newPeer.MPAddress = mpAddress
                            newPeer.isInMesh = isInMesh
                        }
                    }
                    discoveredDevices.add(newPeer)
                    if(newPeer.isInMesh) {
                        inMeshDevices.add(newPeer)
                    } else {
                        outMeshDevices.add(newPeer)
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
        discoveredDevices.clear()
        outMeshDevices.clear()
        inMeshDevices.clear()
        if(isScanning) return
        isScanning = true
        handler.postDelayed({
            stopScan()
        }, scanPeroid)
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setReportDelay(0)
            .setLegacy(false)
            .setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED)
            .build()

        val filters = listOf(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(BleUUIDConstants.MESH_SERVICE_UUID))
                .build()
        )

        scanner.startScan(filters, settings, scanCallback)
    }

    suspend fun onDemandScanSync(scanPeroid: Long = SCAN_PEROID) {
        discoveredDevices.clear()
        outMeshDevices.clear()
        inMeshDevices.clear()
        if(isScanning) return
        isScanning = true
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setReportDelay(0)
            .setLegacy(false)
            .setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED)
            .build()

        val filters = listOf(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(BleUUIDConstants.MESH_SERVICE_UUID))
                .build()
        )
        scanner.startScan(filters, settings, scanCallback)
        delay(scanPeroid)
        stopScan()
    }

    fun onBackgroundScan() {
        discoveredDevices.clear()
        if(isScanning) return
        isScanning = true
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .setReportDelay(0)
            .build()

        val filters = listOf(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(BleUUIDConstants.MESH_SERVICE_UUID))
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

    fun getDiscoveredDevices() : List<PhysicalPeer> {
        return discoveredDevices
    }
    fun getInMeshDevices() : List<PhysicalPeer> {
        return inMeshDevices
    }
    fun getOutMeshDevices() : List<PhysicalPeer> {
        return outMeshDevices
    }
    fun isScanningActive(): Boolean {
        return isScanning
    }
    fun clearDiscoveredDevices() {
        discoveredDevices.clear()
    }
}

// Service is needed for the protocol to stay alive even after closing the app