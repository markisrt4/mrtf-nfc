package com.mrtechforge.mrtfnfc.actions

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mrtechforge.mrtfnfc.R

class BedtimeActionActivity : AppCompatActivity() {

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
            startActivity(
                Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            )
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            txtStatus.text = "DND access not granted"
            btnToggle.isEnabled = false
            btnGrantAccess.isEnabled = true
            return
        }

        btnGrantAccess.isEnabled = false
        btnToggle.isEnabled = true

        val isEnabled =
            notificationManager.currentInterruptionFilter ==
                    NotificationManager.INTERRUPTION_FILTER_NONE

        txtStatus.text = if (isEnabled) {
            "Do Not Disturb is ENABLED"
        } else {
            "Do Not Disturb is DISABLED"
        }
    }

    private fun toggleDnd() {
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            Toast.makeText(this, "Grant DND access first", Toast.LENGTH_LONG).show()
            return
        }

        val enabled =
            notificationManager.currentInterruptionFilter ==
                    NotificationManager.INTERRUPTION_FILTER_NONE

        notificationManager.setInterruptionFilter(
            if (enabled)
                NotificationManager.INTERRUPTION_FILTER_ALL
            else
                NotificationManager.INTERRUPTION_FILTER_NONE
        )

        refreshStatus()
    }
}