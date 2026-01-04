package com.mrtechforge.mrtfnfc.actions

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.mrtechforge.mrtfnfc.R

class SosActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sos)

        findViewById<Button>(R.id.btnSendSos).setOnClickListener {
            sendSos()
        }
    }

    private fun sendSos() {
        val message = "SOS! I need help. Sent via MRTF NFC."

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:")
            putExtra("sms_body", message)
        }

        startActivity(intent)
    }
}