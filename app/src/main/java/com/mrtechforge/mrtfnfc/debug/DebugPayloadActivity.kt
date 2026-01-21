package com.mrtechforge.mrtfnfc.debug

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.mrtechforge.mrtfnfc.MrtfProtocolV1
import com.mrtechforge.mrtfnfc.R
import java.util.Locale

class DebugPayloadActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug_payload)

        val txtPayload = findViewById<TextView>(R.id.txtRawPayload)
        val txtAction = findViewById<TextView>(R.id.txtParsedAction)
        val txtTimestamp = findViewById<TextView>(R.id.txtTimestamp)

        // Optional: add these buttons to your layout (recommended).
        // If you don't have them yet, just comment out these 2 lines and the click listeners.

        val rawPayload = intent.getStringExtra("raw_payload") ?: ""
        val parsedAction = intent.getStringExtra("parsed_action") ?: "N/A"
        val ts = intent.getStringExtra("timestamp") ?: "N/A"

        txtTimestamp.text = ts

        // If rawPayload looks like hex ("AA BB CC"), decode it. Otherwise treat as plain text.
        val bytes = decodeHexStringOrNull(rawPayload)

        if (bytes == null) {
            // Legacy / non-hex debug payload
            txtPayload.text = if (rawPayload.isNotBlank()) rawPayload else "N/A"
            txtAction.text = parsedAction
            hookCopyButtons(btnCopyRaw, btnCopyDecoded, rawPayload, parsedAction)
            return
        }

        // Hex bytes present: decode MRTF
        val decoded = decodeMrtf(bytes)

        txtPayload.text = decoded.rawHex
        txtAction.text = decoded.pretty

        hookCopyButtons(btnCopyRaw, btnCopyDecoded, decoded.rawHex, decoded.pretty)
    }

    private data class DecodeResult(
        val rawHex: String,
        val pretty: String
    )

    private fun decodeMrtf(bytes: ByteArray): DecodeResult {
        val rawHex = bytes.joinToString(" ") { "%02X".format(it) }

        // Quick header extraction for display even if parse fails
        val headerSummary = if (bytes.size >= 7) {
            val magic0 = bytes[0]
            val magic1 = bytes[1]
            val version = bytes[2].toInt() and 0xFF
            val type = bytes[3].toInt() and 0xFF
            val flags = bytes[4].toInt() and 0xFF
            val len = ((bytes[5].toInt() and 0xFF) shl 8) or (bytes[6].toInt() and 0xFF)

            buildString {
                appendLine("HEADER")
                appendLine("  Magic: %02X %02X".format(magic0, magic1))
                appendLine("  Version: 0x%02X".format(version))
                appendLine("  Type: 0x%02X".format(type))
                appendLine("  Flags: 0x%02X".format(flags))
                appendLine("  Payload Len: %d (0x%04X)".format(len, len))
                appendLine()
            }
        } else {
            "HEADER\n  (too short)\n\n"
        }

        val parsed = MrtfProtocolV1.parse(bytes)
        if (parsed == null) {
            val pretty = headerSummary + "PARSE\n  Invalid MRTF frame (parse returned null)\n"
            return DecodeResult(rawHex = rawHex, pretty = pretty)
        }

        val pretty = buildString {
            append(headerSummary)

            appendLine("PARSED")
            appendLine("  Command ID: ${parsed.commandId?.let { "0x%02X".format(it) } ?: "null"}")
            appendLine("  Command Value: ${parsed.commandValue?.let { it.toHex() } ?: "null"}")
            appendLine()

            appendLine("TLVS")
            if (parsed.tlvs.isEmpty()) {
                appendLine("  (none)")
            } else {
                for (t in parsed.tlvs) {
                    val field = "0x%02X".format(t.fieldId)
                    val len = t.length
                    val valueHex = t.value.toHex()

                    appendLine("  $field  len=$len  value=$valueHex")
                }
            }
            appendLine()

            appendLine("CRC16")
            if (!parsed.crcPresent) {
                appendLine("  (not present)")
            } else {
                val expected = parsed.crcExpected?.let { "0x%04X".format(it) } ?: "null"
                val computed = parsed.crcComputed?.let { "0x%04X".format(it) } ?: "null"
                val valid = when (parsed.crcValid) {
                    true -> "VALID"
                    false -> "INVALID"
                    null -> "UNKNOWN"
                }
                appendLine("  Expected: $expected")
                appendLine("  Computed: $computed")
                appendLine("  Status: $valid")
            }
        }

        return DecodeResult(rawHex = rawHex, pretty = pretty)
    }

    private fun ByteArray.toHex(): String =
        if (isEmpty()) "(empty)" else joinToString(" ") { "%02X".format(it) }

    private fun decodeHexStringOrNull(s: String): ByteArray? {
        val trimmed = s.trim()
        if (trimmed.isEmpty()) return null

        // Accept formats like: "4D 54 01 03 ..." or "4D540103..."
        val cleaned = trimmed
            .replace("0x", "", ignoreCase = true)
            .replace(Regex("[^0-9A-Fa-f]"), "")

        if (cleaned.length < 2 || cleaned.length % 2 != 0) return null

        return try {
            val out = ByteArray(cleaned.length / 2)
            var i = 0
            while (i < cleaned.length) {
                val byteStr = cleaned.substring(i, i + 2)
                out[i / 2] = byteStr.toInt(16).toByte()
                i += 2
            }
            out
        } catch (_: Exception) {
            null
        }
    }

    private fun hookCopyButtons(btnRaw: Button, btnDecoded: Button, raw: String, decoded: String) {
        btnRaw.setOnClickListener { copyToClipboard("MRTF Raw", raw) }
        btnDecoded.setOnClickListener { copyToClipboard("MRTF Decoded", decoded) }
    }

    private fun copyToClipboard(label: String, text: String) {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
        // no Toast here to keep debug screen quiet; add one if you want
    }
}