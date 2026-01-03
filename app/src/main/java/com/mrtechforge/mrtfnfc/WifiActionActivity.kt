package com.mrtechforge.mrtfnfc.actions

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.mrtechforge.mrtfnfc.R

class WifiActionActivity : AppCompatActivity() {

    private val TAG = "MRTF-WIFI"
    private lateinit var wifiManager: WifiManager
    private lateinit var statusText: TextView

    private val panelLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            verifyWifiState("Returned from system panel")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wifi_action)

        statusText = findViewById(R.id.txtStatus)
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        executeHybridWifiAction()
    }

    private fun executeHybridWifiAction() {
        val currentlyEnabled = wifiManager.isWifiEnabled
        val targetState = !currentlyEnabled

        Log.i(TAG, "Current=$currentlyEnabled → Target=$targetState")

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // Pre-Android 10: direct control allowed
            val success = wifiManager.setWifiEnabled(targetState)
            Log.i(TAG, "Direct toggle result=$success")
            verifyWifiState("Direct toggle")
        } else {
            // Android 10+: must use system UI
            Log.i(TAG, "Launching Wi-Fi system panel")
            statusText.text = "Waiting for system Wi-Fi change…"
            panelLauncher.launch(Intent(Settings.Panel.ACTION_WIFI))
        }
    }

    private fun verifyWifiState(origin: String) {
        val enabled = wifiManager.isWifiEnabled
        Log.i(TAG, "$origin → Wi-Fi now $enabled")

        statusText.text =
            if (enabled) "Wi-Fi is ON ✅"
            else "Wi-Fi is OFF ⛔"

        Toast.makeText(
            this,
            "Wi-Fi ${if (enabled) "enabled" else "disabled"}",
            Toast.LENGTH_SHORT
        ).show()

        // Optional: auto-close after confirmation
        statusText.postDelayed({ finish() }, 1500)
    }
}