# MRTF Protocol Specification (Version 1)

**Status:** Draft

**Protocol Name:** MRTF (MrTechForge Protocol)

**Version:** 1.0

**Intended Use:** NFC-first, short-range, deterministic application-layer protocol

---

## 1. Introduction

MRTF is a lightweight, binary, application-layer protocol designed for deterministic data exchange over short-range transports, primarily NFC. It is optimized for environments where payload size, parsing simplicity, and predictability are more important than human readability or schema self-description.

MRTF is suitable for:

* Android NFC applications
* Embedded systems
* Linux-based services
* BLE or other constrained transports (future use)

MRTF is **not** a transport protocol and does not handle discovery, sessions, encryption, or authentication by itself.

---

## 2. Design Goals

* Deterministic parsing
* Minimal overhead
* Binary-first encoding
* Forward compatibility via versioning
* Simple implementation in C, Kotlin, Python, and similar languages

---

## 3. Terminology

* **Frame**: A complete MRTF message
* **Payload**: The application-specific data carried inside a frame
* **Field**: A single payload element encoded using MRTF TLV-lite rules
* **Receiver**: Any system parsing an MRTF frame
* **Sender**: Any system generating an MRTF frame

---

## 4. Byte Order and Encoding Rules

| Item       | Rule                   |
| ---------- | ---------------------- |
| Byte order | Big-endian             |
| Strings    | UTF-8 encoded          |
| Integers   | Unsigned unless stated |
| Alignment  | Packed, no padding     |

---

## 5. Frame Structure

Every MRTF frame MUST follow the structure below:

```
+--------+--------+--------+--------+---------+-----------+
| MAGIC  | VER    | TYPE   | FLAGS  | LENGTH  | PAYLOAD   |
| 2 B    | 1 B    | 1 B    | 1 B    | 2 B     | N bytes  |
+--------+--------+--------+--------+---------+-----------+
```

Minimum frame size: **7 bytes**

---

## 6. Header Fields

### 6.1 MAGIC (2 bytes)

**Value:** `0x4D 0x54` (ASCII "MT")

Purpose:

* Identifies the frame as MRTF
* Prevents misinterpretation of arbitrary data

**Receiver behavior:**

* Frames with invalid MAGIC MUST be rejected

---

### 6.2 VERSION (1 byte)

Defines the major protocol version.

| Value  | Meaning        |
| ------ | -------------- |
| `0x01` | MRTF version 1 |

Rules:

* Only major versions are supported
* Incompatible changes require a version increment
* Unsupported versions MUST be rejected

---

### 6.3 TYPE (1 byte)

Indicates the semantic meaning of the payload.

| Value  | Name      | Description                 |
| ------ | --------- | --------------------------- |
| `0x01` | HANDSHAKE | Capability discovery        |
| `0x02` | IDENTITY  | Device identity information |
| `0x03` | COMMAND   | Action request              |
| `0x04` | RESPONSE  | Response to a command       |
| `0x05` | EVENT     | Asynchronous event          |
| `0x7F` | VENDOR    | Experimental or private use |

Receivers MUST interpret the payload based on TYPE.

---

### 6.4 FLAGS (1 byte)

FLAGS is a bitfield.

| Bit | Name         | Meaning                   |
| --- | ------------ | ------------------------- |
| 0   | ACK_REQUIRED | Sender expects a response |
| 1   | ENCRYPTED    | Payload is encrypted      |
| 2   | COMPRESSED   | Payload is compressed     |
| 3   | ERROR        | Frame represents an error |
| 4–7 | RESERVED     | Must be zero              |

Rules:

* Unknown flags MUST be ignored
* RESERVED bits MUST be zero when sending

---

### 6.5 LENGTH (2 bytes)

Unsigned integer indicating payload length in bytes.

Rules:

* MUST exactly match payload size
* Mismatch MUST cause frame rejection

---

## 7. Payload Encoding

### 7.1 Overview

The MRTF payload is a sequence of fields encoded using a simplified TLV (Type-Length-Value) format referred to as **TLV-lite**.

Payloads are parsed sequentially until the declared LENGTH is consumed.

---

### 7.2 Field Encoding Format

Each field is encoded as:

```
+----------+--------+----------------+
| FIELD_ID | LEN    | DATA           |
| 1 byte   | 1 byte | LEN bytes      |
+----------+--------+----------------+
```

Rules:

* FIELD_ID defines the meaning of DATA
* LEN defines the number of DATA bytes
* DATA is uninterpreted binary unless otherwise specified

Receivers MUST skip unknown FIELD_IDs using LEN.

---

## 8. Standard Payload Fields (v1)

### 8.1 Common Fields

| FIELD_ID | Name             | Data Type    | Description               |
| -------- | ---------------- | ------------ | ------------------------- |
| `0x01`   | Device Name      | UTF-8 string | Human-readable identifier |
| `0x02`   | Capabilities     | Bitmask      | Supported features        |
| `0x03`   | Device ID        | Binary       | Unique identifier         |
| `0x04`   | Firmware Version | UTF-8 string | Firmware revision         |

---

### 8.2 Capability Bitmask (FIELD_ID 0x02)

Each bit represents a capability:

| Bit | Meaning        |
| --- | -------------- |
| 0   | NFC            |
| 1   | BLE            |
| 2   | Wi-Fi          |
| 3   | Secure Element |
| 4–7 | Reserved       |

Multiple capabilities may be combined.

---

### 8.3 Command Fields

| FIELD_ID | Name          | Data Type | Description        |
| -------- | ------------- | --------- | ------------------ |
| `0x10`   | Command ID    | UInt8     | Command identifier |
| `0x11`   | Command Value | Binary    | Command argument   |

---

### 8.4 Response Fields

| FIELD_ID | Name          | Data Type | Description               |
| -------- | ------------- | --------- | ------------------------- |
| `0x20`   | Status Code   | UInt8     | Success or error code     |
| `0x21`   | Response Data | Binary    | Optional response payload |

---

## 9. Example Frames

### 9.1 Identity Frame (Hex)

```
4D 54 01 02 00 00 0C
01 06 4D 52 54 46 2D 31
02 02 01 03
```

Meaning:

* MRTF v1
* TYPE = IDENTITY
* Device name: "MRTF-1"
* Capabilities: NFC + BLE

---

### 9.2 Command Frame Example

```
4D 54 01 03 01 00 03
10 01 01
```

Meaning:

* TYPE = COMMAND
* ACK_REQUIRED flag set
* Command ID = 0x01
* Command value = 0x01

---

## 10. Error Handling

Receivers MUST silently discard frames if:

* MAGIC is invalid
* VERSION is unsupported
* LENGTH does not match payload size
* Payload parsing exceeds buffer bounds

Error responses are optional and transport-dependent.

---

## 11. Security Considerations

MRTF does not provide:

* Encryption
* Authentication
* Integrity protection

Security must be implemented via:

* Transport properties (e.g., NFC proximity)
* Backend validation
* Optional encrypted payloads indicated via FLAGS

---

## 12. Extensibility

* New TYPE values may be added
* New FIELD_ID values may be added
* Unknown values MUST be safely ignored

Backward compatibility is maintained by versioning and TLV-lite parsing.

---

## 13. Design Philosophy

MRTF favors:

* Explicit structure over reflection
* Binary determinism over verbosity
* Predictable parsing over flexibility

This makes MRTF suitable for constrained and embedded environments.

---

## Appendix A: NFC Fallback Behavior (Non-Companion Devices)

### A.1 Rationale

When an Android device scans an NFC tag containing MRTF data without the companion application installed, the operating system has no registered handler for the MRTF MIME type. To provide a clear and safe user experience, MRTF tags SHOULD include a human-readable fallback record.

This appendix defines the recommended fallback mechanism and its behavior.

---

### A.2 Recommended NDEF Record Order

MRTF tags SHOULD include the following NDEF records in order:

1. **NDEF Text Record** (fallback)
2. **NDEF MIME Media Record** (`application/mrtf`)

This ordering ensures that non-companion devices can display helpful information, while companion apps can still consume the MRTF payload.

---

### A.3 NDEF Text Record (Fallback)

**Purpose:**

* Inform users that the tag is associated with MRTF
* Avoid automatic actions (e.g., browser launches)
* Maintain offline usability

**Example Text Payload:**

```
This is an MRTF-enabled device.
Install the MRTF app to interact with it.
```

**Behavior:**

* Displayed by generic NFC readers
* Ignored by MRTF companion applications
* No operating-system action is triggered

---

### A.4 MRTF MIME Media Record

**TNF:** MIME Media
**MIME Type:** `application/mrtf`
**Payload:** MRTF frame as defined in Sections 5–9

Only applications explicitly declaring support for `application/mrtf` will receive and process this record.

---

### A.5 Android Intent Handling Summary

| Device State                | Result                                    |
| --------------------------- | ----------------------------------------- |
| Companion app not installed | Text record displayed or silently ignored |
| Companion app installed     | App launched, MRTF payload parsed         |
| Multiple handlers installed | App chooser shown                         |

---

## Appendix B: Example NDEF Creation (Android)

The following example demonstrates writing both a text fallback record and an MRTF binary record to an NFC tag.

```kotlin
val textRecord = NdefRecord.createTextRecord(
    "en",
    "This is an MRTF-enabled device. Install the MRTF app to interact with it."
)

val mrtfRecord = NdefRecord.createMime(
    "application/mrtf",
    mrtfPayloadBytes
)

val message = NdefMessage(arrayOf(textRecord, mrtfRecord))
```

---

## Appendix C: MRTF Companion App Behavior

Companion applications SHOULD:

* Ignore NDEF records other than `application/mrtf`
* Validate MRTF headers before parsing payloads
* Fail silently on invalid frames

This ensures predictable behavior regardless of tag content or record ordering.

---

**End of MRTF Protocol Specification v1**
