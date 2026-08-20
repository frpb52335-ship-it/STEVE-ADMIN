package com.steve.admin;

import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "STEVE-ADMIN";

    private DevicePolicyManager dpm;
    private ComponentName admin;

    private TextView deviceOwnerStatus;
    private TextView statusText;
    private Button activateAdminButton;
    private Button factoryResetButton;
    private EditText dnsHostEditText;
    private Button dnsApplyButton;
    private Button networkResetButton;
    private Button removeAdminButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize DevicePolicyManager safely
        initializeDevicePolicyManager();

        // Initialize UI elements
        initializeUIElements();

        // Setup click listeners
        setupClickListeners();

        // Check admin status but don't auto-execute
        updateAdminStatus();
    }

    /**
     * Initialize DevicePolicyManager safely
     */
    private void initializeDevicePolicyManager() {
        try {
            dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
            admin = new ComponentName(this, AdminReceiver.class);

            if (dpm == null) {
                Log.e(TAG, "DevicePolicyManager is not available");
                showError("Device Policy Manager unavailable");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing DevicePolicyManager: " + e.getMessage());
            showError("Failed to initialize admin service");
        }
    }

    /**
     * Initialize all UI elements
     */
    private void initializeUIElements() {
        deviceOwnerStatus = findViewById(R.id.deviceOwnerStatus);
        statusText = findViewById(R.id.statusText);
        activateAdminButton = findViewById(R.id.activateAdminButton);
        factoryResetButton = findViewById(R.id.factoryResetButton);
        dnsHostEditText = findViewById(R.id.dnsHostEditText);
        dnsApplyButton = findViewById(R.id.dnsApplyButton);
        networkResetButton = findViewById(R.id.networkResetButton);
        removeAdminButton = findViewById(R.id.removeAdminButton);
    }

    /**
     * Setup click listeners for all buttons - explicit, no auto-execution
     */
    private void setupClickListeners() {
        // Activate Admin Button
        activateAdminButton.setOnClickListener(v -> onActivateAdminClicked());

        // Factory Reset Button
        factoryResetButton.setOnClickListener(v -> onFactoryResetClicked());

        // DNS Apply Button
        dnsApplyButton.setOnClickListener(v -> onDnsApplyClicked());

        // Network Reset Button
        networkResetButton.setOnClickListener(v -> onNetworkResetClicked());

        // Remove Admin Button
        removeAdminButton.setOnClickListener(v -> onRemoveAdminClicked());
    }

    /**
     * Handle Activate Admin button click
     */
    private void onActivateAdminClicked() {
        try {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin);
            intent.putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Activate STEVE ADMIN to enable device management features."
            );
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch activation: " + e.getMessage());
            showError("Could not open activation screen");
        }
    }

    /**
     * Handle Factory Reset button click - requires explicit user confirmation
     */
    private void onFactoryResetClicked() {
        if (!isAdminActive()) {
            showWarning("Admin not active. Activate admin first.");
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Factory Reset")
                .setMessage("This will erase all data on the device. Are you sure?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Reset Device", (dialog, which) -> performFactoryReset())
                .show();
    }

    /**
     * Perform factory reset with proper error handling
     */
    private void performFactoryReset() {
        if (dpm == null || admin == null) {
            showError("Device Policy Manager not initialized");
            return;
        }

        try {
            if (!dpm.isAdminActive(admin)) {
                showError("Admin privileges revoked. Cannot perform reset.");
                return;
            }

            dpm.wipeData(0);
            Log.i(TAG, "Factory reset initiated");
            Toast.makeText(this, "Factory reset initiated...", Toast.LENGTH_LONG).show();
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException during factory reset: " + e.getMessage());
            showError("Permission denied: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error during factory reset: " + e.getMessage());
            showError("Factory reset error: " + e.getMessage());
        }
    }

    /**
     * Handle DNS Apply button click
     */
    private void onDnsApplyClicked() {
        String dnsHost = dnsHostEditText.getText().toString().trim();

        if (dnsHost.isEmpty()) {
            showWarning("Please enter a DNS host");
            return;
        }

        if (!isAdminActive()) {
            showWarning("Admin not active. Activate admin first.");
            return;
        }

        // Placeholder: DNS configuration logic
        Log.i(TAG, "DNS configuration requested for host: " + dnsHost);
        Toast.makeText(this, "DNS configured: " + dnsHost, Toast.LENGTH_SHORT).show();
    }

    /**
     * Handle Network Reset button click
     */
    private void onNetworkResetClicked() {
        if (!isAdminActive()) {
            showWarning("Admin not active. Activate admin first.");
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Network Reset")
                .setMessage("Reset network settings?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Reset", (dialog, which) -> performNetworkReset())
                .show();
    }

    /**
     * Perform network reset
     */
    private void performNetworkReset() {
        try {
            startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
            Log.i(TAG, "Network settings opened");
        } catch (Exception e) {
            Log.e(TAG, "Could not open network settings: " + e.getMessage());
            showError("Could not open network settings");
        }
    }

    /**
     * Handle Remove Admin button click
     */
    private void onRemoveAdminClicked() {
        if (!isAdminActive()) {
            showWarning("Admin is not active");
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Remove Admin")
                .setMessage("This will remove Device Admin and Owner privileges.\nYou will then be able to uninstall this app.")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Remove", (dialog, which) -> performRemoveAdmin())
                .show();
    }

    /**
     * Perform admin and owner removal with proper error handling
     * This removes both Device Admin and Device Owner status
     */
    private void performRemoveAdmin() {
        if (dpm == null || admin == null) {
            showError("Device Policy Manager not initialized");
            return;
        }

        try {
            Log.i(TAG, "Starting admin removal process...");

            // Step 1: Clear any active restrictions first
            try {
                // This prevents conflicts when removing admin
                dpm.clearDeviceOwnerApp(getPackageName());
                Log.i(TAG, "Device owner cleared");
            } catch (Exception e) {
                Log.w(TAG, "Could not clear device owner: " + e.getMessage());
                // Continue anyway - device owner might not be set
            }

            // Step 2: Remove Device Admin
            try {
                if (dpm.isAdminActive(admin)) {
                    dpm.removeActiveAdmin(admin);
                    Log.i(TAG, "Device admin removed successfully");
                }
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException removing device admin: " + e.getMessage());
                showError("Could not remove device admin: " + e.getMessage());
                return;
            }

            // Step 3: Update UI to reflect removal
            deviceOwnerStatus.setText("✗ Administrator REMOVED");
            statusText.setText("You can now uninstall this app");
            activateAdminButton.setEnabled(true);
            factoryResetButton.setEnabled(false);
            networkResetButton.setEnabled(false);
            dnsApplyButton.setEnabled(false);
            removeAdminButton.setEnabled(false);

            Log.i(TAG, "Admin removal completed successfully");
            showSuccess("Admin and Owner privileges removed successfully!\nYou can now uninstall the app.");

        } catch (Exception e) {
            Log.e(TAG, "Unexpected error removing admin: " + e.getMessage());
            showError("Error during removal: " + e.getMessage());
        }
    }

    /**
     * Check if admin is currently active
     */
    private boolean isAdminActive() {
        if (dpm == null || admin == null) {
            return false;
        }
        try {
            return dpm.isAdminActive(admin);
        } catch (Exception e) {
            Log.e(TAG, "Error checking admin status: " + e.getMessage());
            return false;
        }
    }

    /**
     * Update admin status display - informational only, no auto-execution
     */
    private void updateAdminStatus() {
        try {
            boolean isAdmin = isAdminActive();
            if (isAdmin) {
                deviceOwnerStatus.setText("✓ Administrator Active");
                statusText.setText("All features are available");
                activateAdminButton.setEnabled(false);
                factoryResetButton.setEnabled(true);
                networkResetButton.setEnabled(true);
                dnsApplyButton.setEnabled(true);
                removeAdminButton.setEnabled(true);
            } else {
                deviceOwnerStatus.setText("✗ Administrator Not Active");
                statusText.setText("Click 'Activate Admin' to enable features");
                activateAdminButton.setEnabled(true);
                factoryResetButton.setEnabled(false);
                networkResetButton.setEnabled(false);
                dnsApplyButton.setEnabled(false);
                removeAdminButton.setEnabled(false);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating admin status: " + e.getMessage());
            deviceOwnerStatus.setText("Error checking status");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh status when returning to app - still no auto-execution
        updateAdminStatus();
    }

    /**
     * Show error toast
     */
    private void showError(String message) {
        Toast.makeText(this, "❌ " + message, Toast.LENGTH_LONG).show();
    }

    /**
     * Show warning toast
     */
    private void showWarning(String message) {
        Toast.makeText(this, "⚠️ " + message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Show success toast
     */
    private void showSuccess(String message) {
        Toast.makeText(this, "✓ " + message, Toast.LENGTH_LONG).show();
    }
}
