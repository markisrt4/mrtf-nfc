package com.mrtechforge.mrtfnfc.actions

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class BluetoothActionActivity : AppCompatActivity() {

    private val TAG = "MRTF_BT"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val adapter = BluetoothAdapter.getDefaultAdapter()

        if (adapter == null) {
            toast("Bluetooth not supported on this device")
            finish()
            return
        }

        // --------------------------------------------------
        // ANDROID 13+ → must show system UI
        // --------------------------------------------------
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.i(TAG, "Android 13+: launching Bluetooth settings")
            toast("Opening Bluetooth settings")
            startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            finish()
            return
        }

        // --------------------------------------------------
        // ANDROID ≤12 → silent toggle allowed
        // --------------------------------------------------
        try {
            if (adapter.isEnabled) {
                adapter.disable()
                toast("Bluetooth disabled")
                Log.i(TAG, "Bluetooth disabled silently")
            } else {
                adapter.enable()
                toast("Bluetooth enabled")
                Log.i(TAG, "Bluetooth enabled silently")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Bluetooth toggle failed, fallback to settings", e)
            toast("Opening Bluetooth settings")
            startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        }

        finish()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}