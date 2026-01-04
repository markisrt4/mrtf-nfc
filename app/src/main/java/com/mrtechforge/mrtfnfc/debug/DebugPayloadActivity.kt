package com.mrtechforge.mrtfnfc.debug

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.mrtechforge.mrtfnfc.R

class DebugPayloadActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug_payload)

        val txtPayload = findViewById<TextView>(R.id.txtRawPayload)
        val txtAction = findViewById<TextView>(R.id.txtParsedAction)
        val txtTimestamp = findViewById<TextView>(R.id.txtTimestamp)

        txtPayload.text = intent.getStringExtra("raw_payload") ?: "N/A"
        txtAction.text = intent.getStringExtra("parsed_action") ?: "N/A"
        txtTimestamp.text = intent.getStringExtra("timestamp") ?: "N/A"
    }
}