package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.DeskFlowViewModel

@Composable
fun HostScreen(
    viewModel: DeskFlowViewModel,
    onRequestScreenCapture: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hostState by viewModel.hostState.collectAsState()
    var partnerId by remember { mutableStateOf("") }
    var partnerPassword by remember { mutableStateOf("") }
    val isConnecting by viewModel.viewerClient.isConnecting.collectAsState()

    // UltraViewer Authentic Window Theme Colors
    val uvHeaderBlue = Color(0xFF8FAED9)
    val uvCardBg = Color(0xFFFFFFFF)
    val uvAccentBlue = Color(0xFF0078D7)
    val uvTextDark = Color(0xFF222222)
    val uvTextGray = Color(0xFF555555)
    val uvBorder = Color(0xFFD4DEEC)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFE8EEF5))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // UltraViewer Window Top Title & Menu Bar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
                colors = CardDefaults.cardColors(containerColor = uvHeaderBlue)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(uvAccentBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("U", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Text(
                            text = "UltraViewer 6.6 - Remote Support",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF1A3254)
                        )
                    }

                    Icon(
                        Icons.Default.HelpOutline,
                        contentDescription = "Help",
                        tint = uvAccentBlue,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable {
                                Toast.makeText(context, "Give your ID to your partner to share screen, or enter partner's ID to connect.", Toast.LENGTH_LONG).show()
                            }
                    )
                }
            }
        }

        // Section 1: Allow Remote Control (Left Column in Desktop UltraViewer)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = uvCardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, uvBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Sensors, contentDescription = null, tint = uvAccentBlue, modifier = Modifier.size(24.dp))
                        Text(
                            text = "Allow Remote Control",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = uvAccentBlue
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Please tell your partner the following ID and Password if you would like to allow remote control",
                        fontSize = 12.sp,
                        color = uvTextGray,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Your ID Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Your ID",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = uvTextDark,
                            modifier = Modifier.width(80.dp)
                        )
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(2.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7F9DB9)),
                            color = Color(0xFFF9FBFE)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = hostState.remoteId.replace(" ", ""),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF004085)
                                )
                                IconButton(
                                    modifier = Modifier.size(24.dp),
                                    onClick = {
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText("UltraViewer ID", hostState.remoteId.replace(" ", "")))
                                        Toast.makeText(context, "ID copied to clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp), tint = uvAccentBlue)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Password Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Password",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = uvTextDark,
                            modifier = Modifier.width(80.dp)
                        )
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(2.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7F9DB9)),
                            color = Color(0xFFF9FBFE)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = hostState.pinCode,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF004085)
                                )
                                Row {
                                    IconButton(
                                        modifier = Modifier.size(24.dp),
                                        onClick = {
                                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            cm.setPrimaryClip(ClipData.newPlainText("Password", hostState.pinCode))
                                            Toast.makeText(context, "Password copied", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp), tint = Color.Gray)
                                    }
                                    IconButton(
                                        modifier = Modifier.size(24.dp),
                                        onClick = {
                                            viewModel.regeneratePin()
                                            Toast.makeText(context, "New Password generated", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(16.dp), tint = uvAccentBlue)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Broadcast Screen Share Button
                    if (!hostState.isRunning) {
                        Button(
                            onClick = {
                                viewModel.startHostServer { onRequestScreenCapture() }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = uvAccentBlue)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ALLOW SCREEN BROADCAST", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.stopHostServer(context) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC3545))
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("STOP SCREEN BROADCAST", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = uvBorder)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Unattended access",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = uvTextDark,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = true,
                            onCheckedChange = {},
                            colors = CheckboxDefaults.colors(checkedColor = uvAccentBlue)
                        )
                        Text("Run UltraViewer with System", fontSize = 12.sp, color = uvTextDark)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = true,
                            onCheckedChange = {},
                            colors = CheckboxDefaults.colors(checkedColor = uvAccentBlue)
                        )
                        Text("Prevent device from going to sleep", fontSize = 12.sp, color = uvTextDark)
                    }
                }
            }
        }

        // Section 2: Control a Remote Computer (Right Column in Desktop UltraViewer)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = uvCardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, uvBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = uvAccentBlue, modifier = Modifier.size(24.dp))
                        Text(
                            text = "Control a Remote Computer",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = uvAccentBlue
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Please enter your partner's ID to remote control your partner's computer or display",
                        fontSize = 12.sp,
                        color = uvTextGray,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Partner ID Input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Partner ID",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = uvTextDark,
                            modifier = Modifier.width(80.dp)
                        )
                        OutlinedTextField(
                            value = partnerId,
                            onValueChange = { partnerId = it },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            placeholder = { Text("Enter ID or IP", fontSize = 13.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = uvAccentBlue,
                                unfocusedBorderColor = Color(0xFF7F9DB9),
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black
                            ),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Partner Password Input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Password",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = uvTextDark,
                            modifier = Modifier.width(80.dp)
                        )
                        OutlinedTextField(
                            value = partnerPassword,
                            onValueChange = { partnerPassword = it },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            placeholder = { Text("Password", fontSize = 13.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = uvAccentBlue,
                                unfocusedBorderColor = Color(0xFF7F9DB9),
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black
                            ),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Connect Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                val target = if (partnerId.isBlank()) "127.0.0.1:${hostState.port}" else partnerId
                                viewModel.connectToRemote(target, partnerPassword) { success, msg ->
                                    Toast.makeText(context, if (success) "Connected to partner!" else msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF0F4F9)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7F9DB9)),
                            modifier = Modifier.height(42.dp)
                        ) {
                            if (isConnecting) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = uvAccentBlue, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Connecting...", color = uvTextDark, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            } else {
                                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = uvAccentBlue, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Connect to partner", color = Color(0xFF1A3254), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        // Bottom Status Bar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F9)),
                border = androidx.compose.foundation.BorderStroke(1.dp, uvBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (hostState.isRunning) Color(0xFF28A745) else Color(0xFF6C757D))
                        )
                        Text(
                            text = if (hostState.isRunning) "Broadcasting Live (ID: ${hostState.remoteId.replace(" ", "")})" else "Ready to connect",
                            fontSize = 12.sp,
                            color = uvTextDark,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = "UltraViewer Remote Engine",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
