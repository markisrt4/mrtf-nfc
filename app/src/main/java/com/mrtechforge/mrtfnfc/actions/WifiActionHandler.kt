package com.mrtechforge.mrtfnfc.actions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.provider.Settings

class WifiActionHandler(private val activity: Activity) {

    fun execute(action: ActionEnvelope) {
        // Android does not allow silent Wi-Fi toggling anymore
        openWifiPanel()
    }

    private fun openWifiPanel() {
        val intent = Intent(Settings.Panel.ACTION_WIFI)
        activity.startActivity(intent)
    }

    fun isWifiEnabled(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)
    }
}