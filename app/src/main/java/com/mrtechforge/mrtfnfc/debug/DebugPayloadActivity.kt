package com.mrtechforge.mrtfnfc.debug

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.mrtechforge.mrtfnfc.MrtfProtocolV1
import com.mrtechforge.mrtfnfc.R

class DebugPayloadActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug_payload)

        val txtPayload = findViewById<TextView>(R.id.txtRawPayload)
        val txtAction = findViewById<TextView>(R.id.txtParsedAction)
        val txtTimestamp = findViewById<TextView>(R.id.txtTimestamp)

        val raw = intent.getStringExtra("raw_payload") ?: ""
        val ts = intent.getStringExtra("timestamp") ?: "N/A"

        txtTimestamp.text = ts

        val bytes = decodeHexStringOrNull(raw)

        if (bytes == null) {
            // Not hex; show as-is
            txtPayload.text = if (raw.isBlank()) "N/A" else raw
            txtAction.text = intent.getStringExtra("parsed_action") ?: "N/A"
            return
        }

        // Show normalized hex as "raw"
        txtPayload.text = bytes.joinToString(" ") { "%02X".format(it) }

        val parsed = MrtfProtocolV1.parse(bytes)
        if (parsed == null) {
            txtAction.text = buildString {
                appendLine("Invalid MRTF frame (parse returned null)")
                appendLine()
                append(intent.getStringExtra("parsed_action") ?: "N/A")
            }
            return
        }

        txtAction.text = buildString {
            appendLine("HEADER/PARSED")
            appendLine("  Version: 0x%02X".format(parsed.version))
            appendLine("  Type: 0x%02X".format(parsed.type))
            appendLine("  Flags: 0x%02X".format(parsed.flags))
            appendLine("  Payload Len: %d".format(parsed.payloadLength))
            appendLine()
            appendLine("  Command ID: ${parsed.commandId?.let { "0x%02X".format(it) } ?: "null"}")
            appendLine("  Command Value: ${parsed.commandValue?.let { it.toHex() } ?: "null"}")
            appendLine()
            appendLine("TLVS")
            if (parsed.tlvs.isEmpty()) {
                appendLine("  (none)")
            } else {
                for (t in parsed.tlvs) {
                    appendLine("  0x%02X len=%d value=%s".format(t.fieldId, t.length, t.value.toHex()))
                }
            }
            appendLine()
            appendLine("CRC16")
            if (!parsed.crcPresent) {
                appendLine("  (not present)")
            } else {
                appendLine("  Expected: ${parsed.crcExpected?.let { "0x%04X".format(it) } ?: "null"}")
                appendLine("  Computed: ${parsed.crcComputed?.let { "0x%04X".format(it) } ?: "null"}")
                appendLine("  Status: ${if (parsed.crcValid == true) "VALID" else "INVALID"}")
            }
        }
    }

    private fun decodeHexStringOrNull(s: String): ByteArray? {
        val trimmed = s.trim()
        if (trimmed.isEmpty()) return null

        val cleaned = trimmed
            .replace("0x", "", ignoreCase = true)
            .replace(Regex("[^0-9A-Fa-f]"), "")

        if (cleaned.length < 2 || cleaned.length % 2 != 0) return null

        return try {
            val out = ByteArray(cleaned.length / 2)
            var i = 0
            while (i < cleaned.length) {
                out[i / 2] = cleaned.substring(i, i + 2).toInt(16).toByte()
                i += 2
            }
            out
        } catch (_: Exception) {
            null
        }
    }

    private fun ByteArray.toHex(): String =
        if (isEmpty()) "(empty)" else joinToString(" ") { "%02X".format(it) }
}