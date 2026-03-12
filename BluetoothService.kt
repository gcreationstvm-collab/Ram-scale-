package com.hc05.weightscale

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * Handles Bluetooth serial communication with HC-05 module.
 * HC-05 uses the standard SPP (Serial Port Profile) UUID.
 */
class BluetoothService {

    companion object {
        private const val TAG = "BluetoothService"
        // Standard SPP UUID used by HC-05
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var readJob: Job? = null

    var onDataReceived: ((String) -> Unit)? = null
    var onConnectionStateChanged: ((Boolean) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    val isConnected: Boolean
        get() = bluetoothSocket?.isConnected == true

    /**
     * Connect to HC-05 device
     */
    suspend fun connect(device: BluetoothDevice): Boolean = withContext(Dispatchers.IO) {
        try {
            disconnect()
            Log.d(TAG, "Connecting to ${device.name} (${device.address})")

            val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            bluetoothSocket = socket
            socket.connect()

            inputStream = socket.inputStream
            outputStream = socket.outputStream

            withContext(Dispatchers.Main) {
                onConnectionStateChanged?.invoke(true)
            }

            startReading()
            true
        } catch (e: IOException) {
            Log.e(TAG, "Connection failed: ${e.message}")
            withContext(Dispatchers.Main) {
                onError?.invoke("Connection failed: ${e.message}")
                onConnectionStateChanged?.invoke(false)
            }
            disconnect()
            false
        }
    }

    /**
     * Start reading data from HC-05 in a coroutine
     */
    private fun startReading() {
        readJob = CoroutineScope(Dispatchers.IO).launch {
            val buffer = ByteArray(1024)
            val stringBuilder = StringBuilder()

            while (isActive && isConnected) {
                try {
                    val bytes = inputStream?.read(buffer) ?: break
                    if (bytes > 0) {
                        val received = String(buffer, 0, bytes)
                        stringBuilder.append(received)

                        // Process complete lines (HC-05 sends newline-terminated data)
                        while (stringBuilder.contains('\n')) {
                            val newlineIdx = stringBuilder.indexOf('\n')
                            val line = stringBuilder.substring(0, newlineIdx).trim()
                            stringBuilder.delete(0, newlineIdx + 1)

                            if (line.isNotEmpty()) {
                                withContext(Dispatchers.Main) {
                                    onDataReceived?.invoke(line)
                                }
                            }
                        }
                    }
                } catch (e: IOException) {
                    if (isActive) {
                        Log.e(TAG, "Read error: ${e.message}")
                        withContext(Dispatchers.Main) {
                            onError?.invoke("Connection lost: ${e.message}")
                            onConnectionStateChanged?.invoke(false)
                        }
                    }
                    break
                }
            }
        }
    }

    /**
     * Send a string command to HC-05
     */
    fun sendData(data: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                outputStream?.write((data + "\n").toByteArray())
                outputStream?.flush()
                Log.d(TAG, "Sent: $data")
            } catch (e: IOException) {
                Log.e(TAG, "Send failed: ${e.message}")
                withContext(Dispatchers.Main) {
                    onError?.invoke("Send failed: ${e.message}")
                }
            }
        }
    }

    /**
     * Disconnect from HC-05
     */
    fun disconnect() {
        readJob?.cancel()
        readJob = null
        try {
            inputStream?.close()
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Disconnect error: ${e.message}")
        }
        inputStream = null
        outputStream = null
        bluetoothSocket = null
    }
}
