package com.hc05.weightscale

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * Main screen: lists paired HC-05 devices and connects to selected one.
 * Replaces the original PIN login screen.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var deviceListView: ListView
    private lateinit var statusText: TextView
    private lateinit var connectButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var demoButton: Button

    private val devices = mutableListOf<BluetoothDevice>()
    private val deviceNames = mutableListOf<String>()
    private lateinit var deviceAdapter: ArrayAdapter<String>
    private var selectedDevice: BluetoothDevice? = null

    val bluetoothService = BluetoothService()

    companion object {
        var instance: MainActivity? = null
        const val REQUEST_PERMISSIONS = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
        setContentView(R.layout.activity_main)

        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        deviceListView = findViewById(R.id.deviceListView)
        statusText = findViewById(R.id.statusText)
        connectButton = findViewById(R.id.connectButton)
        progressBar = findViewById(R.id.progressBar)
        demoButton = findViewById(R.id.demoButton)

        deviceAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_single_choice, deviceNames)
        deviceListView.adapter = deviceAdapter
        deviceListView.choiceMode = ListView.CHOICE_MODE_SINGLE

        deviceListView.setOnItemClickListener { _, _, position, _ ->
            selectedDevice = devices[position]
            connectButton.isEnabled = true
            statusText.text = "Selected: ${deviceNames[position]}"
        }

        connectButton.setOnClickListener {
            selectedDevice?.let { device -> connectToDevice(device) }
        }

        demoButton.setOnClickListener {
            // Launch demo mode without Bluetooth
            val intent = Intent(this, WeightDisplayActivity::class.java)
            intent.putExtra("demo_mode", true)
            startActivity(intent)
        }

        checkPermissionsAndLoadDevices()
    }

    private fun checkPermissionsAndLoadDevices() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), REQUEST_PERMISSIONS)
        } else {
            loadPairedDevices()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS) {
            loadPairedDevices()
        }
    }

    private fun loadPairedDevices() {
        if (!bluetoothAdapter.isEnabled) {
            statusText.text = "Bluetooth is disabled. Please enable it."
            return
        }

        try {
            val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices ?: emptySet()
            devices.clear()
            deviceNames.clear()

            pairedDevices.forEach { device ->
                devices.add(device)
                val name = try { device.name ?: "Unknown" } catch (e: SecurityException) { device.address }
                deviceNames.add("$name\n${device.address}")
            }

            deviceAdapter.notifyDataSetChanged()

            if (devices.isEmpty()) {
                statusText.text = "No paired devices found.\nPair HC-05 in Bluetooth settings first."
            } else {
                statusText.text = "Select your HC-05 device:"
            }

            // Auto-select HC-05 if found
            devices.forEachIndexed { index, device ->
                val name = try { device.name ?: "" } catch (e: SecurityException) { "" }
                if (name.contains("HC-05", ignoreCase = true) || name.contains("HC05", ignoreCase = true)) {
                    selectedDevice = device
                    deviceListView.setItemChecked(index, true)
                    connectButton.isEnabled = true
                    statusText.text = "HC-05 found! Tap Connect."
                }
            }
        } catch (e: SecurityException) {
            statusText.text = "Permission denied for Bluetooth access."
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        progressBar.visibility = View.VISIBLE
        connectButton.isEnabled = false
        val deviceName = try { device.name ?: device.address } catch (e: SecurityException) { device.address }
        statusText.text = "Connecting to $deviceName..."

        bluetoothService.onConnectionStateChanged = { connected ->
            progressBar.visibility = View.GONE
            if (connected) {
                statusText.text = "Connected to $deviceName"
                val intent = Intent(this, WeightDisplayActivity::class.java)
                intent.putExtra("demo_mode", false)
                startActivity(intent)
            } else {
                statusText.text = "Disconnected"
                connectButton.isEnabled = true
            }
        }

        bluetoothService.onError = { error ->
            progressBar.visibility = View.GONE
            statusText.text = "Error: $error"
            connectButton.isEnabled = true
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
        }

        lifecycleScope.launch {
            bluetoothService.connect(device)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}
