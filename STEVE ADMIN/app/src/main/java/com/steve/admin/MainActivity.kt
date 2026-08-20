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

    private lateinit var factoryResetButton: Button
    private lateinit var networkResetButton: Button
    private lateinit var dnsApplyButton: Button
    private lateinit var activateAdminButton: Button
    private lateinit var dnsHostEditText: EditText
    private lateinit var removeAdminButton: Button
    
    private lateinit var factoryResetSwitch: Switch
    private lateinit var networkResetSwitch: Switch

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
        
        factoryResetSwitch = findViewById(R.id.factoryResetSwitch)
        networkResetSwitch = findViewById(R.id.networkResetSwitch)

        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        // Refresh UI status when returning to app
        updateAdminStatus()
    }

    private fun isAdminActive(): Boolean {
        return dpm.isAdminActive(admin)
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
            if (!isAdminActive()) {
                Toast.makeText(this, "Disabled by admin", Toast.LENGTH_SHORT).show()
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

        factoryResetSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!isAdminActive()) {
                factoryResetSwitch.isChecked = false
                Toast.makeText(this, "Disabled by admin", Toast.LENGTH_SHORT).show()
                return@setOnCheckedChangeListener
            }
            
            if (isChecked) {
                AlertDialog.Builder(this)
                    .setTitle("Block Factory Reset")
                    .setMessage("Prevent users from factory resetting this device?")
                    .setNegativeButton("Cancel") { _, _ ->
                        factoryResetSwitch.isChecked = false
                    }
                    .setPositiveButton("Block") { _, _ ->
                        blockFactoryReset()
                    }
                    .show()
            } else {
                AlertDialog.Builder(this)
                    .setTitle("Allow Factory Reset")
                    .setMessage("Allow users to factory reset this device?")
                    .setNegativeButton("Cancel") { _, _ ->
                        factoryResetSwitch.isChecked = true
                    }
                    .setPositiveButton("Allow") { _, _ ->
                        allowFactoryReset()
                    }
                    .show()
            }
        }

        networkResetButton.setOnClickListener {
            if (!isAdminActive()) {
                Toast.makeText(this, "Disabled by admin", Toast.LENGTH_SHORT).show()
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

        networkResetSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!isAdminActive()) {
                networkResetSwitch.isChecked = false
                Toast.makeText(this, "Disabled by admin", Toast.LENGTH_SHORT).show()
                return@setOnCheckedChangeListener
            }
            
            if (isChecked) {
                AlertDialog.Builder(this)
                    .setTitle("Block Network Reset")
                    .setMessage("Prevent users from resetting network settings?")
                    .setNegativeButton("Cancel") { _, _ ->
                        networkResetSwitch.isChecked = false
                    }
                    .setPositiveButton("Block") { _, _ ->
                        blockNetworkReset()
                    }
                    .show()
            } else {
                AlertDialog.Builder(this)
                    .setTitle("Allow Network Reset")
                    .setMessage("Allow users to reset network settings?")
                    .setNegativeButton("Cancel") { _, _ ->
                        networkResetSwitch.isChecked = true
                    }
                    .setPositiveButton("Allow") { _, _ ->
                        allowNetworkReset()
                    }
                    .show()
            }
        }

        dnsApplyButton.setOnClickListener {
            Toast.makeText(this, "DNS configuration is disabled.", Toast.LENGTH_SHORT).show()
        }

        removeAdminButton.setOnClickListener {
            if (!isAdminActive()) {
                Toast.makeText(this, "Admin not active.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AlertDialog.Builder(this)
                .setTitle("Remove Admin")
                .setMessage("Remove Device Admin status? The device will no longer be managed.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Remove") { _, _ ->
                    try {
                        dpm.removeActiveAdmin(admin)
                        ownerStatus.text = "Administrator REMOVED"
                        statusText.text = "Device Admin removed."
                        Toast.makeText(this, "Admin removed successfully", Toast.LENGTH_LONG).show()
                        updateAdminStatus()
                    } catch (se: SecurityException) {
                        Log.e(TAG, "Could not remove admin: ${se.message}")
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
            val isAdmin = isAdminActive()
            if (isAdmin) {
                ownerStatus.text = "✓ Administrator Active"
                
                // Enable admin features
                factoryResetButton.isEnabled = true
                networkResetButton.isEnabled = true
                factoryResetSwitch.isEnabled = true
                networkResetSwitch.isEnabled = true
                activateAdminButton.isEnabled = false
                removeAdminButton.isEnabled = true
                
                // Disable DNS
                dnsApplyButton.isEnabled = false
                dnsHostEditText.isEnabled = false
                dnsStatus.text = "DNS: Disabled"

                loadFactoryResetState()
                statusText.text = "Device Admin is active"
            } else {
                ownerStatus.text = "✗ Administrator Not Active"
                
                // Buttons enabled but will show message when clicked
                factoryResetButton.isEnabled = true
                networkResetButton.isEnabled = true
                factoryResetSwitch.isEnabled = false
                networkResetSwitch.isEnabled = false
                factoryResetSwitch.isChecked = false
                networkResetSwitch.isChecked = false
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

    private fun blockFactoryReset() {
        try {
            dpm.addUserRestriction(admin, UserManager.DISALLOW_FACTORY_RESET)
            Toast.makeText(this, "Factory reset blocked", Toast.LENGTH_SHORT).show()
            Log.i(TAG, "Factory reset blocked")
        } catch (se: SecurityException) {
            Log.e(TAG, "Failed to block factory reset: ${se.message}")
            Toast.makeText(this, "Failed: ${se.message}", Toast.LENGTH_SHORT).show()
            factoryResetSwitch.isChecked = false
        }
    }

    private fun allowFactoryReset() {
        try {
            dpm.clearUserRestriction(admin, UserManager.DISALLOW_FACTORY_RESET)
            Toast.makeText(this, "Factory reset allowed", Toast.LENGTH_SHORT).show()
            Log.i(TAG, "Factory reset allowed")
        } catch (se: SecurityException) {
            Log.e(TAG, "Failed to allow factory reset: ${se.message}")
            Toast.makeText(this, "Failed: ${se.message}", Toast.LENGTH_SHORT).show()
            factoryResetSwitch.isChecked = true
        }
    }

    private fun blockNetworkReset() {
        try {
            Toast.makeText(this, "Network reset blocked", Toast.LENGTH_SHORT).show()
            Log.i(TAG, "Network reset blocked")
        } catch (se: SecurityException) {
            Log.e(TAG, "Failed to block network reset: ${se.message}")
            Toast.makeText(this, "Failed: ${se.message}", Toast.LENGTH_SHORT).show()
            networkResetSwitch.isChecked = false
        }
    }

    private fun allowNetworkReset() {
        try {
            Toast.makeText(this, "Network reset allowed", Toast.LENGTH_SHORT).show()
            Log.i(TAG, "Network reset allowed")
        } catch (se: SecurityException) {
            Log.e(TAG, "Failed to allow network reset: ${se.message}")
            Toast.makeText(this, "Failed: ${se.message}", Toast.LENGTH_SHORT).show()
            networkResetSwitch.isChecked = true
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
            factoryResetSwitch.isChecked = blocked
            statusText.text = if (blocked) "Factory reset: BLOCKED" else "Factory reset: ALLOWED"
        } catch (e: Exception) {
            Log.e(TAG, "Error loading factory reset state: ${e.message}")
        }
    }
}
