package com.mrtechforge.mrtfnfc

import com.mrtechforge.mrtfnfc.actions.*
import com.mrtechforge.mrtfnfc.debug.DebugPayloadActivity
import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
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
    private var tagPayload: ByteArray? = null  // non-null = WRITE MODE

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("mrtf_prefs", MODE_PRIVATE)

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

        // ---------------- WRITER BUTTONS ----------------

        findViewById<Button>(R.id.btnWifi).setOnClickListener {
            tagPayload = "mrtf://wifi-toggle".toByteArray()
            toast("Ready to write Wi-Fi tag")
        }

        findViewById<Button>(R.id.btnBluetooth).setOnClickListener {
            tagPayload = "mrtf://bt-toggle".toByteArray()
            toast("Ready to write Bluetooth tag")
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

        findViewById<Button>(R.id.btnOpenApp).setOnClickListener {
            tagPayload = "mrtf://openapp".toByteArray()
            toast("Ready to write App Launcher tag")
        }

        // ---------------- DEBUG TOGGLE ----------------
        // Long-press Open App button to toggle debug mode
        findViewById<Button>(R.id.btnOpenApp).setOnLongClickListener {
            val enabled = !isDebugEnabled()
            setDebugEnabled(enabled)
            toast("Debug mode ${if (enabled) "ENABLED" else "DISABLED"}")
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
            Log.w(TAG, "No Tag in intent")
            return
        }

        // WRITE MODE
        if (tagPayload != null) {
            val payload = tagPayload!!
            val record = NdefRecord.createMime("text/plain", payload)
            val message = NdefMessage(arrayOf(record))
            writeNdefToTag(tag, message)
        } else {
            // READ MODE
            readAndRouteTag(tag)
        }
    }

    // ---------------- WRITE HELPERS ----------------

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
                    toast("Tag too small for payload.")
                    ndef.close()
                    return
                }

                ndef.writeNdefMessage(message)
                ndef.close()

                toast("Tag written successfully!")
                tagPayload = null
            } else {
                val format = NdefFormatable.get(tag)
                if (format != null) {
                    format.connect()
                    format.format(message)
                    format.close()
                    toast("Tag formatted and written!")
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

    // ---------------- READ + ROUTER ----------------

    private fun readAndRouteTag(tag: Tag) {
        try {
            val ndef = Ndef.get(tag)
            if (ndef == null) {
                toast("Tag is not NDEF formatted.")
                return
            }

            ndef.connect()
            val message = ndef.cachedNdefMessage ?: ndef.ndefMessage
            ndef.close()

            if (message == null || message.records.isEmpty()) {
                toast("Empty tag.")
                return
            }

            val record = message.records[0]
            if (record.tnf != NdefRecord.TNF_MIME_MEDIA ||
                String(record.type, Charsets.US_ASCII) != "text/plain"
            ) {
                toast("Unsupported tag type.")
                return
            }

            val actionString = String(record.payload, Charsets.UTF_8)
            Log.d(TAG, "Read payload: $actionString")

            // ---- DEBUG VIEWER (OPTION C) ----
            if (isDebugEnabled()) {
                startActivity(
                    Intent(this, DebugPayloadActivity::class.java).apply {
                        putExtra("raw_payload", actionString)
                        putExtra("parsed_action", actionString.substringAfter("mrtf://"))
                        putExtra("timestamp", System.currentTimeMillis().toString())
                    }
                )
            }

            routeMrtfAction(actionString)

        } catch (e: Exception) {
            toast("Read failed: ${e.message}")
            Log.e(TAG, "Read error", e)
        }
    }

    private fun routeMrtfAction(action: String) {
        when (action) {
            "mrtf://wifi-toggle" -> startActivity(Intent(this, WifiActionActivity::class.java))
            "mrtf://bt-toggle" -> startActivity(Intent(this, BluetoothActionActivity::class.java))
            "mrtf://dnd", "mrtf://bedtime" -> startActivity(Intent(this, BedtimeActionActivity::class.java))
            "mrtf://car-location" -> toast("Car location coming soon")
            "mrtf://sos" -> startActivity(Intent(this, SosActivity::class.java))
            "mrtf://openapp" -> toast("MRTF app opened")
            else -> toast("Unknown MRTF action: $action")
        }
    }

    // ---------------- DEBUG PREFS ----------------

    private fun isDebugEnabled(): Boolean =
        prefs.getBoolean("debug_enabled", false)

    private fun setDebugEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("debug_enabled", enabled).apply()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}