package com.mrtechforge.mrtfnfc

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
import com.mrtechforge.mrtfnfc.actions.BedtimeActionActivity
import com.mrtechforge.mrtfnfc.actions.BluetoothActionActivity
import com.mrtechforge.mrtfnfc.actions.SosActivity
import com.mrtechforge.mrtfnfc.actions.WifiActionActivity
import com.mrtechforge.mrtfnfc.debug.DebugPayloadActivity

class MainActivity : AppCompatActivity() {

    private val TAG = "MRTF"

    private val MRTF_MIME = "application/mrtf"
    private val MRTF_MIME_LEGACY = "application/vnd.mrtechforge.mrtf"

    // If user doesn't have the app, scanning the tag should open this page
    // (replace with your real landing page or Play Store link later)
    private val INSTALL_URL = "https://mrtechforge.com/mrtf-nfc"

    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null

    // If non-null => WRITE MODE (contains MRTF frame bytes)
    private var pendingWriteFrame: ByteArray? = null

    private lateinit var prefs: SharedPreferences

    // Keep stable once you write a bunch of tags
    private object Cmd {
        const val WIFI_TOGGLE = 0x01
        const val BT_TOGGLE = 0x02
        const val DND = 0x03
        const val CAR_LOCATION = 0x04
        const val SOS = 0x05
        const val BEDTIME = 0x06
        const val OPEN_APP = 0x07
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("mrtf_prefs", MODE_PRIVATE)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            toast("This device does not support NFC.")
        } else if (nfcAdapter?.isEnabled == false) {
            toast("NFC is disabled. Please enable it in system settings.")
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

        // Writer buttons (match your activity_main.xml IDs)
        findViewById<Button>(R.id.btnWifi).setOnClickListener {
            pendingWriteFrame = MrtfProtocolV1.buildCommandFrame(Cmd.WIFI_TOGGLE, includeCrc = true)
            toast("Ready to write MRTF Wi-Fi tag")
        }

        findViewById<Button>(R.id.btnBluetooth).setOnClickListener {
            pendingWriteFrame = MrtfProtocolV1.buildCommandFrame(Cmd.BT_TOGGLE, includeCrc = true)
            toast("Ready to write MRTF Bluetooth tag")
        }

        findViewById<Button>(R.id.btnDND).setOnClickListener {
            pendingWriteFrame = MrtfProtocolV1.buildCommandFrame(Cmd.DND, includeCrc = true)
            toast("Ready to write MRTF DND tag")
        }

        findViewById<Button>(R.id.btnCarLocation).setOnClickListener {
            pendingWriteFrame = MrtfProtocolV1.buildCommandFrame(Cmd.CAR_LOCATION, includeCrc = true)
            toast("Ready to write MRTF Car Location tag")
        }

        findViewById<Button>(R.id.btnSOSText).setOnClickListener {
            pendingWriteFrame = MrtfProtocolV1.buildCommandFrame(Cmd.SOS, includeCrc = true)
            toast("Ready to write MRTF SOS tag")
        }

        findViewById<Button>(R.id.btnBedtime).setOnClickListener {
            pendingWriteFrame = MrtfProtocolV1.buildCommandFrame(Cmd.BEDTIME, includeCrc = true)
            toast("Ready to write MRTF Bedtime tag")
        }

        findViewById<Button>(R.id.btnOpenApp).setOnClickListener {
            pendingWriteFrame = MrtfProtocolV1.buildCommandFrame(Cmd.OPEN_APP, includeCrc = true)
            toast("Ready to write MRTF Open App tag")
        }

        // Long-press Open App toggles debug
        findViewById<Button>(R.id.btnOpenApp).setOnLongClickListener {
            val enabled = !isDebugEnabled()
            setDebugEnabled(enabled)
            toast("Debug mode ${if (enabled) "ENABLED" else "DISABLED"}")
            true
        }

        // Handle NFC if app was cold-launched by a scan
        handleNfcIntent(intent)
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
        handleNfcIntent(intent)
    }

    private fun handleNfcIntent(intent: Intent?) {
        val tag = intent?.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return

        // WRITE MODE
        pendingWriteFrame?.let { frame ->
            val msg = buildNdefMessageForMrtf(frame)
            writeNdefToTag(tag, msg)
            return
        }

        // READ MODE
        readAndRouteTag(tag)
    }

    // ---------------- WRITE ----------------

    /**
     * Best-practice record order:
     *  1) URI (install page) -> helps when app isn't installed
     *  2) MIME (application/mrtf) -> your binary MRTF payload
     *  3) AAR -> prefers your app when installed
     *  4) TEXT (optional) -> human readable
     */
    private fun buildNdefMessageForMrtf(frame: ByteArray): NdefMessage {
        val uriRecord = NdefRecord.createUri(INSTALL_URL)
        val mrtfRecord = NdefRecord.createMime(MRTF_MIME, frame)
        val aarRecord = NdefRecord.createApplicationRecord(packageName)

        val fallbackText = "MRTF tag detected. Install MRTF NFC: $INSTALL_URL"
        val textRecord = createTextRecord(lang = "en", text = fallbackText)

        return NdefMessage(arrayOf(uriRecord, mrtfRecord, aarRecord, textRecord))
    }

    private fun createTextRecord(lang: String, text: String): NdefRecord {
        val langBytes = lang.toByteArray(Charsets.US_ASCII)
        val textBytes = text.toByteArray(Charsets.UTF_8)

        // [status][lang][text]
        val payload = ByteArray(1 + langBytes.size + textBytes.size)
        payload[0] = (langBytes.size and 0x3F).toByte()
        System.arraycopy(langBytes, 0, payload, 1, langBytes.size)
        System.arraycopy(textBytes, 0, payload, 1 + langBytes.size, textBytes.size)

        return NdefRecord(
            NdefRecord.TNF_WELL_KNOWN,
            NdefRecord.RTD_TEXT,
            ByteArray(0),
            payload
        )
    }

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
                pendingWriteFrame = null
            } else {
                val format = NdefFormatable.get(tag)
                if (format != null) {
                    format.connect()
                    format.format(message)
                    format.close()
                    toast("Tag formatted and written!")
                    pendingWriteFrame = null
                } else {
                    toast("Tag does not support NDEF.")
                }
            }
        } catch (e: Exception) {
            toast("Write failed: ${e.message}")
            Log.e(TAG, "Write error", e)
        }
    }

    // ---------------- READ ----------------

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

            // Prefer MRTF binary record
            val mrtfRecord = message.records.firstOrNull { r ->
                r.tnf == NdefRecord.TNF_MIME_MEDIA &&
                    (String(r.type, Charsets.US_ASCII) == MRTF_MIME ||
                     String(r.type, Charsets.US_ASCII) == MRTF_MIME_LEGACY)
            }

            if (mrtfRecord != null) {
                val frame = mrtfRecord.payload
                val parsedNullable = MrtfProtocolV1.parse(frame)

                if (parsedNullable == null) {
                    toast("Read failed: Invalid MRTF frame")
                    return
                }
                val parsed = parsedNullable // non-null alias

                // CRC enforcement (only when present)
                if (parsed.crcPresent && parsed.crcValid == false) {
                    toast("Read failed: checksum mismatch")
                    if (isDebugEnabled()) {
                        openDebug(frame, parsed, extra = "CRC INVALID")
                    }
                    return
                }

                if (isDebugEnabled()) {
                    openDebug(frame, parsed, extra = null)
                }

                routeCommandId(parsed.commandId)
                return
            }

            // Legacy: text/plain MIME record containing "mrtf://..."
            val legacyRecord = message.records.firstOrNull { r ->
                r.tnf == NdefRecord.TNF_MIME_MEDIA &&
                    String(r.type, Charsets.US_ASCII) == "text/plain"
            }

            if (legacyRecord != null) {
                val actionString = String(legacyRecord.payload, Charsets.UTF_8)
                routeLegacyString(actionString)
                return
            }

            toast("Unsupported tag type.")
        } catch (e: Exception) {
            toast("Read failed: ${e.message}")
            Log.e(TAG, "Read error", e)
        }
    }

    private fun openDebug(frame: ByteArray, parsed: MrtfProtocolV1.ParsedFrame, extra: String?) {
        val hex = frame.joinToString(" ") { b -> "%02X".format(b) }

        val cmd = parsed.commandId?.let { "0x%02X".format(it) } ?: "null"
        val crcLine = if (parsed.crcPresent) {
            val exp = parsed.crcExpected?.let { "0x%04X".format(it) } ?: "null"
            val comp = parsed.crcComputed?.let { "0x%04X".format(it) } ?: "null"
            val ok = if (parsed.crcValid == true) "VALID" else "INVALID"
            " CRC=$ok exp=$exp comp=$comp"
        } else {
            " CRC=(none)"
        }

        val summary = buildString {
            append("TYPE=0x%02X FLAGS=0x%02X CMD=%s".format(parsed.type, parsed.flags, cmd))
            append(crcLine)
            if (!extra.isNullOrBlank()) append(" [$extra]")
        }

        startActivity(
            Intent(this, DebugPayloadActivity::class.java).apply {
                putExtra("raw_payload", hex)
                putExtra("parsed_action", summary)
                putExtra("timestamp", System.currentTimeMillis().toString())
            }
        )
    }

    private fun routeCommandId(commandId: Int?) {
        when (commandId) {
            Cmd.WIFI_TOGGLE -> startActivity(Intent(this, WifiActionActivity::class.java))
            Cmd.BT_TOGGLE -> startActivity(Intent(this, BluetoothActionActivity::class.java))
            Cmd.DND, Cmd.BEDTIME -> startActivity(Intent(this, BedtimeActionActivity::class.java))
            Cmd.CAR_LOCATION -> toast("Car location coming soon")
            Cmd.SOS -> startActivity(Intent(this, SosActivity::class.java))
            Cmd.OPEN_APP -> toast("MRTF app opened")
            null -> toast("Read failed: MRTF missing Command ID (field 0x10)")
            else -> toast("Unknown MRTF command: 0x%02X".format(commandId))
        }
    }

    private fun routeLegacyString(action: String) {
        when (action) {
            "mrtf://wifi-toggle" -> routeCommandId(Cmd.WIFI_TOGGLE)
            "mrtf://bt-toggle" -> routeCommandId(Cmd.BT_TOGGLE)
            "mrtf://dnd" -> routeCommandId(Cmd.DND)
            "mrtf://bedtime" -> routeCommandId(Cmd.BEDTIME)
            "mrtf://car-location" -> routeCommandId(Cmd.CAR_LOCATION)
            "mrtf://sos" -> routeCommandId(Cmd.SOS)
            "mrtf://openapp" -> routeCommandId(Cmd.OPEN_APP)
            else -> toast("Unknown legacy MRTF action: $action")
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