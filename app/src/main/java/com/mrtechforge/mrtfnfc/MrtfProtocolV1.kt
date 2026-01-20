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
*/
object MrtfProtocolV1 {
    private const val MAGIC0: Byte = 0x4D // 'M'
    private const val MAGIC1: Byte = 0x54 // 'T'

    const val VERSION_V1: Int = 0x01
    const val TYPE_COMMAND: Int = 0x03

    private const val FIELD_COMMAND_ID: Int = 0x10
    private const val FIELD_COMMAND_VALUE: Int = 0x11

    data class ParsedFrame(
        val version: Int,
        val type: Int,
        val flags: Int,
        val payloadLength: Int,
        val commandId: Int?,
        val commandValue: ByteArray?
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

        var i = payloadStart
        var cmdId: Int? = null
        var cmdVal: ByteArray? = null

        while (i + 2 <= payloadEnd) {
            val fieldId = frame[i].toInt() and 0xFF
            val len = frame[i + 1].toInt() and 0xFF
            i += 2

            if (i + len > payloadEnd) return null

            val data = frame.copyOfRange(i, i + len)
            i += len

            when (fieldId) {
                FIELD_COMMAND_ID -> if (len == 1) cmdId = data[0].toInt() and 0xFF
                FIELD_COMMAND_VALUE -> cmdVal = data
                else -> {
                    // ignore unknown fields
                }
            }
        }

        return ParsedFrame(
            version = version,
            type = type,
            flags = flags,
            payloadLength = length,
            commandId = cmdId,
            commandValue = cmdVal
        )
    }

    fun buildCommandFrame(commandId: Int, commandValue: ByteArray? = null, flags: Int = 0): ByteArray {
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

        return header + payloadBytes
    }
}
