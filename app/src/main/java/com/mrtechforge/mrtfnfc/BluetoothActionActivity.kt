package com.mrtechforge.mrtfnfc.actions

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mrtechforge.mrtfnfc.R

class BluetoothActionActivity : AppCompatActivity() {

    private val btAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    private lateinit var txtStatus: TextView
    private lateinit var btnToggle: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth_action)

        txtStatus = findViewById(R.id.txtBtStatus)
        btnToggle = findViewById(R.id.btnToggleBluetooth)

        btnToggle.setOnClickListener {
            toggleBluetoothHybrid()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        if (btAdapter == null) {
            txtStatus.text = "Bluetooth not supported"
            btnToggle.isEnabled = false
            return
        }

        txtStatus.text =
            if (btAdapter.isEnabled)
                "Bluetooth is ENABLED"
            else
                "Bluetooth is DISABLED"
    }

    private fun toggleBluetoothHybrid() {
        if (btAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_LONG).show()
            return
        }

        if (btAdapter.isEnabled) {
            Toast.makeText(
                this,
                "Android does not allow disabling Bluetooth directly",
                Toast.LENGTH_LONG
            ).show()

            startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS))
        } else {
            startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
    }
}