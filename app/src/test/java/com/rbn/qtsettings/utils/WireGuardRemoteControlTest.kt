package com.rbn.qtsettings.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WireGuardRemoteControlTest {

    @Test
    fun `normalizes tunnel names and rejects blanks`() {
        assertEquals("Home", WireGuardRemoteControl.normalizeTunnelName("  Home  "))
        assertNull(WireGuardRemoteControl.normalizeTunnelName(null))
        assertNull(WireGuardRemoteControl.normalizeTunnelName("   "))
    }

    @Test
    fun `builds an explicit up intent for the WireGuard app`() {
        val intent = WireGuardRemoteControl.buildTunnelIntent("Home", up = true)
        assertEquals(WireGuardRemoteControl.ACTION_SET_TUNNEL_UP, intent.action)
        assertEquals("Home", intent.getStringExtra(WireGuardRemoteControl.EXTRA_TUNNEL))
        assertEquals(WireGuardRemoteControl.WIREGUARD_PACKAGE, intent.`package`)
    }

    @Test
    fun `builds an explicit down intent for the WireGuard app`() {
        val intent = WireGuardRemoteControl.buildTunnelIntent("Home", up = false)
        assertEquals(WireGuardRemoteControl.ACTION_SET_TUNNEL_DOWN, intent.action)
        assertEquals("Home", intent.getStringExtra(WireGuardRemoteControl.EXTRA_TUNNEL))
        assertEquals(WireGuardRemoteControl.WIREGUARD_PACKAGE, intent.`package`)
    }
}
