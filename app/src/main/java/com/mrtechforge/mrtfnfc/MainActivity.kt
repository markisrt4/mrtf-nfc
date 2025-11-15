package com.mrtechforge.mrtfnfc

import android.content.Intent
import android.net.Uri
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var statusView: TextView
    private lateinit var testButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusView = findViewById(R.id.statusText)
        testButton = findViewById(R.id.testButton)

        testButton.setOnClickListener {
            statusView.text = "Launching Settings…"
            launchNfcSettings()
        }
    }

    override fun onResume() {
        super.onResume()

        if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_TECH_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_TAG_DISCOVERED == intent.action
        ) {
            val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            tag?.let {
                statusView.text = "Tag detected — writing..."
                writeDefaultNdefMessage(it)
            }
        }
    }

    // -----------------------------------------------------------
    // NFC Write Logic (Simple Test Payload)
    // -----------------------------------------------------------
    private fun writeDefaultNdefMessage(tag: Tag) {
        val ndefMessage = Ndef.createRecord(
            "text/plain",
            "Hello from MRTF NFC!".toByteArray(),
            ByteArray(0),
            ByteArray(0)
        )
        val message = NdefMessage(arrayOf(ndefMessageRecord = ndefMessage))

        try {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                if (!ndef.isWritable) {
                    statusView.text = "Tag is not writable."
                    return
                }
                ndef.writeNdefMessage(message)
                ndef.close()
                statusView.text = "Write successful!"
            } else {
                val format = NdefFormatable.get(tag)
                if (format != null) {
                    format.connect()
                    format.format(message)
                    format.close()
                    statusView.text = "Formatted & written successfully!"
                } else {
                    statusView.text = "NDEF not supported."
                }
            }
        } catch (e: Exception) {
            statusView.text = "Error writing tag: ${e.message}"
        }
    }

    // -----------------------------------------------------------
    // Settings Launchers — All PUBLIC/SUPPORTED Intents
    // -----------------------------------------------------------

    private fun launchWifiSettings() {
        startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
    }

    private fun launchBluetoothSettings() {
        startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
    }

    private fun launchNfcSettings() {
        startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
    }

    private fun launchDoNotDisturbSettings() {
        // SAFE PUBLIC STRING VERSION
        val intent = Intent("android.settings.ZEN_MODE_SETTINGS")
        startActivity(intent)
    }

    private fun launchAppNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        }
        startActivity(intent)
    }

    private fun launchCustomUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
