package com.mrtechforge.mrtfnfc

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class WifiActionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wifi_action)

        findViewById<Button>(R.id.btnOpenWifiSettings).setOnClickListener {
            // Best-effort Wi-Fi panel / settings
            try {
                val panelIntent = Intent(Settings.Panel.ACTION_WIFI)
                startActivity(panelIntent)
            } catch (e: Exception) {
                // Fallback
                val wifiSettingsIntent = Intent(Settings.ACTION_WIFI_SETTINGS)
                startActivity(wifiSettingsIntent)
            }
        }

        Toast.makeText(this, "Wi-Fi tag scanned", Toast.LENGTH_SHORT).show()
    }
}