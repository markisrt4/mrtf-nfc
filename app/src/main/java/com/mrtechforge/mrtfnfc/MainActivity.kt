package com.mrtechforge.mrtfnfc

import com.mrtechforge.mrtfnfc.actions.*
import com.mrtechforge.mrtfnfc.debug.DebugConfig
import com.mrtechforge.mrtfnfc.debug.DebugPayloadActivity

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

    private val TAG = "MRTF"

    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null

    // non-null = WRITE MODE
    private var tagPayload: ByteArray? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            toast("This device does not support NFC.")
            Log.e(TAG, "NFC adapter is null.")
        } else if (nfcAdapter?.isEnabled == false) {
            toast("NFC is disabled. Please enable it in system settings.")
            Log.w(TAG, "NFC present but disabled.")
        }

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

        // --------------------------------------------------
        // Writer buttons
        // --------------------------------------------------

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
            toast("Ready to write SOS tag")
        }

        findViewById<Button>(R.id.btnBedtime).setOnClickListener {
            tagPayload = "mrtf://bedtime".toByteArray()
            toast("Ready to write Bedtime tag")
        }

        val openBtn = findViewById<Button>(R.id.btnOpenApp)
        openBtn.setOnClickListener {
            tagPayload = "mrtf://openapp".toByteArray()
            toast("Ready to write App Launcher tag")
        }

        // --------------------------------------------------
        // DEBUG TOGGLE (long-press, intentional)
        // --------------------------------------------------
        openBtn.setOnLongClickListener {
            DebugConfig.ENABLED = !DebugConfig.ENABLED
            toast("Debug mode: ${if (DebugConfig.ENABLED) "ON" else "OFF"}")
            true
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: enabling foreground dispatch")
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause: disabling foreground dispatch")
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent: $intent")

        val tag = intent?.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        if (tag == null) {
            toast("No NFC tag detected.")
            return
        }

        if (tagPayload != null) {
            // ---------------- WRITE MODE ----------------
            val payload = tagPayload!!
            val record = NdefRecord.createMime("text/plain", payload)
            val message = NdefMessage(arrayOf(record))
            writeNdefToTag(tag, message)
        } else {
            // ---------------- READ MODE ----------------
            readAndRouteTag(tag)
        }
    }

    // --------------------------------------------------
    // WRITE HELPERS
    // --------------------------------------------------

    private fun writeNdefToTag(tag: Tag, message: NdefMessage) {
        try {
            val ndef = Ndef.get(tag)

            if (ndef != null) {
                ndef.connect()

                if (!ndef.isWritable) {
                    toast("Tag is read-only.")
                    ndef.close()
                    return
                }

                if (ndef.maxSize < message.toByteArray().size) {
                    toast("Tag is too small.")
                    ndef.close()
                    return
                }

                ndef.writeNdefMessage(message)
                ndef.close()

                toast("Tag written successfully.")
                tagPayload = null
            } else {
                val format = NdefFormatable.get(tag)
                if (format != null) {
                    format.connect()
                    format.format(message)
                    format.close()
                    toast("Tag formatted + written.")
                    tagPayload = null
                } else {
                    toast("Tag does not support NDEF.")
                }
            }
        } catch (e: Exception) {
            toast("Write failed: ${e.message}")
            Log.e(TAG, "Write error", e)
        }
    }

    // --------------------------------------------------
    // READ + ROUTER
    // --------------------------------------------------

    private fun readAndRouteTag(tag: Tag) {
        try {
            val ndef = Ndef.get(tag) ?: run {
                toast("Tag is not NDEF formatted.")
                return
            }

            ndef.connect()
            val message = ndef.cachedNdefMessage ?: ndef.ndefMessage
            ndef.close()

            if (message == null || message.records.isEmpty()) {
                toast("Tag is empty.")
                return
            }

            val record = message.records[0]
            val actionString = String(record.payload, Charsets.UTF_8)

            // ---------------- DEBUG VIEW ----------------
            if (DebugConfig.ENABLED) {
                val dbg = Intent(this, DebugPayloadActivity::class.java)
                dbg.putExtra("payload", actionString)
                startActivity(dbg)
            }

            routeMrtfAction(actionString)

        } catch (e: Exception) {
            toast("Read failed: ${e.message}")
            Log.e(TAG, "Read error", e)
        }
    }

    private fun routeMrtfAction(action: String) {
        when (action) {
            "mrtf://wifi-toggle" -> {
                startActivity(Intent(this, WifiActionActivity::class.java))
            }
            "mrtf://bt-toggle" -> {
                startActivity(Intent(this, BluetoothActionActivity::class.java))
            }
            "mrtf://dnd", "mrtf://bedtime" -> {
                startActivity(Intent(this, BedtimeActionActivity::class.java))
            }
            "mrtf://sos" -> {
                startActivity(Intent(this, SosActivity::class.java))
            }
            "mrtf://openapp" -> {
                toast("MRTF opened from tag.")
            }
            else -> {
                toast("Unknown MRTF action: $action")
            }
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}