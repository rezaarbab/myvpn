package com.myvpn.app

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.myvpn.app.bg.TunnelService
import com.myvpn.app.data.ProfileStore
import com.myvpn.app.ui.AppNavHost
import com.myvpn.app.ui.theme.MyVpnTheme

class MainActivity : ComponentActivity() {

    private var pendingConnect = false

    private val vpnPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && pendingConnect) {
                pendingConnect = false
                TunnelService.start(this)
            }
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            tryConnect()
        }

    fun requestConnect() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            pendingConnect = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        tryConnect()
    }

    private fun tryConnect() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            pendingConnect = true
            vpnPermissionLauncher.launch(intent)
        } else {
            TunnelService.start(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ProfileStore.init(this)
        setContent {
            MyVpnTheme {
                AppNavHost(onConnect = { requestConnect() })
            }
        }
    }
}
