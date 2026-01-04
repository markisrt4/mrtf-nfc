package com.mrtechforge.mrtfnfc.actions

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mrtechforge.mrtfnfc.R

class BluetoothActionActivity : AppCompatActivity() {

    private lateinit var txtStatus: TextView
    private lateinit var btnAction: Button

    private val bluetoothAdapter: BluetoothAdapter? =
        BluetoothAdapter.getDefaultAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth_action)

        txtStatus = findViewById(R.id.txtStatus)
        btnAction = findViewById(R.id.btnToggle)

        if (bluetoothAdapter == null) {
            txtStatus.text = "Bluetooth not supported on this device."
            btnAction.isEnabled = false
            return
        }

        refreshStatus()

        btnAction.setOnClickListener {
            if (bluetoothAdapter.isEnabled) {
                Toast.makeText(this, "Bluetooth already ON", Toast.LENGTH_SHORT).show()
                refreshStatus()
            } else {
                // Hybrid: open OS dialog
                startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        val enabled = bluetoothAdapter?.isEnabled == true
        txtStatus.text =
            if (enabled) "Bluetooth is ON ✅"
            else "Bluetooth is OFF ❌"

        btnAction.text =
            if (enabled) "Bluetooth Enabled"
            else "Enable Bluetooth"
    }
}