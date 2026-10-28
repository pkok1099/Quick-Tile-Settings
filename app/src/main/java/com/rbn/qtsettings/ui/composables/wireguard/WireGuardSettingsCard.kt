package com.rbn.qtsettings.ui.composables.wireguard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rbn.qtsettings.R
import com.rbn.qtsettings.utils.WireGuardRemoteControl
import com.rbn.qtsettings.viewmodel.MainViewModel

@Composable
fun WireGuardSettingsCard(
    viewModel: MainViewModel,
    onRequestWireGuardControlPermission: () -> Unit
) {
    val context = LocalContext.current
    val tunnelName by viewModel.wireGuardTunnelName.collectAsState()
    val tunnelNames by viewModel.wireGuardTunnelNames.collectAsState()
    val isInstalled by viewModel.isWireGuardAppInstalled.collectAsState()
    val hasPermission by viewModel.hasWireGuardControlPermission.collectAsState()
    var newTunnelName by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(contentColor = MaterialTheme.colorScheme.onSurface)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.setting_title_wireguard),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = stringResource(R.string.setting_desc_wireguard_remote),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // The WireGuard app exposes no tunnel list, so connections are
            // entered here by hand. Tapping a row selects it for the tile;
            // long-pressing the tile opens a quick-action menu to pick and connect.
            Text(
                text = stringResource(R.string.wireguard_connections_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            if (tunnelNames.isEmpty()) {
                Text(
                    text = stringResource(R.string.wireguard_connections_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            } else {
                tunnelNames.forEach { name ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = name == tunnelName,
                            onClick = { viewModel.setWireGuardTunnelName(name) }
                        )
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.removeWireGuardTunnelName(name) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(
                                    R.string.wireguard_connection_delete
                                )
                            )
                        }
                    }
                }
            }
            OutlinedTextField(
                value = newTunnelName,
                onValueChange = { newTunnelName = it },
                label = { Text(stringResource(R.string.wireguard_tunnel_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    viewModel.addWireGuardTunnelName(newTunnelName)
                    newTunnelName = ""
                },
                enabled = newTunnelName.trim().isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.wireguard_connection_add))
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(
                    if (isInstalled) {
                        R.string.wireguard_status_installed
                    } else {
                        R.string.wireguard_status_not_installed
                    }
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isInstalled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
            Text(
                text = stringResource(
                    if (hasPermission) {
                        R.string.wireguard_status_permission_granted
                    } else {
                        R.string.wireguard_status_permission_missing
                    }
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = if (hasPermission) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.error
                },
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (!isInstalled) {
                Button(
                    onClick = { WireGuardRemoteControl.openWireGuardInStore(context) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.wireguard_button_open_store))
                }
            } else {
                if (!hasPermission) {
                    Button(
                        onClick = onRequestWireGuardControlPermission,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.wireguard_button_request_permission))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                OutlinedButton(
                    onClick = { WireGuardRemoteControl.openWireGuardApp(context) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.wireguard_button_open_app))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.wireguard_note_tile_state),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}
