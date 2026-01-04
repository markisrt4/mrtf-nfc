package com.mrtechforge.mrtfnfc.actions

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mrtechforge.mrtfnfc.R

class BedtimeActionActivity : AppCompatActivity() {

    private val TAG = "MRTF-DND"

    private lateinit var notificationManager: NotificationManager
    private lateinit var txtStatus: TextView
    private lateinit var btnToggle: Button
    private lateinit var btnGrantAccess: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bedtime_action)

        notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        txtStatus = findViewById(R.id.txtDndStatus)
        btnToggle = findViewById(R.id.btnToggleDnd)
        btnGrantAccess = findViewById(R.id.btnGrantDndAccess)

        btnToggle.setOnClickListener {
            toggleDnd()
        }

        btnGrantAccess.setOnClickListener {
            openDndSettings()
        }

        updateUi()
    }

    override fun onResume() {
        super.onResume()
        updateUi()
    }

    // ---------------- CORE LOGIC ----------------

    private fun updateUi() {
        val hasAccess = notificationManager.isNotificationPolicyAccessGranted

        if (!hasAccess) {
            txtStatus.text = "Do Not Disturb access not granted."
            btnToggle.isEnabled = false
            btnGrantAccess.isEnabled = true
            Log.w(TAG, "DND access NOT granted")
            return
        }

        btnGrantAccess.isEnabled = false
        btnToggle.isEnabled = true

        when (notificationManager.currentInterruptionFilter) {
            NotificationManager.INTERRUPTION_FILTER_ALL -> {
                txtStatus.text = "Do Not Disturb is OFF"
                btnToggle.text = "Enable Do Not Disturb"
            }

            NotificationManager.INTERRUPTION_FILTER_PRIORITY,
            NotificationManager.INTERRUPTION_FILTER_NONE -> {
                txtStatus.text = "Do Not Disturb is ON"
                btnToggle.text = "Disable Do Not Disturb"
            }

            else -> {
                txtStatus.text = "Do Not Disturb status unknown"
                btnToggle.text = "Toggle Do Not Disturb"
            }
        }
    }

    private fun toggleDnd() {
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            toast("Grant DND access first.")
            openDndSettings()
            return
        }

        val current = notificationManager.currentInterruptionFilter

        if (current == NotificationManager.INTERRUPTION_FILTER_ALL) {
            // Turn DND ON
            notificationManager.setInterruptionFilter(
                NotificationManager.INTERRUPTION_FILTER_PRIORITY
            )
            Log.i(TAG, "DND enabled (PRIORITY)")
            toast("Do Not Disturb enabled")
        } else {
            // Turn DND OFF
            notificationManager.setInterruptionFilter(
                NotificationManager.INTERRUPTION_FILTER_ALL
            )
            Log.i(TAG, "DND disabled")
            toast("Do Not Disturb disabled")
        }

        updateUi()
    }

    private fun openDndSettings() {
        try {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open DND settings", e)
            toast("Unable to open DND settings")
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}