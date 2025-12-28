package com.mrtechforge.mrtfnfc

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SosActivity : AppCompatActivity() {

    // TODO: make this configurable later
    private val sosPhoneNumber = "1234567890"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sos)

        val btn = findViewById<Button>(R.id.btnPrepareSosText)

        btn.setOnClickListener {
            // TODO: add real location link later
            val message = "SOS – I need help. (Location will be added in a future version.)"

            val uri = Uri.parse("smsto:$sosPhoneNumber")
            val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                putExtra("sms_body", message)
            }

            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "No SMS app available.", Toast.LENGTH_SHORT).show()
            }
        }

        Toast.makeText(this, "SOS tag scanned", Toast.LENGTH_SHORT).show()
    }
}