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

        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        // Refresh UI status when returning to app
        updateAdminStatus()
    }

    private fun setupClickListeners() {
        activateAdminButton.setOnClickListener {
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
            if (!dpm.isDeviceOwnerApp(packageName)) {
                Toast.makeText(this, "Admin not active. Please activate first.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            AlertDialog.Builder(this)
                .setTitle("Factory Reset")
                .setMessage("This will erase all data on the device. Continue?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Continue") { _, _ ->
                    performFactoryReset()
                }
                .show()
        }

        networkResetButton.setOnClickListener {
            if (!dpm.isDeviceOwnerApp(packageName)) {
                Toast.makeText(this, "Admin not active. Please activate first.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            AlertDialog.Builder(this)
                .setTitle("Network Reset")
                .setMessage("Reset network settings?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Continue") { _, _ ->
                    performNetworkReset()
                }
                .show()
        }

        dnsApplyButton.setOnClickListener {
            Toast.makeText(this, "DNS configuration is disabled.", Toast.LENGTH_SHORT).show()
        }

        removeAdminButton.setOnClickListener {
            if (!dpm.isDeviceOwnerApp(packageName)) {
                Toast.makeText(this, "Admin not active.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AlertDialog.Builder(this)
                .setTitle("Remove Admin")
                .setMessage("Remove Device Owner status? The device will no longer be managed.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Remove") { _, _ ->
                    try {
                        dpm.clearUserRestriction(admin, UserManager.DISALLOW_FACTORY_RESET)
                        @Suppress("DEPRECATION")
                        dpm.clearDeviceOwnerApp(packageName)

                        ownerStatus.text = "Administrator REMOVED"
                        statusText.text = "Device Owner removed."
                        Toast.makeText(this, "Admin removed successfully", Toast.LENGTH_LONG).show()
                        updateAdminStatus()
                    } catch (se: SecurityException) {
                        Log.e(TAG, "Could not remove Device Owner: ${se.message}")
                        Toast.makeText(this, "Failed: ${se.message}", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error: ${e.message}")
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
                .show()
        }
    }

    private fun updateAdminStatus() {
        try {
            val isOwner = dpm.isDeviceOwnerApp(packageName)
            if (isOwner) {
                ownerStatus.text = "✓ Administrator Active"
                
                // Enable admin features
                factoryResetButton.isEnabled = true
                networkResetButton.isEnabled = true
                activateAdminButton.isEnabled = false
                removeAdminButton.isEnabled = true
                
                // Disable DNS
                dnsApplyButton.isEnabled = false
                dnsHostEditText.isEnabled = false
                dnsStatus.text = "DNS: Disabled"

                loadFactoryResetState()
            } else {
                ownerStatus.text = "✗ Administrator Not Active"
                
                // Buttons enabled but will show message when clicked
                factoryResetButton.isEnabled = true
                networkResetButton.isEnabled = true
                activateAdminButton.isEnabled = true
                removeAdminButton.isEnabled = false
                
                // Disable DNS
                dnsApplyButton.isEnabled = false
                dnsHostEditText.isEnabled = false
                dnsStatus.text = "DNS: Disabled"

                statusText.text = "Tap 'Activate Admin' to enable features."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking admin status: ${e.message}")
            ownerStatus.text = "Error checking status"
        }
    }

    private fun performFactoryReset() {
        try {
            dpm.wipeData(0)
            Toast.makeText(this, "Factory reset initiated...", Toast.LENGTH_LONG).show()
            Log.i(TAG, "Factory reset called")
        } catch (se: SecurityException) {
            Log.e(TAG, "Factory reset failed: ${se.message}")
            Toast.makeText(this, "Failed: ${se.message}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}")
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun performNetworkReset() {
        try {
            startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
        } catch (e: Exception) {
            Log.e(TAG, "Could not open network settings: ${e.message}")
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
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
}
