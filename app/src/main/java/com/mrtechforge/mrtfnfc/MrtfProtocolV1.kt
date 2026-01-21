package com.mrtechforge.mrtfnfc

/*
MRTF Protocol v1 (binary)

Header (7 bytes):
  0..1  MAGIC: 0x4D 0x54 ("MT")
  2     VER:   0x01
  3     TYPE:  0x03 for COMMAND
  4     FLAGS: bitfield
  5..6  LEN:   payload length (big-endian)

Payload: TLV-lite:
  [FIELD_ID 1B][LEN 1B][DATA...]

For TYPE=COMMAND:
  0x10 Command ID (1 byte)
  0x11 Command Value (0..255 bytes)

Optional integrity:
  0xFE CRC16-CCITT-FALSE (2 bytes, big-endian)
  CRC computed over: [HEADER (7 bytes)] + [PAYLOAD TLVs EXCLUDING the CRC TLV]
*/
object MrtfProtocolV1 {
    private const val MAGIC0: Byte = 0x4D // 'M'
    private const val MAGIC1: Byte = 0x54 // 'T'

    const val VERSION_V1: Int = 0x01
    const val TYPE_COMMAND: Int = 0x03

    private const val FIELD_COMMAND_ID: Int = 0x10
    private const val FIELD_COMMAND_VALUE: Int = 0x11
    private const val FIELD_CRC16: Int = 0xFE

    data class ParsedTlv(
        val fieldId: Int,
        val length: Int,
        val value: ByteArray
    )

    data class ParsedFrame(
        val version: Int,
        val type: Int,
        val flags: Int,
        val payloadLength: Int,
        val commandId: Int?,
        val commandValue: ByteArray?,
        val tlvs: List<ParsedTlv>,
        val crcPresent: Boolean,
        val crcValid: Boolean?,      // null if no CRC present
        val crcExpected: Int?,       // from tag
        val crcComputed: Int?        // computed
    )

    fun parse(frame: ByteArray): ParsedFrame? {
        if (frame.size < 7) return null
        if (frame[0] != MAGIC0 || frame[1] != MAGIC1) return null

        val version = frame[2].toInt() and 0xFF
        if (version != VERSION_V1) return null

        val type = frame[3].toInt() and 0xFF
        val flags = frame[4].toInt() and 0xFF

        val length = ((frame[5].toInt() and 0xFF) shl 8) or (frame[6].toInt() and 0xFF)
        if (frame.size != 7 + length) return null

        val payloadStart = 7
        val payloadEnd = payloadStart + length

        val tlvs = parseTlvs(frame, payloadStart, payloadEnd) ?: return null

        var cmdId: Int? = null
        var cmdVal: ByteArray? = null
        var crcExpected: Int? = null

        for (t in tlvs) {
            when (t.fieldId) {
                FIELD_COMMAND_ID -> if (t.length == 1) cmdId = t.value[0].toInt() and 0xFF
                FIELD_COMMAND_VALUE -> cmdVal = t.value
                FIELD_CRC16 -> if (t.length == 2) {
                    crcExpected = ((t.value[0].toInt() and 0xFF) shl 8) or (t.value[1].toInt() and 0xFF)
                }
            }
        }

        val crcPresent = (crcExpected != null)
        val crcComputed = if (crcPresent) {
            val header = frame.copyOfRange(0, 7)
            val payloadWithoutCrc = buildPayloadExcludingField(tlvs, FIELD_CRC16)
            crc16CcittFalse(header + payloadWithoutCrc)
        } else null

        val crcValid = if (crcPresent) (crcExpected == crcComputed) else null

        return ParsedFrame(
            version = version,
            type = type,
            flags = flags,
            payloadLength = length,
            commandId = cmdId,
            commandValue = cmdVal,
            tlvs = tlvs,
            crcPresent = crcPresent,
            crcValid = crcValid,
            crcExpected = crcExpected,
            crcComputed = crcComputed
        )
    }

    fun buildCommandFrame(
        commandId: Int,
        commandValue: ByteArray? = null,
        flags: Int = 0,
        includeCrc: Boolean = true
    ): ByteArray {
        require(commandId in 0..255) { "commandId must fit UInt8" }
        if (commandValue != null) require(commandValue.size <= 255) { "commandValue max is 255 bytes" }

        val payload = ArrayList<Byte>()

        // TLV: Command ID
        payload.add(FIELD_COMMAND_ID.toByte())
        payload.add(0x01)
        payload.add((commandId and 0xFF).toByte())

        // TLV: Command Value (optional)
        if (commandValue != null) {
            payload.add(FIELD_COMMAND_VALUE.toByte())
            payload.add((commandValue.size and 0xFF).toByte())
            payload.addAll(commandValue.toList())
        }

        // CRC TLV placeholder
        if (includeCrc) {
            payload.add(FIELD_CRC16.toByte())
            payload.add(0x02)
            payload.add(0x00)
            payload.add(0x00)
        }

        val payloadBytes = payload.toByteArray()
        val len = payloadBytes.size

        val header = byteArrayOf(
            MAGIC0,
            MAGIC1,
            VERSION_V1.toByte(),
            TYPE_COMMAND.toByte(),
            (flags and 0xFF).toByte(),
            ((len shr 8) and 0xFF).toByte(),
            (len and 0xFF).toByte()
        )

        if (!includeCrc) return header + payloadBytes

        // Compute CRC over header + payload excluding CRC TLV
        val tlvs = parseTlvs(payloadBytes, 0, payloadBytes.size) ?: return header + payloadBytes
        val payloadWithoutCrc = buildPayloadExcludingField(tlvs, FIELD_CRC16)
        val crc = crc16CcittFalse(header + payloadWithoutCrc)

        // Patch CRC bytes in payload
        var i = 0
        while (i + 2 <= payloadBytes.size) {
            val fieldId = payloadBytes[i].toInt() and 0xFF
            val l = payloadBytes[i + 1].toInt() and 0xFF
            if (i + 2 + l > payloadBytes.size) break
            if (fieldId == FIELD_CRC16 && l == 2) {
                payloadBytes[i + 2] = ((crc shr 8) and 0xFF).toByte()
                payloadBytes[i + 3] = (crc and 0xFF).toByte()
                break
            }
            i += 2 + l
        }

        return header + payloadBytes
    }

    private fun parseTlvs(buf: ByteArray, start: Int, end: Int): List<ParsedTlv>? {
        var i = start
        val out = ArrayList<ParsedTlv>()
        while (i + 2 <= end) {
            val fieldId = buf[i].toInt() and 0xFF
            val len = buf[i + 1].toInt() and 0xFF
            i += 2
            if (i + len > end) return null
            val value = buf.copyOfRange(i, i + len)
            i += len
            out.add(ParsedTlv(fieldId, len, value))
        }
        if (i != end) return null
        return out
    }

    private fun buildPayloadExcludingField(tlvs: List<ParsedTlv>, excludedFieldId: Int): ByteArray {
        val out = ArrayList<Byte>()
        for (t in tlvs) {
            if (t.fieldId == excludedFieldId) continue
            out.add((t.fieldId and 0xFF).toByte())
            out.add((t.length and 0xFF).toByte())
            out.addAll(t.value.toList())
        }
        return out.toByteArray()
    }

    // CRC16-CCITT-FALSE: poly 0x1021, init 0xFFFF, xorout 0x0000, refin/refout false
    private fun crc16CcittFalse(data: ByteArray): Int {
        var crc = 0xFFFF
        for (b in data) {
            crc = crc xor ((b.toInt() and 0xFF) shl 8)
            for (i in 0 until 8) {
                crc = if ((crc and 0x8000) != 0) ((crc shl 1) xor 0x1021) else (crc shl 1)
                crc = crc and 0xFFFF
            }
        }
        return crc and 0xFFFF
    }
}