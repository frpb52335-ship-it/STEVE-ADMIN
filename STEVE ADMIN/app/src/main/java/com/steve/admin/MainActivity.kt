package com.steve.admin

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.os.Bundle
import android.os.UserManager
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var dpm: DevicePolicyManager
    private lateinit var admin: ComponentName

    private lateinit var ownerStatus: TextView
    private lateinit var statusText: TextView

    private lateinit var factoryResetSwitch: Switch
    private lateinit var cameraSwitch: Switch
    private lateinit var masterSwitch: Switch

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

        factoryResetSwitch =
            findViewById(R.id.factoryResetSwitch)

        cameraSwitch =
            findViewById(R.id.cameraSwitch)

        masterSwitch =
            findViewById(R.id.restrictionsSwitch)

        if (!dpm.isDeviceOwnerApp(packageName)) {
            ownerStatus.text = "Device Owner: NOT ACTIVE"

            factoryResetSwitch.isEnabled = false
            cameraSwitch.isEnabled = false
            masterSwitch.isEnabled = false

            statusText.text =
                "Provision this app as Device Owner first."

            return
        }

        ownerStatus.text = "Device Owner: ACTIVE"

        loadCurrentState()

        factoryResetSwitch.setOnCheckedChangeListener {
                _, enabled ->

            if (enabled) {
                blockFactoryReset()
            } else {
                allowFactoryReset()
            }

            updateStatus()
        }

        cameraSwitch.setOnCheckedChangeListener {
                _, enabled ->

            dpm.setCameraDisabled(
                admin,
                enabled
            )

            updateStatus()
        }

        masterSwitch.setOnCheckedChangeListener {
                _, enabled ->

            if (enabled) {
                enableRestrictions()
            } else {
                disableRestrictions()
            }

            loadCurrentState()
            updateStatus()
        }
    }

    private fun blockFactoryReset() {
        dpm.addUserRestriction(
            admin,
            UserManager.DISALLOW_FACTORY_RESET
        )
    }

    private fun allowFactoryReset() {
        dpm.clearUserRestriction(
            admin,
            UserManager.DISALLOW_FACTORY_RESET
        )
    }

    private fun enableRestrictions() {
        blockFactoryReset()

        dpm.setCameraDisabled(
            admin,
            true
        )
    }

    private fun disableRestrictions() {
        allowFactoryReset()

        dpm.setCameraDisabled(
            admin,
            false
        )
    }

    private fun loadCurrentState() {

        factoryResetSwitch.isChecked =
            dpm.getUserRestrictions(admin)
                .getBoolean(
                    UserManager.DISALLOW_FACTORY_RESET,
                    false
                )

        cameraSwitch.isChecked =
            dpm.getCameraDisabled(admin)

        masterSwitch.isChecked =
            factoryResetSwitch.isChecked &&
            cameraSwitch.isChecked
    }

    private fun updateStatus() {

        val factoryBlocked =
            factoryResetSwitch.isChecked

        val cameraBlocked =
            cameraSwitch.isChecked

        statusText.text =
            "Factory reset: " +
                    if (factoryBlocked) "BLOCKED"
                    else "ALLOWED" +
                    "\nCamera: " +
                    if (cameraBlocked) "DISABLED"
                    else "ENABLED"
    }
}
