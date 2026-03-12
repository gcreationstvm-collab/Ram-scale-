package com.hc05.weightscale

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

/**
 * Weight Screen - timed measurement session (300 second countdown).
 * Matches activity_weight.xml: green theme, GridLayout buttons, START button, countdown.
 */
class WeightScreenActivity : AppCompatActivity() {

    private lateinit var titleText: TextView
    private lateinit var remainingText: TextView
    private lateinit var startButton: Button
    private lateinit var weightResultText: TextView
    private lateinit var itemButtonsGrid: GridLayout

    private var countDownTimer: CountDownTimer? = null
    private var isRunning = false
    private var isDemoMode = false
    private var selectedItems = mutableListOf<Int>()
    private val SESSION_DURATION_MS = 300_000L // 300 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weight)

        isDemoMode = intent.getBooleanExtra("demo_mode", false)

        titleText = findViewById(R.id.weightScreenTitle)
        remainingText = findViewById(R.id.remainingText)
        startButton = findViewById(R.id.startButton)
        weightResultText = findViewById(R.id.weightResult)
        itemButtonsGrid = findViewById(R.id.itemButtonsGrid)

        setupItemButtons()

        startButton.setOnClickListener {
            if (isRunning) {
                stopSession()
            } else {
                startSession()
            }
        }

        setupBluetoothCallbacks()
    }

    private fun setupItemButtons() {
        val items = listOf("Item 1", "Item 2", "Item 3", "Item 4", "Item 5",
                           "Item 6", "Item 7", "Item 8", "Item 9", "Item 10")
        items.forEachIndexed { index, name ->
            val btn = Button(this).apply {
                text = name
                setOnClickListener {
                    val itemNum = index + 1
                    if (selectedItems.contains(itemNum)) {
                        selectedItems.remove(itemNum)
                        setBackgroundColor(resources.getColor(android.R.color.darker_gray, null))
                    } else {
                        selectedItems.add(itemNum)
                        setBackgroundColor(resources.getColor(android.R.color.holo_green_dark, null))
                        MainActivity.instance?.bluetoothService?.sendData("SELECT:$itemNum")
                    }
                }
            }
            itemButtonsGrid.addView(btn)
        }
    }

    private fun startSession() {
        isRunning = true
        startButton.text = "STOP"
        weightResultText.text = "Measuring..."

        MainActivity.instance?.bluetoothService?.sendData("START")

        countDownTimer = object : CountDownTimer(SESSION_DURATION_MS, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                remainingText.text = "Remaining: $seconds sec"
            }

            override fun onFinish() {
                remainingText.text = "Remaining: 0 sec"
                stopSession()
                showSessionCompleteDialog()
            }
        }.start()
    }

    private fun stopSession() {
        isRunning = false
        startButton.text = "START"
        countDownTimer?.cancel()
        MainActivity.instance?.bluetoothService?.sendData("STOP")
    }

    private fun showSessionCompleteDialog() {
        AlertDialog.Builder(this)
            .setTitle("Session Complete")
            .setMessage("Weight measurement session finished.\n${weightResultText.text}")
            .setPositiveButton("Ok") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun setupBluetoothCallbacks() {
        MainActivity.instance?.bluetoothService?.onDataReceived = { data ->
            try {
                val cleaned = data.replace(Regex("[^0-9.]"), "")
                if (cleaned.isNotEmpty()) {
                    val weight = cleaned.toDoubleOrNull()
                    if (weight != null) {
                        weightResultText.text = "Weight: %.3f kg".format(weight)
                    }
                }
            } catch (e: Exception) { /* ignore */ }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        MainActivity.instance?.bluetoothService?.onDataReceived = null
    }
}
