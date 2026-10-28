package com.rbn.qtsettings.services

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import android.widget.Toast
import com.rbn.qtsettings.MainActivity
import com.rbn.qtsettings.R
import com.rbn.qtsettings.data.PreferencesManager
import com.rbn.qtsettings.utils.VpnDetectionUtils
import com.rbn.qtsettings.utils.WireGuardRemoteControl

/**
 * Quick Settings tile that toggles one tunnel in the official WireGuard app
 * via its remote-control intents. See [WireGuardRemoteControl] for the
 * limits: state here means "a VPN is active", and a sent command can be
 * silently ignored by the WireGuard app (remote control off, wrong tunnel
 * name, permission missing).
 *
 * Unlike the DNS/USB tiles this one has no auto-revert: the tunnel is owned
 * by another app, so this app cannot reliably restore a previous state.
 * Toggling from the lock screen always asks for unlock first, because a
 * tunnel toggle reroutes all network traffic.
 */
class WireGuardTileService : TileService() {

    private lateinit var prefsManager: PreferencesManager
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        prefsManager = PreferencesManager.getInstance(applicationContext)
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
        mainHandler.postDelayed({ updateTile() }, 1500)
    }

    override fun onClick() {
        super.onClick()
        if (isLocked) {
            unlockAndRun { performToggle() }
            return
        }
        performToggle()
    }

    private fun performToggle() {
        val tunnelName = prefsManager.getWireGuardTunnelNameBlocking()
        if (tunnelName == null) {
            Toast.makeText(this, R.string.toast_wireguard_no_tunnel, Toast.LENGTH_LONG).show()
            openAppSettings()
            return
        }

        if (!WireGuardRemoteControl.isWireGuardAppInstalled(this)) {
            Toast.makeText(this, R.string.toast_wireguard_not_installed, Toast.LENGTH_LONG)
                .show()
            openAppSettings()
            return
        }

        if (!WireGuardRemoteControl.hasControlPermission(this)) {
            Toast.makeText(this, R.string.toast_wireguard_permission_missing, Toast.LENGTH_LONG)
                .show()
            openAppSettings()
            return
        }

        val up = !VpnDetectionUtils.isVpnActive(this)
        val sent = WireGuardRemoteControl.sendTunnelCommand(this, tunnelName, up)
        if (!sent) {
            Toast.makeText(this, R.string.toast_error_saving_settings, Toast.LENGTH_SHORT).show()
            return
        }
        Log.i("WireGuardTile", "Sent tunnel ${if (up) "UP" else "DOWN"} for $tunnelName")
        // The tunnel takes a moment to change the VPN transport; refresh
        // once after a delay so the tile does not stay stale until the
        // panel is reopened.
        updateTile()
        mainHandler.postDelayed({ updateTile() }, 2000)
    }

    private fun openAppSettings() {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("android.intent.extra.COMPONENT_NAME", "WireGuardTileService")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("WireGuardTile", "Error opening app settings", e)
        }
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        tile.subtitle = ""

        val tunnelName = prefsManager.getWireGuardTunnelNameBlocking()
        if (tunnelName == null) {
            tile.state = Tile.STATE_UNAVAILABLE
            tile.label = getString(R.string.tile_wireguard_no_tunnel)
            tile.icon = Icon.createWithResource(this, R.drawable.ic_wireguard_off)
            tile.updateTile()
            return
        }

        val label = if (tunnelName.length > 15) tunnelName.take(12) + "..." else tunnelName
        if (VpnDetectionUtils.isVpnActive(this)) {
            tile.state = Tile.STATE_ACTIVE
            tile.label = label
            tile.icon = Icon.createWithResource(this, R.drawable.ic_wireguard_on)
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.label = label
            tile.icon = Icon.createWithResource(this, R.drawable.ic_wireguard_off)
        }
        tile.updateTile()
    }

    override fun onDestroy() {
        super.onDestroy()
        mainHandler.removeCallbacksAndMessages(null)
    }
}
