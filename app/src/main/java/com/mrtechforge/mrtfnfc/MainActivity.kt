package com.mrtechforge.mrtfnfc

import com.mrtechforge.mrtfnfc.actions.*
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
    private var tagPayload: ByteArray? = null  // non-null = WRITE MODE

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

        // --- Writer buttons: select payload to write ---
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
            Log.w(TAG, "No Tag in intent")
            toast("No NFC tag detected.")
            return
        }

        // If we have a payload selected, we are in WRITE mode.
        if (tagPayload != null) {
            Log.d(TAG, "Write mode: writing payload to tag")
            val payload = tagPayload!!
            val record = NdefRecord.createMime("text/plain", payload)
            val message = NdefMessage(arrayOf(record))
            writeNdefToTag(tag, message)
        } else {
            // No payload selected → READER/CLIENT mode.
            Log.d(TAG, "Read mode: reading tag and routing action")
            readAndRouteTag(tag)
        }
    }

    // --- WRITE HELPERS ---

    private fun writeNdefToTag(tag: Tag, message: NdefMessage) {
        try {
            val ndef = Ndef.get(tag)

            if (ndef != null) {
                ndef.connect()

                if (!ndef.isWritable) {
                    toast("Tag is read-only.")
                    Log.e(TAG, "NDEF tag is read-only")
                    ndef.close()
                    return
                }

                val size = message.toByteArray().size
                if (ndef.maxSize < size) {
                    toast("Tag is too small (${ndef.maxSize} bytes) for this message ($size bytes).")
                    Log.e(TAG, "Tag too small: max=${ndef.maxSize}, needed=$size")
                    ndef.close()
                    return
                }

                ndef.writeNdefMessage(message)
                ndef.close()

                toast("Tag written successfully!")
                Log.i(TAG, "Tag written successfully.")

                // after successful write, switch back to reader mode
                tagPayload = null
            } else {
                val format = NdefFormatable.get(tag)
                if (format != null) {
                    format.connect()
                    format.format(message)
                    format.close()
                    toast("Tag formatted and written successfully!")
                    Log.i(TAG, "Tag formatted and written successfully.")

                    // after successful write, switch back to reader mode
                    tagPayload = null
                } else {
                    toast("This tag does not support NDEF.")
                    Log.e(TAG, "Tag not NDEF/NdefFormatable.")
                }
            }
        } catch (e: Exception) {
            toast("Write failed: ${e.message}")
            Log.e(TAG, "Error writing tag", e)
        }
    }

    // --- READ + ROUTER ---

    private fun readAndRouteTag(tag: Tag) {
        try {
            val ndef = Ndef.get(tag)
            if (ndef == null) {
                toast("Tag is not NDEF formatted.")
                Log.e(TAG, "Tag is not NDEF")
                return
            }

            ndef.connect()
            val message = ndef.cachedNdefMessage ?: ndef.ndefMessage
            ndef.close()

            if (message == null || message.records.isEmpty()) {
                toast("Tag is empty.")
                Log.w(TAG, "NDEF message is null or empty")
                return
            }

            val record = message.records[0]
            if (record.tnf != NdefRecord.TNF_MIME_MEDIA ||
                String(record.type, Charsets.US_ASCII) != "text/plain"
            ) {
                toast("Tag is not an MRTF action tag.")
                Log.w(TAG, "Unexpected record type: tnf=${record.tnf}, type=${String(record.type)}")
                return
            }

            val actionString = String(record.payload, Charsets.UTF_8)
            Log.d(TAG, "Read MRTF action payload: $actionString")
            routeMrtfAction(actionString)

        } catch (e: Exception) {
            toast("Failed to read tag: ${e.message}")
            Log.e(TAG, "Error reading tag", e)
        }
    }

    private fun routeMrtfAction(action: String) {
        when (action) {
            "mrtf://wifi-toggle" -> {
                Log.i(TAG, "Route: wifi-toggle")
                startActivity(Intent(this, WifiActionActivity::class.java))
            }
            "mrtf://bt-toggle" -> {
                Log.i(TAG, "Route: bt-toggle")
                startActivity(Intent(this, BluetoothActionActivity::class.java))
            }
            "mrtf://dnd", "mrtf://bedtime" -> {
                Log.i(TAG, "Route: bedtime/dnd")
                startActivity(Intent(this, BedtimeActionActivity::class.java))
            }
            "mrtf://car-location" -> {
                Log.i(TAG, "Route: car-location")
                // TODO: CarLocationActivity later
                toast("Car location screen coming soon.")
            }
            "mrtf://sos" -> {
                Log.i(TAG, "Route: sos")
                startActivity(Intent(this, SosActivity::class.java))
            }
            "mrtf://openapp" -> {
                Log.i(TAG, "Route: openapp")
                // For now, we're already in MainActivity; later you can show a dashboard
                toast("MRTF app opened from tag.")
            }
            else -> {
                toast("Unknown MRTF action: $action")
                Log.w(TAG, "Unknown MRTF action: $action")
            }
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}