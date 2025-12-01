package com.mrtechforge.mrtfnfc

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.Build
import android.os.Bundle
import android.util.Log
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

        if (nfcAdapter == null) {
            toast("This device does not support NFC.")
            // You could also disable buttons or finish the activity here.
            Log.e("MRTF", "NFC adapter is null (no NFC hardware).")
        } else if (nfcAdapter?.isEnabled == false) {
            toast("NFC is disabled. Please enable it in system settings.")
            Log.w("MRTF", "NFC is present but disabled.")
        }

        // PendingIntent for foreground dispatch
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            flags
        )

        // --- Hook up buttons (select write payloads) ---
        findViewById<Button>(R.id.btnWifi).setOnClickListener {
            tagPayload = "mrtf://wifi-toggle".toByteArray()
            toast("Ready to write Wi-Fi Toggle tag")
        }

        findViewById<Button>(R.id.btnBluetooth).setOnClickListener {
            tagPayload = "mrtf://bt-toggle".toByteArray()
            toast("Ready to write Bluetooth Toggle tag")
        }

        findViewById<Button>(R.id.btnDND).setOnClickListener {
            tagPayload = "mrtf://dnd".toByteArray()
            toast("Ready to write DND tag")
        }

        findViewById<Button>(R.id.btnCarLocation).setOnClickListener {
            tagPayload = "mrtf://car-location".toByteArray()
            toast("Ready to write Car Location tag")
        }

        findViewById<Button>(R.id.btnSOSText).setOnClickListener {
            tagPayload = "mrtf://sos".toByteArray()
            toast("Ready to write SOS Text tag")
        }

        findViewById<Button>(R.id.btnBedtime).setOnClickListener {
            tagPayload = "mrtf://bedtime".toByteArray()
            toast("Ready to write Bedtime tag")
        }

        findViewById<Button>(R.id.btnOpenApp).setOnClickListener {
            tagPayload = "mrtf://openapp".toByteArray()
            toast("Ready to write App Launcher tag")
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("MRTF", "onResume: enabling NFC foreground dispatch")
        nfcAdapter?.enableForegroundDispatch(
            this,
            pendingIntent,
            null,   // no specific intent filters for now
            null    // no tech filters
        )
    }

    override fun onPause() {
        super.onPause()
        Log.d("MRTF", "onPause: disabling NFC foreground dispatch")
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d("MRTF", "onNewIntent: $intent")

        val tag = intent?.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)

        if (tag == null) {
            Log.w("MRTF", "No Tag found in onNewIntent")
            toast("No NFC tag detected.")
            return
        }

        if (tagPayload == null) {
            Log.w("MRTF", "Tag tapped but no payload selected")
            toast("Tap a button first to choose what to write.")
            return
        }

        val payload = tagPayload!!
        val record = NdefRecord.createMime("text/plain", payload)
        val message = NdefMessage(arrayOf(record))

        writeNdefToTag(tag, message)
    }

    private fun writeNdefToTag(tag: Tag, message: NdefMessage) {
        try {
            val ndef = Ndef.get(tag)

            if (ndef != null) {
                ndef.connect()

                if (!ndef.isWritable) {
                    toast("Tag is read-only.")
                    Log.e("MRTF", "NDEF tag is read-only")
                    ndef.close()
                    return
                }

                val size = message.toByteArray().size
                if (ndef.maxSize < size) {
                    toast("Tag is too small (${ndef.maxSize} bytes) for this message ($size bytes).")
                    Log.e("MRTF", "Tag too small: max=${ndef.maxSize}, needed=$size")
                    ndef.close()
                    return
                }

                ndef.writeNdefMessage(message)
                ndef.close()

                toast("Tag written successfully!")
                Log.i("MRTF", "Tag written successfully.")
            } else {
                // Tag is not NDEF formatted – try to format it
                val format = NdefFormatable.get(tag)
                if (format != null) {
                    format.connect()
                    format.format(message)
                    format.close()
                    toast("Tag formatted and written successfully!")
                    Log.i("MRTF", "Tag formatted and written successfully.")
                } else {
                    toast("This tag does not support NDEF.")
                    Log.e("MRTF", "Tag does not support NDEF or NdefFormatable.")
                }
            }
        } catch (e: Exception) {
            toast("Write failed: ${e.message}")
            Log.e("MRTF", "Error writing tag", e)
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}