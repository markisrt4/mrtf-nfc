package com.mrtechforge.mrtfnfc

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null
    private var tagPayload: ByteArray? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null || !nfcAdapter!!.isEnabled) {
            Toast.makeText(this, "NFC is not supported or disabled.", Toast.LENGTH_LONG).show()
        }

        pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )

        // --- Hook up buttons ---
        findViewById<Button>(R.id.btnWifi).setOnClickListener {
            tagPayload = "mrtf://wifi-toggle".toByteArray()
            Toast.makeText(this, "Ready to write Wi-Fi Toggle tag", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnBluetooth).setOnClickListener {
            tagPayload = "mrtf://bt-toggle".toByteArray()
            Toast.makeText(this, "Ready to write Bluetooth Toggle tag", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnDND).setOnClickListener {
            tagPayload = "mrtf://dnd".toByteArray()
            Toast.makeText(this, "Ready to write DND tag", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnCarLocation).setOnClickListener {
            tagPayload = "mrtf://car-location".toByteArray()
            Toast.makeText(this, "Ready to write Car Location tag", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnSOSText).setOnClickListener {
            tagPayload = "mrtf://sos".toByteArray()
            Toast.makeText(this, "Ready to write SOS Text tag", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnBedtime).setOnClickListener {
            tagPayload = "mrtf://bedtime".toByteArray()
            Toast.makeText(this, "Ready to write Bedtime tag", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnOpenApp).setOnClickListener {
            tagPayload = "mrtf://openapp".toByteArray()
            Toast.makeText(this, "Ready to write App Launcher tag", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(
            this,
            pendingIntent,
            arrayOf(),
            null
        )
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        val tag = intent?.getParcelableExtra<android.nfc.Tag>(NfcAdapter.EXTRA_TAG)
        if (tag != null && tagPayload != null) {
            val record = NdefRecord.createMime("text/plain", tagPayload!!)
            val message = NdefMessage(arrayOf(record))

            try {
                val ndef = android.nfc.tech.Ndef.get(tag)
                ndef?.connect()
                ndef?.writeNdefMessage(message)
                ndef?.close()

                Toast.makeText(this, "Tag written successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                toast("Write failed: ${e.message}")
            }
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}
