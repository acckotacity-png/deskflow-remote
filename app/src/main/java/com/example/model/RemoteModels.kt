package com.example.model

data class ConnectedClient(
    val id: String,
    val ipAddress: String,
    val userAgent: String,
    val connectedAt: Long = System.currentTimeMillis(),
    val isAuthenticated: Boolean = false,
    val lastPing: Long = System.currentTimeMillis()
)

data class HostServerState(
    val isRunning: Boolean = false,
    val port: Int = 8080,
    val localIp: String = "127.0.0.1",
    val remoteId: String = "942 813 520",
    val pinCode: String = "6429",
    val connectedClients: List<ConnectedClient> = emptyList(),
    val isScreenCaptureActive: Boolean = false,
    val fps: Int = 30,
    val bytesSent: Long = 0
)

data class RemoteTouchEvent(
    val action: String, // "down", "move", "up"
    val xRatio: Float,  // 0.0 to 1.0
    val yRatio: Float,  // 0.0 to 1.0
    val button: Int = 0 // 0 = primary/left, 1 = secondary/right
)

data class RemoteKeyEvent(
    val keyType: String, // "action" or "text"
    val actionName: String = "", // "back", "home", "recents", "power", "volume_up", "volume_down"
    val text: String = ""
)

enum class QualityPreset(val label: String, val scale: Float, val jpegQuality: Int, val targetFps: Int) {
    BALANCED("Balanced 720p (30 FPS)", 0.75f, 75, 30),
    FAST("Smooth 480p (45 FPS)", 0.5f, 60, 45),
    HIGH("Crisp 1080p (25 FPS)", 1.0f, 85, 25)
}
