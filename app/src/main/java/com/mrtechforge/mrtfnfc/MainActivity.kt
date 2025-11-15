package com.mrtechforge.mrtfnfc

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var testButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        testButton = findViewById(R.id.testButton)

        testButton.setOnClickListener {
            statusText.text = "Pressed Test Button"
        }

        // Handle app launch via NFC tag
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null) {
            handleIntent(intent)
        }
    }

    private fun handleIntent(intent: Intent) {
        val action = intent.action
        val data: Uri? = intent.data

        if (action == Intent.ACTION_VIEW && data != null) {
            statusText.text = "Protocol triggered:\n${data.toString()}"

            // Example: mrtf://wifi/toggle
            when (data.host) {
                "wifi" -> {
                    statusText.text = "Received: WiFi command (placeholder)"
                }
                "dnd" -> {
                    val zenIntent = Intent("android.settings.ZEN_MODE_SETTINGS")
                    startActivity(zenIntent)
                }
                else -> {
                    statusText.text = "Unhandled command:\n${data}"
                }
            }
        }
    }
}
