package com.steve.admin

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.os.Bundle
import android.os.UserManager
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var dpm: DevicePolicyManager
    private lateinit var admin: ComponentName

    private lateinit var ownerStatus: TextView
    private lateinit var statusText: TextView
    private lateinit var dnsStatus: TextView

    private lateinit var factoryResetSwitch: Switch
    private lateinit var networkResetSwitch: Switch
    private lateinit var dnsSwitch: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        dpm = getSystemService(DEVICE_POLICY_SERVICE)
                as DevicePolicyManager

        admin = ComponentName(
            this,
            AdminReceiver::class.java
        )

        ownerStatus = findViewById(R.id.deviceOwnerStatus)
        statusText = findViewById(R.id.statusText)
        dnsStatus = findViewById(R.id.dnsStatus)

        factoryResetSwitch =
            findViewById(R.id.factoryResetSwitch)

        networkResetSwitch =
            findViewById(R.id.networkResetSwitch)

        dnsSwitch =
            findViewById(R.id.dnsSwitch)

        val removeAdminButton: Button =
            findViewById(R.id.removeAdminButton)

        if (!dpm.isDeviceOwnerApp(packageName)) {

            ownerStatus.text =
                "Device Owner: NOT ACTIVE"

            disableControls()

            statusText.text =
                "Set STEVE ADMIN as Device Owner first."

            return
        }

        ownerStatus.text =
            "Device Owner: ACTIVE"

        loadFactoryResetState()

        factoryResetSwitch.setOnCheckedChangeListener {
                _, enabled ->

            if (enabled) {
                blockFactoryReset()
            } else {
                allowFactoryReset()
            }

            updateStatus()
        }

        networkResetSwitch.setOnCheckedChangeListener {
                _, enabled ->

            Toast.makeText(
                this,
                if (enabled)
                    "Network reset restriction selected"
                else
                    "Network reset restriction disabled",
                Toast.LENGTH_SHORT
            ).show()

            updateStatus()
        }

        dnsSwitch.setOnCheckedChangeListener {
                _, enabled ->

            dnsStatus.text =
                if (enabled)
                    "DNS: Configuration enabled"
                else
                    "DNS: Not configured"

            updateStatus()
        }

        removeAdminButton.setOnClickListener {

            if (!dpm.isDeviceOwnerApp(packageName)) {

                Toast.makeText(
                    this,
                    "STEVE ADMIN is not Device Owner.",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            try {

                // Remove the factory-reset restriction first.
                dpm.clearUserRestriction(
                    admin,
                    UserManager.DISALLOW_FACTORY_RESET
                )

                // Testing/development only.
                @Suppress("DEPRECATION")
                dpm.clearDeviceOwnerApp(packageName)

                ownerStatus.text =
                    "Device Owner: REMOVED"

                factoryResetSwitch.isEnabled = false
                networkResetSwitch.isEnabled = false
                dnsSwitch.isEnabled = false
                removeAdminButton.isEnabled = false

                statusText.text =
                    "Device Owner removed. You can uninstall STEVE ADMIN."
Toast.makeText(
                    this,
                    "Device Owner removed",
                    Toast.LENGTH_LONG
                ).show()

            } catch (e: SecurityException) {

                Toast.makeText(
                    this,
                    "Could not remove Device Owner: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun disableControls() {
        factoryResetSwitch.isEnabled = false
        networkResetSwitch.isEnabled = false
        dnsSwitch.isEnabled = false
    }

    private fun blockFactoryReset() {
        dpm.addUserRestriction(
            admin,
            UserManager.DISALLOW_FACTORY_RESET
        )

        Toast.makeText(
            this,
            "Factory reset restriction ON",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun allowFactoryReset() {
        dpm.clearUserRestriction(
            admin,
            UserManager.DISALLOW_FACTORY_RESET
        )

        Toast.makeText(
            this,
            "Factory reset restriction OFF",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun loadFactoryResetState() {

        val restrictions =
            dpm.getUserRestrictions(admin)

        factoryResetSwitch.isChecked =
            restrictions.getBoolean(
                UserManager.DISALLOW_FACTORY_RESET,
                false
            )
    }

    private fun updateStatus() {

        val factory =
            if (factoryResetSwitch.isChecked)
                "BLOCKED"
            else
                "ALLOWED"

        val network =
            if (networkResetSwitch.isChecked)
                "RESTRICTED"
            else
                "NORMAL"

        val dns =
            if (dnsSwitch.isChecked)
                "CONFIGURED"
            else
                "NORMAL"

        statusText.text =
            "Factory reset: $factory\n" +
            "Network reset: $network\n" +
            "DNS: $dns"
    }
}
