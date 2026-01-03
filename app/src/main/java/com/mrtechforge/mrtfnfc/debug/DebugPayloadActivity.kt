package com.mrtechforge.mrtfnfc.debug

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.mrtechforge.mrtfnfc.R

class DebugPayloadActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug_payload)

        val textView = findViewById<TextView>(R.id.txtDebug)

        val payload = intent.getStringExtra("payload") ?: "<none>"
        val tnf = intent.getShortExtra("tnf", -1)
        val type = intent.getStringExtra("type") ?: "<none>"

        textView.text = """
            MRTF DEBUG VIEWER
            
            Payload:
            $payload
            
            TNF:
            $tnf
            
            Type:
            $type
        """.trimIndent()
    }
}