package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.QualityPreset
import com.example.ui.DeskFlowViewModel
import com.example.ui.theme.DeskCyanPrimary
import com.example.ui.theme.DeskOrangeAccent
import com.example.ui.theme.DeskStatusActive

@Composable
fun SettingsScreen(
    viewModel: DeskFlowViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pinCode by viewModel.settings.pinCode.collectAsState()
    val serverPort by viewModel.settings.serverPort.collectAsState()
    val allowControl by viewModel.settings.allowRemoteControl.collectAsState()
    val allowClipboard by viewModel.settings.allowClipboardSync.collectAsState()
    val qualityPreset by viewModel.settings.qualityPreset.collectAsState()
    val unattendedEnabled by viewModel.settings.enableUnattendedAccess.collectAsState()
    val unattendedPassword by viewModel.settings.unattendedPassword.collectAsState()

    var showPasswordDialog by remember { mutableStateOf(false) }
    var tempPassword by remember { mutableStateOf(unattendedPassword) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Settings & Security",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Text(
                text = "Configure access permissions, quality, and passwords",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Security & Access Control Section
        item {
            Text(
                text = "ACCESS CONTROL & AUTHENTICATION",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // One-Time Security PIN
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Default Security PIN", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.White)
                            Text(text = "Required for web clients to connect", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = pinCode,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeskOrangeAccent
                            )
                            IconButton(onClick = { viewModel.regeneratePin() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Regenerate", tint = Color.LightGray)
                            }
                        }
                    }

                    // Unattended Access
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Unattended Access", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.White)
                            Text(text = "Allow connecting anytime with a permanent master password", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = unattendedEnabled,
                            onCheckedChange = { checked ->
                                if (checked && unattendedPassword.isEmpty()) {
                                    showPasswordDialog = true
                                } else {
                                    viewModel.saveUnattendedAccess(checked, unattendedPassword)
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = DeskCyanPrimary)
                        )
                    }

                    if (unattendedEnabled) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showPasswordDialog = true },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Master Password", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "•••••••• (tap to change)", fontSize = 13.sp, color = DeskCyanPrimary, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Allow Remote Control
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Allow Remote Touch / Clicks", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.White)
                            Text(text = "Allows remote PC mouse clicks and gestures", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = allowControl,
                            onCheckedChange = { viewModel.toggleRemoteControl(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = DeskCyanPrimary)
                        )
                    }

                    // Allow Clipboard Sync
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Clipboard Synchronization", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.White)
                            Text(text = "Sync copied text between PC and mobile", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = allowClipboard,
                            onCheckedChange = { viewModel.toggleClipboard(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = DeskCyanPrimary)
                        )
                    }
                }
            }
        }

        // Streaming Quality & Network
        item {
            Text(
                text = "STREAMING QUALITY & NETWORK",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(text = "Screen Quality Profile", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.White)

                    QualityPreset.values().forEach { preset ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.updateQualityPreset(preset) },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = preset.label,
                                fontSize = 14.sp,
                                color = if (qualityPreset == preset) DeskCyanPrimary else Color.LightGray,
                                fontWeight = if (qualityPreset == preset) FontWeight.Bold else FontWeight.Normal
                            )
                            if (qualityPreset == preset) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = DeskCyanPrimary)
                            }
                        }
                    }

                    // Port info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "HTTP Web Port", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.White)
                            Text(text = "Default local port for browser client", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            text = "$serverPort",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Accessibility Service Helper
        item {
            Text(
                text = "REMOTE INPUT SERVICE",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "DeskFlow Accessibility", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.White)
                        Text(
                            text = if (viewModel.isAccessibilityEnabled()) "Enabled • PC clicks and navigation gestures active" else "Disabled • Click below to enable in Android settings",
                            fontSize = 12.sp,
                            color = if (viewModel.isAccessibilityEnabled()) DeskStatusActive else Color.Gray
                        )
                    }
                    Button(
                        onClick = { viewModel.openAccessibilitySettings(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = if (viewModel.isAccessibilityEnabled()) Color(0xFF1E293B) else DeskOrangeAccent)
                    ) {
                        Text(
                            text = if (viewModel.isAccessibilityEnabled()) "Settings" else "Enable",
                            color = Color.White
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text("Set Unattended Access Password", color = Color.White) },
            text = {
                Column {
                    OutlinedTextField(
                        value = tempPassword,
                        onValueChange = { tempPassword = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter master password...") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You can enter this password on your PC to connect immediately without device approval.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempPassword.isNotEmpty()) {
                            viewModel.saveUnattendedAccess(true, tempPassword)
                            showPasswordDialog = false
                            Toast.makeText(context, "Unattended password saved", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeskCyanPrimary)
                ) {
                    Text("Save", color = Color(0xFF00363D), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}
