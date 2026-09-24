package com.example.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.DeskFlowViewModel
import com.example.ui.theme.DeskCyanPrimary
import com.example.ui.theme.DeskOrangeAccent

@Composable
fun RemoteViewerScreen(
    viewModel: DeskFlowViewModel,
    onCloseViewer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentBitmap by viewModel.viewerClient.currentBitmap.collectAsState()
    val isConnected by viewModel.viewerClient.isConnected.collectAsState()
    val isConnecting by viewModel.viewerClient.isConnecting.collectAsState()
    val fps by viewModel.viewerClient.fps.collectAsState()
    val pingMs by viewModel.viewerClient.latencyMs.collectAsState()

    var isMouseMode by remember { mutableStateOf(false) } // false = Direct Touch, true = Trackpad
    var cursorX by remember { mutableFloatStateOf(300f) }
    var cursorY by remember { mutableFloatStateOf(400f) }

    var showKeyboardDialog by remember { mutableStateOf(false) }
    var typedText by remember { mutableStateOf("") }
    var showShortcutsBar by remember { mutableStateOf(true) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Main Screen View Area
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val containerWidth = maxWidth.value
            val containerHeight = maxHeight.value

            if (currentBitmap != null) {
                val bmp = currentBitmap!!
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(isMouseMode) {
                            if (!isMouseMode) {
                                // Direct Touch Mode
                                detectTapGestures(
                                    onPress = { offset ->
                                        val xRatio = (offset.x / size.width).coerceIn(0f, 1f)
                                        val yRatio = (offset.y / size.height).coerceIn(0f, 1f)
                                        viewModel.viewerClient.sendTouchEvent("down", xRatio, yRatio, 0)
                                        tryAwaitRelease()
                                        viewModel.viewerClient.sendTouchEvent("up", xRatio, yRatio, 0)
                                    }
                                )
                            } else {
                                // Trackpad Mouse Mode
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    cursorX = (cursorX + dragAmount.x).coerceIn(0f, size.width.toFloat())
                                    cursorY = (cursorY + dragAmount.y).coerceIn(0f, size.height.toFloat())
                                    val xRatio = (cursorX / size.width).coerceIn(0f, 1f)
                                    val yRatio = (cursorY / size.height).coerceIn(0f, 1f)
                                    viewModel.viewerClient.sendTouchEvent("move", xRatio, yRatio, 0)
                                }
                            }
                        }
                ) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Remote Screen Stream",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    // Virtual Mouse Cursor if Mouse Mode is active
                    if (isMouseMode) {
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(cursorX.toInt(), cursorY.toInt()) }
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(DeskOrangeAccent)
                                .border(2.dp, Color.White, CircleShape)
                        )
                    }
                }
            } else {
                // Loading or Connecting State
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    CircularProgressIndicator(
                        color = DeskCyanPrimary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isConnecting) "Connecting to Remote Screen..." else "Waiting for screen frames...",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "DeskFlow high-speed video link",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Top Telemetry & Control Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(12.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xCC111827),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3300E5FF))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                    Text(
                        text = "LIVE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                    Text(
                        text = "•",
                        color = Color.DarkGray
                    )
                    Text(
                        text = "${fps} FPS",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = DeskCyanPrimary
                    )
                    Text(
                        text = "•",
                        color = Color.DarkGray
                    )
                    Text(
                        text = "${pingMs}ms",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.LightGray
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Touch Mode / Trackpad Mode Toggle
                    IconButton(
                        onClick = { isMouseMode = !isMouseMode },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            if (isMouseMode) Icons.Default.Mouse else Icons.Default.TouchApp,
                            contentDescription = "Mode",
                            tint = if (isMouseMode) DeskOrangeAccent else DeskCyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Keyboard input button
                    IconButton(
                        onClick = { showKeyboardDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Keyboard,
                            contentDescription = "Keyboard",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Disconnect
                    IconButton(
                        onClick = {
                            viewModel.disconnectRemoteClient()
                            onCloseViewer()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // Bottom Floating Remote Control Dock
        AnimatedVisibility(
            visible = showShortcutsBar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xE6131A2B),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
                shadowElevation = 12.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back
                    IconButton(onClick = { viewModel.viewerClient.sendKeyEvent("action", "back") }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }

                    // Home
                    IconButton(onClick = { viewModel.viewerClient.sendKeyEvent("action", "home") }) {
                        Icon(Icons.Default.RadioButtonUnchecked, contentDescription = "Home", tint = Color.White)
                    }

                    // Recents
                    IconButton(onClick = { viewModel.viewerClient.sendKeyEvent("action", "recents") }) {
                        Icon(Icons.Default.CropSquare, contentDescription = "Recents", tint = Color.White)
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(Color.DarkGray)
                    )

                    // Vol-
                    IconButton(onClick = { viewModel.viewerClient.sendKeyEvent("action", "volume_down") }) {
                        Icon(Icons.Default.VolumeDown, contentDescription = "Vol-", tint = Color.LightGray)
                    }

                    // Vol+
                    IconButton(onClick = { viewModel.viewerClient.sendKeyEvent("action", "volume_up") }) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Vol+", tint = Color.LightGray)
                    }

                    // Power / Lock
                    IconButton(onClick = { viewModel.viewerClient.sendKeyEvent("action", "power") }) {
                        Icon(Icons.Default.PowerSettingsNew, contentDescription = "Power", tint = DeskOrangeAccent)
                    }
                }
            }
        }
    }

    // Keyboard Text Input Modal
    if (showKeyboardDialog) {
        AlertDialog(
            onDismissRequest = { showKeyboardDialog = false },
            title = { Text("Send Text to Remote System", color = Color.White) },
            text = {
                Column {
                    OutlinedTextField(
                        value = typedText,
                        onValueChange = { typedText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("remote_text_field"),
                        placeholder = { Text("Type any message or command...") },
                        singleLine = false,
                        maxLines = 4
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Text will be typed directly on the remote system.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (typedText.isNotEmpty()) {
                            viewModel.viewerClient.sendKeyEvent("text", text = typedText)
                            Toast.makeText(context, "Text dispatched", Toast.LENGTH_SHORT).show()
                            typedText = ""
                            showKeyboardDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeskCyanPrimary)
                ) {
                    Text("Send to Screen", color = Color(0xFF00363D), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showKeyboardDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}
