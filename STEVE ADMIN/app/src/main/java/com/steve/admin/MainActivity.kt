package com.steve.admin

import android.app.AlertDialog
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.UserManager
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var dpm: DevicePolicyManager
    private lateinit var admin: ComponentName

    private lateinit var ownerStatus: TextView
    private lateinit var statusText: TextView
    private lateinit var dnsStatus: TextView

    private lateinit var factoryResetButton: Button
    private lateinit var networkResetButton: Button
    private lateinit var dnsApplyButton: Button
    private lateinit var activateAdminButton: Button
    private lateinit var dnsHostEditText: EditText
    private lateinit var removeAdminButton: Button

    companion object {
        private const val TAG = "STEVE-ADMIN"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager

        admin = ComponentName(
            this,
            AdminReceiver::class.java
        )

        ownerStatus = findViewById(R.id.deviceOwnerStatus)
        statusText = findViewById(R.id.statusText)
        dnsStatus = findViewById(R.id.dnsStatus)

        factoryResetButton = findViewById(R.id.factoryResetButton)
        networkResetButton = findViewById(R.id.networkResetButton)
        dnsApplyButton = findViewById(R.id.dnsApplyButton)
        activateAdminButton = findViewById(R.id.activateAdminButton)
        dnsHostEditText = findViewById(R.id.dnsHostEditText)
        removeAdminButton = findViewById(R.id.removeAdminButton)

        updateAdminStatus()

        activateAdminButton.setOnClickListener {
            // Open Android's device admin activation screen for our receiver
            try {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
                    putExtra(
                        DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        "Activate STEVE ADMIN to enable device management features."
                    )
                }
                startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch activation: ${e.message}")
                Toast.makeText(
                    this,
                    "Could not open activation screen: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        factoryResetButton.setOnClickListener {
            // Confirmation dialog first
            AlertDialog.Builder(this)
                .setTitle("Factory reset")
                .setMessage("This will erase the device. Continue?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Continue") { _, _ ->
                    performFactoryReset()
                }
                .show()
        }

        networkResetButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Network reset")
                .setMessage("This will attempt to reset network settings. Continue?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Continue") { _, _ ->
                    performNetworkReset()
                }
                .show()
        }

        dnsApplyButton.setOnClickListener {
            val host = dnsHostEditText.text.toString().trim()
            if (host.isEmpty()) {
                Toast.makeText(this, "Please enter a DNS hostname.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isValidHostname(host)) {
                Toast.makeText(this, "Invalid hostname.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            applyDnsHostname(host)
        }

        removeAdminButton.setOnClickListener {
            if (!dpm.isDeviceOwnerApp(packageName)) {
                Toast.makeText(this, "STEVE ADMIN is not Device Owner.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                // Remove the factory-reset restriction first.
                dpm.clearUserRestriction(admin, UserManager.DISALLOW_FACTORY_RESET)

                @Suppress("DEPRECATION")
                dpm.clearDeviceOwnerApp(packageName)

                ownerStatus.text = "Administrator REMOVED"

                statusText.text = "Device Owner removed. You can uninstall STEVE ADMIN."

                Toast.makeText(this, "Device Owner removed", Toast.LENGTH_LONG).show()
            } catch (se: SecurityException) {
                Log.e(TAG, "Could not remove Device Owner: ${se.message}")
                Toast.makeText(this, "Could not remove Device Owner: ${se.message}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error clearing owner: ${e.message}")
                Toast.makeText(this, "Unexpected error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateAdminStatus() {
        try {
            val isOwner = dpm.isDeviceOwnerApp(packageName)
            if (isOwner) {
                ownerStatus.text = "Administrator Active"
                // enable controls
                factoryResetButton.isEnabled = true
                networkResetButton.isEnabled = true
                dnsApplyButton.isEnabled = true
                activateAdminButton.isEnabled = false
                removeAdminButton.isEnabled = true

                loadFactoryResetState()
                updateStatusText()
            } else {
                ownerStatus.text = "Administrator Not Active"
                factoryResetButton.isEnabled = false
                networkResetButton.isEnabled = false
                dnsApplyButton.isEnabled = false
                activateAdminButton.isEnabled = true
                removeAdminButton.isEnabled = false

                statusText.text = "Set STEVE ADMIN as Device Owner first."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking admin status: ${e.message}")
            ownerStatus.text = "Administrator Not Active"
            Toast.makeText(this, "Error checking admin status: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun performFactoryReset() {
        if (!dpm.isDeviceOwnerApp(packageName)) {
            Toast.makeText(this, "Factory reset is not permitted with the current administrator privileges.", Toast.LENGTH_LONG).show()
            return
        }

        try {
            // Attempt factory reset using official API. This requires device owner privileges.
            dpm.wipeData(0)
            // If the call returns, we show a message; in most cases the device will reboot before next lines run.
            Toast.makeText(this, "Factory reset request completed.", Toast.LENGTH_LONG).show()
            Log.i(TAG, "wipeData() called")
        } catch (se: SecurityException) {
            Log.e(TAG, "Factory reset not permitted: ${se.message}")
            Toast.makeText(this, "Factory reset failed: ${se.message}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "Factory reset error: ${e.message}")
            Toast.makeText(this, "Factory reset failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun performNetworkReset() {
        // There is no official public DevicePolicyManager API to perform a full "network reset"
        // from a normal device-owner app on standard Android builds. Perform best-effort checks
        // and report exact reason.
        if (!dpm.isDeviceOwnerApp(packageName)) {
            Toast.makeText(this, "Network reset failed: Not device owner.", Toast.LENGTH_LONG).show()
            return
        }

        // Many Android versions do not expose a programmatic way to reset network settings
        // for third-party apps. Explain this clearly.
        val reason = "Android does not provide a public API for apps to perform a full network reset. " +
                "This operation typically requires system-level privileges or a manual user action."

        Log.e(TAG, "Network reset not permitted: $reason")
        AlertDialog.Builder(this)
            .setTitle("Network reset not permitted")
            .setMessage("Network reset failed: $reason")
            .setPositiveButton("Open network settings") { _, _ ->
                try {
                    startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                } catch (e: Exception) {
                    Log.e(TAG, "Could not open network settings: ${e.message}")
                    Toast.makeText(this, "Could not open network settings: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("OK", null)
            .show()
    }

    private fun applyDnsHostname(host: String) {
        // Apps cannot change global/private DNS settings without privileged permissions (WRITE_SECURE_SETTINGS)
        // so explain that clearly.
        if (!dpm.isDeviceOwnerApp(packageName)) {
            Toast.makeText(this, "DNS change failed: Not device owner.", Toast.LENGTH_LONG).show()
            return
        }

        val reason = "Changing the system Private DNS/Global DNS requires system-level permissions (WRITE_SECURE_SETTINGS) " +
                "or use of a Device Policy controller with appropriate OEM support. This app cannot change system DNS on standard Android builds."

        Log.e(TAG, "DNS change not permitted: $reason")
        AlertDialog.Builder(this)
            .setTitle("DNS change not permitted")
            .setMessage("DNS change failed: $reason")
            .setPositiveButton("Open Private DNS settings") { _, _ ->
                try {
                    startActivity(Intent("android.settings.PRIVATE_DNS_SETTINGS"))
                } catch (e: Exception) {
                    Log.e(TAG, "Could not open Private DNS settings: ${e.message}")
                    Toast.makeText(this, "Could not open Private DNS settings: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("OK", null)
            .show()
    }

    private fun isValidHostname(host: String): Boolean {
        // Basic hostname validation (RFC 1035-ish). Allow letters, digits, hyphens and dots.
        val regex = "^[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z]{2,})+$".toRegex()
        return regex.matches(host)
    }

    private fun loadFactoryResetState() {
        try {
            val restrictions = dpm.getUserRestrictions(admin)
            val blocked = restrictions.getBoolean(UserManager.DISALLOW_FACTORY_RESET, false)
            statusText.text = if (blocked) "Factory reset: BLOCKED" else "Factory reset: ALLOWED"
        } catch (e: Exception) {
            Log.e(TAG, "Error loading factory reset state: ${e.message}")
        }
    }

    private fun updateStatusText() {
        // Update a more detailed status area
        try {
            val factory = if (dpm.getUserRestrictions(admin).getBoolean(UserManager.DISALLOW_FACTORY_RESET, false)) "BLOCKED" else "ALLOWED"
            val network = "UNKNOWN"
            val dns = "UNKNOWN"

            statusText.text = "Factory reset: $factory\nNetwork reset: $network\nDNS: $dns"
        } catch (e: Exception) {
            Log.e(TAG, "Error updating status text: ${e.message}")
        }
    }
}
