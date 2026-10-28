package com.rbn.qtsettings.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Remote control for the official WireGuard app (com.wireguard.android).
 *
 * This app owns no tunnel itself: it only asks the WireGuard app to bring a
 * tunnel up or down. That app gates the request behind its own
 * "Allow remote control apps" setting, so a sent command is not a guarantee
 * that the tunnel changed state -- delivery failures are silent for
 * broadcasts, and there is no API to query one tunnel's state from here.
 * Tile state therefore reflects "a VPN is active", not "this tunnel is up".
 */
object WireGuardRemoteControl {
    const val WIREGUARD_PACKAGE = "com.wireguard.android"
    const val PERMISSION_CONTROL_TUNNELS =
        "com.wireguard.android.permission.CONTROL_TUNNELS"
    const val ACTION_SET_TUNNEL_UP = "com.wireguard.android.action.SET_TUNNEL_UP"
    const val ACTION_SET_TUNNEL_DOWN = "com.wireguard.android.action.SET_TUNNEL_DOWN"
    const val EXTRA_TUNNEL = "tunnel"

    fun normalizeTunnelName(name: String?): String? =
        name?.trim()?.takeIf { it.isNotEmpty() }

    fun buildTunnelIntent(tunnelName: String, up: Boolean): Intent =
        Intent(if (up) ACTION_SET_TUNNEL_UP else ACTION_SET_TUNNEL_DOWN)
            .putExtra(EXTRA_TUNNEL, tunnelName)
            // Explicit package: implicit broadcasts to a manifest receiver are
            // not delivered on modern Android, and this also keeps the
            // command from reaching any other app listening for the action.
            .setPackage(WIREGUARD_PACKAGE)

    fun isWireGuardAppInstalled(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    WIREGUARD_PACKAGE,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(WIREGUARD_PACKAGE, 0)
            }
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun hasControlPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, PERMISSION_CONTROL_TUNNELS) ==
            PackageManager.PERMISSION_GRANTED

    /** Returns false only when sending itself threw; a sent broadcast may still be ignored. */
    fun sendTunnelCommand(context: Context, tunnelName: String, up: Boolean): Boolean {
        return try {
            context.sendBroadcast(buildTunnelIntent(tunnelName, up))
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openWireGuardApp(context: Context): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(WIREGUARD_PACKAGE)
                ?: return false
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openWireGuardInStore(context: Context): Boolean {
        return try {
            val marketIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=$WIREGUARD_PACKAGE")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(marketIntent)
            true
        } catch (_: Exception) {
            try {
                val webIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$WIREGUARD_PACKAGE")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(webIntent)
                true
            } catch (_: Exception) {
                false
            }
        }
    }
}
