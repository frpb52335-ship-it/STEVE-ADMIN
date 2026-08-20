package com.steve.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class AdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(context, "STEVE ADMIN Enabled", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Toast.makeText(context, "STEVE ADMIN Disabled", Toast.LENGTH_SHORT).show()
    }
}