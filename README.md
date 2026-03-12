# HC-05 Weight Scale App (Kotlin/Android)

Recreated from the original APK without the login screen.

## Project Structure

```
app/src/main/
├── AndroidManifest.xml
├── java/com/hc05/weightscale/
│   ├── MainActivity.kt          ← Device scan + connect (replaces login)
│   ├── BluetoothService.kt      ← HC-05 SPP Bluetooth communication
│   ├── WeightDisplayActivity.kt ← Main weight display + log
│   ├── WeightScreenActivity.kt  ← Timed 300s measurement session
│   └── WeightLiveActivity.kt    ← Real-time live streaming
└── res/layout/
    ├── activity_main.xml        ← Paired devices list
    ├── activity_main1.xml       ← Weight display + number buttons + log
    ├── activity_weight.xml      ← Timed weight screen (green theme)
    └── activity_weightlive.xml  ← Live weight (blue theme)
```

## How to Build

1. Open in **Android Studio** (Arctic Fox or newer)
2. Let Gradle sync
3. Build → Make Project
4. Run on device or emulator

## HC-05 Pairing Setup

1. Power on your HC-05 module
2. On Android: Settings → Bluetooth → Pair new device
3. Search for "HC-05", tap it, enter PIN **1234** (default)
4. Launch the app — it will auto-detect and highlight the HC-05

## Data Protocol

The app expects the HC-05 to send weight as newline-terminated strings:
- `123.456\n` → plain number in kg/grams
- `W:123.456\n` → prefixed format

Outgoing commands sent to HC-05:
- `START` / `STOP` → begin/end session
- `ITEM:N` → select item N (1–10)
- `LIVE:START` / `LIVE:STOP` → live streaming mode
- `SELECT:N` → select item in weight screen

## Features

- ✅ No login required
- ✅ Auto-detects HC-05 in paired devices list
- ✅ DEMO mode (no Bluetooth needed for testing)
- ✅ Weight log with timestamps
- ✅ Timed session (300 seconds countdown)
- ✅ Live streaming with Min/Max/Avg stats
- ✅ 10 item/scale selector buttons
- ✅ Bluetooth permissions handled for Android 12+
