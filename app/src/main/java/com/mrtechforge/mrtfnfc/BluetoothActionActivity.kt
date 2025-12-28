package com.mrtechforge.mrtfnfc

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class BluetoothActionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth_action)

        val btn = findViewById<Button>(R.id.btnOpenBluetoothSettings)

        btn.setOnClickListener {
            try {
                // Open Bluetooth settings directly
                val btSettingsIntent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                startActivity(btSettingsIntent)
            } catch (e: Exception) {
                Toast.makeText(this, "Unable to open Bluetooth settings.", Toast.LENGTH_SHORT).show()
            }
        }

        Toast.makeText(this, "Bluetooth tag scanned", Toast.LENGTH_SHORT).show()
    }
}