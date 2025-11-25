// app/src/main/java/com/arranquesuave/motorcontrolapp/ui/screens/BluetoothDeviceDialog.kt
package com.arranquesuave.motorcontrolapp.ui.screens

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BluetoothDeviceDialog(
    devices: List<BluetoothDevice>,
    scanning: Boolean,
    onSelect: (BluetoothDevice) -> Unit,
    onDismiss: () -> Unit,
    onScanAgain: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dispositivos Bluetooth", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                if (scanning) {
                    Text("Buscando dispositivos…", modifier = Modifier.padding(8.dp))
                } else if (devices.isEmpty()) {
                    Text("No se encontró ninguno.\nPulsa 'Buscar otra vez'.", modifier = Modifier.padding(8.dp))
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                    ) {
                        items(devices) { device ->
                            Text(
                                text = "${device.name ?: "Sin nombre"}\n${device.address}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(device) }
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onScanAgain) {
                Text("Buscar otra vez")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}
