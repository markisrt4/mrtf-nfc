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
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private val TAG = "MRTF"

    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null
    private var tagPayload: ByteArray? = null   // non-null = WRITE MODE

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

        // --- Writer buttons ---
        findViewById<Button>(R.id.btnWifi).setOnClickListener {
            tagPayload = buildPayload("wifi-toggle")
            toast("Ready to write Wi-Fi Toggle tag")
        }

        findViewById<Button>(R.id.btnBluetooth).setOnClickListener {
            tagPayload = buildPayload("bt-toggle")
            toast("Ready to write Bluetooth Toggle tag")
        }

        findViewById<Button>(R.id.btnDND).setOnClickListener {
            tagPayload = buildPayload("dnd")
            toast("Ready to write DND tag")
        }

        findViewById<Button>(R.id.btnCarLocation).setOnClickListener {
            tagPayload = buildPayload("car-location")
            toast("Ready to write Car Location tag")
        }

        findViewById<Button>(R.id.btnSOSText).setOnClickListener {
            tagPayload = buildPayload("sos")
            toast("Ready to write SOS tag")
        }

        findViewById<Button>(R.id.btnBedtime).setOnClickListener {
            tagPayload = buildPayload("bedtime")
            toast("Ready to write Bedtime tag")
        }

        findViewById<Button>(R.id.btnOpenApp).setOnClickListener {
            tagPayload = buildPayload("openapp")
            toast("Ready to write App Launcher tag")
        }
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        val tag = intent?.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        if (tag == null) {
            toast("No NFC tag detected.")
            return
        }

        if (tagPayload != null) {
            writeNdefToTag(tag, tagPayload!!)
        } else {
            readAndRouteTag(tag)
        }
    }

    // ---------- PAYLOAD LAYER ----------

    private fun buildPayload(action: String): ByteArray {
        val json = JSONObject()
        json.put("version", 1)
        json.put("action", action)
        return json.toString().toByteArray(Charsets.UTF_8)
    }

    // ---------- WRITE ----------

    private fun writeNdefToTag(tag: Tag, payload: ByteArray) {
        try {
            val record = NdefRecord.createMime("text/plain", payload)
            val message = NdefMessage(arrayOf(record))

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

            } else {
                val format = NdefFormatable.get(tag)
                if (format != null) {
                    format.connect()
                    format.format(message)
                    format.close()
                } else {
                    toast("Tag does not support NDEF.")
                    return
                }
            }

            toast("Tag written successfully.")
            tagPayload = null

        } catch (e: Exception) {
            toast("Write failed: ${e.message}")
            Log.e(TAG, "Write error", e)
        }
    }

    // ---------- READ + ROUTE ----------

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
            if (record.tnf != NdefRecord.TNF_MIME_MEDIA ||
                String(record.type) != "text/plain"
            ) {
                toast("Not an MRTF tag.")
                return
            }

            val json = JSONObject(String(record.payload, Charsets.UTF_8))
            val action = json.getString("action")

            Log.i(TAG, "Routing action: $action")
            routeMrtfAction(action)

        } catch (e: Exception) {
            toast("Read failed: ${e.message}")
            Log.e(TAG, "Read error", e)
        }
    }

    private fun routeMrtfAction(action: String) {
        when (action) {
            "wifi-toggle" -> startActivity(Intent(this, WifiActionActivity::class.java))
            "bt-toggle" -> startActivity(Intent(this, BluetoothActionActivity::class.java))
            "dnd", "bedtime" -> startActivity(Intent(this, BedtimeActionActivity::class.java))
            "car-location" -> toast("Car location coming soon.")
            "sos" -> startActivity(Intent(this, SosActivity::class.java))
            "openapp" -> toast("MRTF app opened from tag.")
            else -> toast("Unknown MRTF action: $action")
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}