package com.rbn.qtsettings.data

import android.content.Context
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class WireGuardTunnelListTest {

    private lateinit var context: Context
    private lateinit var prefsManager: PreferencesManager

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        resetPreferencesManagerSingleton()
        clearPreferences()
        prefsManager = PreferencesManager.getInstance(context)
    }

    @After
    fun tearDown() {
        clearPreferences()
        resetPreferencesManagerSingleton()
    }

    @Test
    fun addTunnel_selectsItAndKeepsListSorted() {
        prefsManager.addWireGuardTunnelName("zeta")
        prefsManager.addWireGuardTunnelName("alpha")

        assertEquals(listOf("alpha", "zeta"), prefsManager.wireGuardTunnelNames.value)
        assertEquals("alpha", prefsManager.wireGuardTunnelName.value)
    }

    @Test
    fun removeSelectedTunnel_selectsFirstRemaining() {
        prefsManager.addWireGuardTunnelName("alpha")
        prefsManager.addWireGuardTunnelName("beta")

        prefsManager.removeWireGuardTunnelName("beta")

        assertEquals(listOf("alpha"), prefsManager.wireGuardTunnelNames.value)
        assertEquals("alpha", prefsManager.wireGuardTunnelName.value)
    }

    @Test
    fun removeLastTunnel_clearsSelection() {
        prefsManager.addWireGuardTunnelName("only")

        prefsManager.removeWireGuardTunnelName("only")

        assertEquals(emptyList<String>(), prefsManager.wireGuardTunnelNames.value)
        assertNull(prefsManager.wireGuardTunnelName.value)
    }

    @Test
    fun legacySingleName_migratesToListOnLoad() {
        // A name saved before the list existed: only the old key is present.
        context.getSharedPreferences("qt_settings_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("wireguard_tunnel_name", "legacy")
            .commit()
        resetPreferencesManagerSingleton()
        prefsManager = PreferencesManager.getInstance(context)

        assertEquals(listOf("legacy"), prefsManager.wireGuardTunnelNames.value)
        assertEquals("legacy", prefsManager.wireGuardTunnelName.value)
    }

    private fun clearPreferences() {
        context.getSharedPreferences("qt_settings_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    private fun resetPreferencesManagerSingleton() {
        val instanceField = PreferencesManager::class.java.getDeclaredField("INSTANCE")
        instanceField.isAccessible = true
        instanceField.set(null, null)
    }
}
