package com.mrtechforge.mrtfnfc

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        status = findViewById(R.id.statusText)

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIntent(it) }
    }

    private fun handleIntent(intent: Intent) {
        val data = intent.data ?: run {
            status.text = "MRTF NFC Actions\nReady for tags."
            return
        }

        val action = data.host ?: ""
        val pkg = data.getQueryParameter("pkg")
        val dest = data.getQueryParameter("dest")

        when (action.lowercase()) {
            "wifi"       -> openWifi()
            "vpn"        -> openVpn()
            "bt"         -> openBluetooth()
            "auto"       -> openAndroidAuto()
            "appinfo",
            "clearbrowser" -> pkg?.let { openAppInfo(it) }
            "dnd"        -> openDnd()
            "night"      -> openNightSettings()
            "drive"      -> startDriveMode()
            "maps"       -> openMaps(dest)
            "settings"   -> openMainSettings()
            "battery"    -> openBattery()
            else         -> status.text = "Unknown MRTF action: $action"
        }
    }

    private fun openWifi() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startActivity(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY))
            } else {
                startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            }
            status.text = "Opened Wi-Fi / Internet panel."
        } catch (e: Exception) {
            status.text = "Error opening Wi-Fi."
        }
    }

    private fun openVpn() {
        try {
            startActivity(Intent("android.settings.VPN_SETTINGS"))
            status.text = "Opened VPN settings."
        } catch (e: Exception) {
            status.text = "VPN settings not supported."
        }
    }

    private fun openBluetooth() {
        try {
            startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            status.text = "Opened Bluetooth settings."
        } catch (e: Exception) {
            status.text = "Error opening Bluetooth."
        }
    }

    private fun openAndroidAuto() {
        val pkg = "com.google.android.projection.gearhead"
        try {
            val launch = packageManager.getLaunchIntentForPackage(pkg)
            if (launch != null) {
                startActivity(launch)
                status.text = "Launched Android Auto."
            } else {
                status.text = "Android Auto not installed."
            }
        } catch (e: Exception) {
            status.text = "Error launching Android Auto."
        }
    }

    private fun openAppInfo(packageName: String) {
        try {
            val uri = Uri.parse("package:$packageName")
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri)
            startActivity(intent)
            status.text = "Opened App Info for $packageName."
        } catch (e: Exception) {
            status.text = "Error opening App Info."
        }
    }

    private fun openDnd() {
        try {
            startActivity(Intent(Settings.ACTION_ZEN_MODE_SETTINGS))
            status.text = "Opened Do Not Disturb settings."
        } catch (e: Exception) {
            status.text = "Error opening DND."
        }
    }

    private fun openNightSettings() {
        try {
            startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS))
            status.text = "Opened Display / Night settings."
        } catch (e: Exception) {
            status.text = "Error opening display settings."
        }
    }

    private fun startDriveMode() {
        openAndroidAuto()
        openDnd()
        status.text = "Drive mode: Android Auto + DND opened."
    }

    private fun openMaps(dest: String?) {
        val query = dest ?: "Home"
        val uri = Uri.parse("google.navigation:q=$query")
        val intent = Intent(Intent.ACTION_VIEW, uri)
            .setPackage("com.google.android.apps.maps")
        startActivity(intent)
        status.text = "Launching Maps → $query"
    }

    private fun openMainSettings() {
        startActivity(Intent(Settings.ACTION_SETTINGS))
        status.text = "Opened main Settings."
    }

    private fun openBattery() {
        startActivity(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS))
        status.text = "Opened Battery settings."
    }
}
