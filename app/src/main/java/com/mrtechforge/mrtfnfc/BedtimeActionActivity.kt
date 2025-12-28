package com.mrtechforge.mrtfnfc

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class BedtimeActionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bedtime_action)

        findViewById<Button>(R.id.btnBedtimeAction).setOnClickListener {
            // TODO: hook into DND / bedtime routines (requires Notification policy access)
            Toast.makeText(this, "Bedtime/DND action would run here.", Toast.LENGTH_SHORT).show()
        }

        Toast.makeText(this, "Bedtime tag scanned", Toast.LENGTH_SHORT).show()
    }
}