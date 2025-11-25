package com.developerfromjokela.nissanleaftelematics.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.developerfromjokela.nissanleaftelematics.R

class BleDeviceSelectActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DEVICE_ADDRESS = "device_address"
    }

    private lateinit var newDevicesAdapter: ArrayAdapter<String>
    private val discoveredDevices = mutableSetOf<String>() // just store addresses

    private var scanner: BluetoothLeScanner? = null
    private var isScanning = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.all { it }) startBleScan()
        else Toast.makeText(this, "Permissions denied", Toast.LENGTH_LONG).show()
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            runOnUiThread {
                val device = result.device
                val address = device.address
                if (address in discoveredDevices) return@runOnUiThread

                discoveredDevices.add(address)

                val name = device.name ?: "Unknown"
                val uuids = result.scanRecord?.serviceUuids?.joinToString(", ") { uuid ->
                    uuid.uuid.toString().uppercase().substring(0, 8) // short form
                } ?: "No UUIDs"

                val item = "$name\n$address\nUUIDs: $uuids"
                newDevicesAdapter.add(item)
                newDevicesAdapter.notifyDataSetChanged()
            }
        }

        override fun onScanFailed(errorCode: Int) {
            runOnUiThread {
                Toast.makeText(this@BleDeviceSelectActivity, "Scan failed: $errorCode", Toast.LENGTH_SHORT).show()
                updateScanButton()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ble_device_select)

        title = "Select BLE OBD Adapter"

        newDevicesAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        val listView = findViewById<ListView>(R.id.new_devices)
        listView.adapter = newDevicesAdapter

        // THIS IS THE CORRECT WAY — no more type mismatch!
        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val text = newDevicesAdapter.getItem(position) ?: return@OnItemClickListener
            val address = text.split("\n")[1] // second line is MAC

            val resultIntent = Intent().apply {
                putExtra(EXTRA_DEVICE_ADDRESS, address)
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }

        findViewById<Button>(R.id.button_scan).setOnClickListener {
            if (isScanning) stopBleScan() else startBleScan()
        }

        checkPermissionsAndStart()
    }

    private fun checkPermissionsAndStart() {
        val perms = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (perms.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            startBleScan()
        } else {
            requestPermissionLauncher.launch(perms)
        }
    }

    private fun startBleScan() {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: run {
            Toast.makeText(this, "No Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            Toast.makeText(this, "BLE not supported", Toast.LENGTH_SHORT).show()
            return
        }

        discoveredDevices.clear()
        newDevicesAdapter.clear()
        newDevicesAdapter.add("Scanning...")
        updateScanButton("Stop Scan", true)

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner!!.startScan(null, settings, scanCallback)
        isScanning = true
    }

    private fun stopBleScan() {
        scanner?.stopScan(scanCallback)
        isScanning = false
        updateScanButton("Scan Again", true)
        if (newDevicesAdapter.count == 0) {
            newDevicesAdapter.add("No devices found")
        }
    }

    private fun updateScanButton(text: String = "Scan Again", enabled: Boolean = true) {
        findViewById<Button>(R.id.button_scan).apply {
            this.text = text
            isEnabled = enabled
        }
    }

    override fun onStop() {
        super.onStop()
        if (isScanning) stopBleScan()
    }
}