package com.steve.admin

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.UserManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class MainActivity : AppCompatActivity() {

    private lateinit var dpm: DevicePolicyManager
    private lateinit var adminComponent: ComponentName
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, AdminReceiver::class.java)
        prefs = getSharedPreferences("admin_prefs", Context.MODE_PRIVATE)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }

        val statusText = TextView(this).apply {
            textSize = 20f
            setPadding(0, 0, 0, 32)
        }
        layout.addView(statusText)

        val isOwner = dpm.isDeviceOwnerApp(packageName)

        if (isOwner) {
            statusText.text = "STEVE ADMIN\nStatus: ACTIVE DEVICE OWNER"

            createOfflineSwitch(layout, "Block Private DNS", UserManager.DISALLOW_CONFIG_PRIVATE_DNS, "dns_key")
            createOfflineSwitch(layout, "Block Factory Reset", UserManager.DISALLOW_FACTORY_RESET, "reset_key")
            createOfflineSwitch(layout, "Block Network Reset", UserManager.DISALLOW_NETWORK_RESET, "net_reset_key")
            createOfflineSwitch(layout, "Block Wi-Fi Settings Config", UserManager.DISALLOW_CONFIG_WIFI, "wifi_key")
            createOfflineSwitch(layout, "Block Mobile Network Config", UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS, "mobile_key")
        } else {
            statusText.text = "STEVE ADMIN\nStatus: NOT DEVICE OWNER\nPlease provision via ADB."
        }

        val scrollView = ScrollView(this)
        scrollView.addView(layout)
        setContentView(scrollView)
    }

    private fun createOfflineSwitch(
        parent: LinearLayout, 
        title: String, 
        restriction: String, 
        prefKey: String
    ) {
        val switch = SwitchCompat(this).apply {
            text = title
            textSize = 16f
            setPadding(0, 24, 0, 24)
            isChecked = prefs.getBoolean(prefKey, false)
        }

        toggleRestriction(restriction, switch.isChecked)

        switch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(prefKey, isChecked).apply()
            toggleRestriction(restriction, isChecked)
        }

        parent.addView(switch)
    }

    private fun toggleRestriction(restriction: String, enable: Boolean) {
        if (enable) {
            dpm.addUserRestriction(adminComponent, restriction)
        } else {
            dpm.clearUserRestriction(adminComponent, restriction)
        }
    }
}