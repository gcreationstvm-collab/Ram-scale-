package com.hc05.weightscale

import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

/**
 * Live Weight Screen - real-time continuous weight reading from HC-05.
 * Matches activity_weightlive.xml.
 */
class WeightLiveActivity : AppCompatActivity() {

    private lateinit var titleText: TextView
    private lateinit var liveWeightText: TextView
    private lateinit var statusText: TextView
    private lateinit var startButton: Button
    private lateinit var itemButtonsGrid: GridLayout
    private lateinit var minWeightText: TextView
    private lateinit var maxWeightText: TextView
    private lateinit var avgWeightText: TextView

    private var isStreaming = false
    private var isDemoMode = false
    private var selectedItem = 0
    private val readings = mutableListOf<Double>()
    private var demoHandler: android.os.Handler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weightlive)

        isDemoMode = intent.getBooleanExtra("demo_mode", false)

        titleText = findViewById(R.id.liveTitle)
        liveWeightText = findViewById(R.id.liveWeightDisplay)
        statusText = findViewById(R.id.liveStatus)
        startButton = findViewById(R.id.liveStartButton)
        itemButtonsGrid = findViewById(R.id.liveItemGrid)
        minWeightText = findViewById(R.id.minWeight)
        maxWeightText = findViewById(R.id.maxWeight)
        avgWeightText = findViewById(R.id.avgWeight)

        setupItemButtons()

        startButton.setOnClickListener {
            if (isStreaming) {
                stopStreaming()
            } else {
                startStreaming()
            }
        }

        setupBluetoothCallbacks()
    }

    private fun setupItemButtons() {
        for (i in 1..10) {
            val btn = Button(this).apply {
                text = "$i"
                setBackgroundColor(Color.LTGRAY)
                setOnClickListener {
                    selectedItem = i
                    updateItemSelection(this)
                    MainActivity.instance?.bluetoothService?.sendData("ITEM:$i")
                    statusText.text = "Item $i selected"
                }
            }
            itemButtonsGrid.addView(btn)
        }
    }

    private fun updateItemSelection(selected: Button) {
        for (i in 0 until itemButtonsGrid.childCount) {
            val child = itemButtonsGrid.getChildAt(i) as? Button
            child?.setBackgroundColor(Color.LTGRAY)
        }
        selected.setBackgroundColor(Color.parseColor("#4CAF50"))
    }

    private fun startStreaming() {
        isStreaming = true
        startButton.text = "STOP"
        readings.clear()
        statusText.text = "Streaming live weight..."
        MainActivity.instance?.bluetoothService?.sendData("LIVE:START")

        if (isDemoMode) {
            demoHandler = android.os.Handler(mainLooper)
            val runnable = object : Runnable {
                override fun run() {
                    if (isStreaming) {
                        val w = 50.0 + Random.nextDouble(-2.0, 2.0)
                        updateLiveReading(w)
                        demoHandler?.postDelayed(this, 500)
                    }
                }
            }
            demoHandler?.post(runnable)
        }
    }

    private fun stopStreaming() {
        isStreaming = false
        startButton.text = "START"
        statusText.text = "Stopped"
        demoHandler?.removeCallbacksAndMessages(null)
        MainActivity.instance?.bluetoothService?.sendData("LIVE:STOP")
    }

    private fun updateLiveReading(weight: Double) {
        liveWeightText.text = "%.3f kg".format(weight)
        readings.add(weight)

        if (readings.isNotEmpty()) {
            minWeightText.text = "Min: %.3f".format(readings.min())
            maxWeightText.text = "Max: %.3f".format(readings.max())
            avgWeightText.text = "Avg: %.3f".format(readings.average())
        }
    }

    private fun setupBluetoothCallbacks() {
        MainActivity.instance?.bluetoothService?.onDataReceived = { data ->
            if (isStreaming) {
                try {
                    val cleaned = data.replace(Regex("[^0-9.]"), "")
                    val weight = cleaned.toDoubleOrNull()
                    if (weight != null) updateLiveReading(weight)
                } catch (e: Exception) { /* ignore */ }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        demoHandler?.removeCallbacksAndMessages(null)
        MainActivity.instance?.bluetoothService?.onDataReceived = null
        if (isStreaming) {
            MainActivity.instance?.bluetoothService?.sendData("LIVE:STOP")
        }
    }
}
