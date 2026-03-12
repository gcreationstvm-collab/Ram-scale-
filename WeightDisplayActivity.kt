package com.hc05.weightscale

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

/**
 * Weight Display Screen - shows current weight from HC-05/scale.
 * Matches activity_main1.xml: weight display + number buttons + log list.
 */
class WeightDisplayActivity : AppCompatActivity() {

    private lateinit var weightTextView: TextView
    private lateinit var weightLogListView: ListView
    private lateinit var weightScaleButton: Button
    private lateinit var weightLiveButton: Button
    private lateinit var disconnectButton: Button

    private val weightLog = mutableListOf<String>()
    private lateinit var logAdapter: ArrayAdapter<String>
    private var isDemoMode = false
    private var currentWeight = 0.0

    // Number buttons for item/scale selection
    private val buttonIds = listOf(
        R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, R.id.btn5,
        R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9, R.id.btn10
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main1)

        isDemoMode = intent.getBooleanExtra("demo_mode", false)

        weightTextView = findViewById(R.id.weightDisplay)
        weightLogListView = findViewById(R.id.weightLogList)
        weightScaleButton = findViewById(R.id.btnWeightScreen)
        weightLiveButton = findViewById(R.id.btnWeightLive)
        disconnectButton = findViewById(R.id.btnDisconnect)

        logAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, weightLog)
        weightLogListView.adapter = logAdapter

        // Set up number buttons (item selectors)
        buttonIds.forEachIndexed { index, id ->
            findViewById<Button>(id).setOnClickListener {
                val itemNum = index + 1
                val service = MainActivity.instance?.bluetoothService
                service?.sendData("ITEM:$itemNum")
                Toast.makeText(this, "Selected item $itemNum", Toast.LENGTH_SHORT).show()
            }
        }

        weightScaleButton.setOnClickListener {
            val intent = Intent(this, WeightScreenActivity::class.java)
            intent.putExtra("demo_mode", isDemoMode)
            startActivity(intent)
        }

        weightLiveButton.setOnClickListener {
            val intent = Intent(this, WeightLiveActivity::class.java)
            intent.putExtra("demo_mode", isDemoMode)
            startActivity(intent)
        }

        disconnectButton.setOnClickListener {
            MainActivity.instance?.bluetoothService?.disconnect()
            finish()
        }

        setupBluetoothCallbacks()

        if (isDemoMode) {
            startDemoMode()
        }
    }

    private fun setupBluetoothCallbacks() {
        MainActivity.instance?.bluetoothService?.onDataReceived = { data ->
            parseWeightData(data)
        }
        MainActivity.instance?.bluetoothService?.onConnectionStateChanged = { connected ->
            if (!connected && !isDemoMode) {
                Toast.makeText(this, "Bluetooth disconnected", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun parseWeightData(data: String) {
        // HC-05 scale typically sends: "W:123.45" or just "123.45"
        try {
            val cleaned = data.replace(Regex("[^0-9.]"), "")
            if (cleaned.isNotEmpty()) {
                currentWeight = cleaned.toDoubleOrNull() ?: currentWeight
                updateWeightDisplay(currentWeight)
                addToLog("%.3f kg".format(currentWeight))
            }
        } catch (e: Exception) {
            // ignore malformed data
        }
    }

    private fun updateWeightDisplay(weight: Double) {
        weightTextView.text = "Weight: %.3f".format(weight)
    }

    private fun addToLog(entry: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
        weightLog.add(0, "[$timestamp] $entry")
        if (weightLog.size > 50) weightLog.removeAt(weightLog.size - 1)
        logAdapter.notifyDataSetChanged()
    }

    private fun startDemoMode() {
        // Simulate weight readings in demo mode
        val handler = android.os.Handler(mainLooper)
        val runnable = object : Runnable {
            override fun run() {
                currentWeight = 50.0 + Random.nextDouble(-5.0, 5.0)
                updateWeightDisplay(currentWeight)
                addToLog("%.3f kg (demo)".format(currentWeight))
                handler.postDelayed(this, 2000)
            }
        }
        handler.post(runnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        MainActivity.instance?.bluetoothService?.onDataReceived = null
    }
}
