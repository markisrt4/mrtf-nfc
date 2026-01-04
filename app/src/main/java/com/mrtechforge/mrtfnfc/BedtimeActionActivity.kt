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

    private lateinit var txtStatus: TextView
    private lateinit var btnAction: Button

    private lateinit var notificationManager: NotificationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bedtime_action)

        txtStatus = findViewById(R.id.txtStatus)
        btnAction = findViewById(R.id.btnToggle)

        notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        refreshStatus()

        btnAction.setOnClickListener {
            if (!notificationManager.isNotificationPolicyAccessGranted) {
                // Permission required
                Toast.makeText(this, "Grant DND permission", Toast.LENGTH_SHORT).show()
                startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            } else {
                // Hybrid confirmation only — Android controls the toggle
                Toast.makeText(this, "You can now enable Bedtime / DND", Toast.LENGTH_SHORT).show()
                startActivity(Intent(Settings.ACTION_DND_SETTINGS))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        val granted = notificationManager.isNotificationPolicyAccessGranted

        txtStatus.text =
            if (granted) "DND permission granted ✅"
            else "DND permission NOT granted ❌"

        btnAction.text =
            if (granted) "Open Bedtime / DND Settings"
            else "Grant DND Permission"
    }
}