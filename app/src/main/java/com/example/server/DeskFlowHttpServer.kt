package com.example.server

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.data.SettingsManager
import com.example.model.ConnectedClient
import com.example.model.RemoteKeyEvent
import com.example.model.RemoteTouchEvent
import com.example.service.ScreenStreamManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID

class DeskFlowHttpServer(
    private val context: Context,
    private val settingsManager: SettingsManager
) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null

    @Volatile
    var isRunning = false
        private set

    fun start(port: Int = 8080): Boolean {
        if (isRunning) return true
        return try {
            val ss = ServerSocket(port)
            serverSocket = ss
            isRunning = true

            serverJob = scope.launch {
                while (isActive && !ss.isClosed) {
                    try {
                        val clientSocket = ss.accept()
                        scope.launch {
                            handleClient(clientSocket)
                        }
                    } catch (e: Exception) {
                        if (!ss.isClosed) Log.e("DeskFlowServer", "Accept error", e)
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.e("DeskFlowServer", "Failed to bind port $port", e)
            isRunning = false
            false
        }
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {}
        serverJob?.cancel()
        serverSocket = null
        ScreenStreamManager.clearAllClients()
    }

    private suspend fun handleClient(socket: Socket) {
        val clientIp = socket.inetAddress.hostAddress ?: "unknown"
        val clientId = UUID.randomUUID().toString()
        var userAgent = "Web Browser"

        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val output = socket.getOutputStream()

            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return

            val method = parts[0]
            val path = parts[1]

            // Read HTTP headers
            var contentLength = 0
            var headerLine = reader.readLine()
            while (!headerLine.isNullOrEmpty()) {
                val lower = headerLine.lowercase()
                if (lower.startsWith("user-agent:")) {
                    userAgent = headerLine.substringAfter(":").trim()
                } else if (lower.startsWith("content-length:")) {
                    contentLength = headerLine.substringAfter(":").trim().toIntOrNull() ?: 0
                }
                headerLine = reader.readLine()
            }

            // Register client
            ScreenStreamManager.registerClient(
                ConnectedClient(
                    id = clientId,
                    ipAddress = clientIp,
                    userAgent = userAgent,
                    isAuthenticated = false
                )
            )

            when {
                path == "/" || path.startsWith("/?") -> {
                    // Serve Web Portal
                    val remoteId = settingsManager.remoteId.value
                    val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
                    val html = WebPortalHtml.getHtml(remoteId, deviceName, requiresAuth = true)
                    val bytes = html.toByteArray(Charsets.UTF_8)
                    val response = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: text/html; charset=utf-8\r\n" +
                            "Content-Length: ${bytes.size}\r\n" +
                            "Connection: close\r\n\r\n"
                    output.write(response.toByteArray(Charsets.UTF_8))
                    output.write(bytes)
                    output.flush()
                }

                path.startsWith("/stream.mjpeg") -> {
                    // Stream live MJPEG frames
                    output.write(
                        ("HTTP/1.1 200 OK\r\n" +
                                "Connection: close\r\n" +
                                "Cache-Control: no-cache, no-store, must-revalidate, pre-check=0, post-check=0, max-age=0\r\n" +
                                "Pragma: no-cache\r\n" +
                                "Access-Control-Allow-Origin: *\r\n" +
                                "Content-Type: multipart/x-mixed-replace; boundary=--frame\r\n\r\n").toByteArray()
                    )
                    output.flush()

                    // Stream loop
                    while (scope.isActive && !socket.isClosed && isRunning) {
                        val frame = ScreenStreamManager.getLatestFrame()
                        if (frame != null) {
                            val header = "--frame\r\nContent-Type: image/jpeg\r\nContent-Length: ${frame.size}\r\n\r\n"
                            output.write(header.toByteArray())
                            output.write(frame)
                            output.write("\r\n".toByteArray())
                            output.flush()
                        }
                        kotlinx.coroutines.delay(33) // ~30 fps
                    }
                }

                path.startsWith("/api/status") -> {
                    val statusJson = JSONObject().apply {
                        put("running", true)
                        put("fps", ScreenStreamManager.isRealScreenCapturing.value.let { if (it) 30 else 25 })
                        put("deviceName", "${Build.MANUFACTURER} ${Build.MODEL}")
                        put("remoteId", settingsManager.remoteId.value)
                    }.toString()

                    sendJsonResponse(output, 200, statusJson)
                }

                path.startsWith("/api/auth") && method == "POST" -> {
                    val body = readBody(reader, contentLength)
                    val json = try { JSONObject(body) } catch (e: Exception) { JSONObject() }
                    val submittedPin = json.optString("pin", "")

                    val targetPin = settingsManager.pinCode.value
                    val unattendedPassword = settingsManager.unattendedPassword.value
                    val unattendedEnabled = settingsManager.enableUnattendedAccess.value

                    val isValid = submittedPin == targetPin || (unattendedEnabled && submittedPin == unattendedPassword)

                    if (isValid) {
                        ScreenStreamManager.authenticateClient(clientId)
                        val resp = JSONObject().apply {
                            put("success", true)
                            put("message", "Authenticated successfully")
                        }.toString()
                        sendJsonResponse(output, 200, resp)
                    } else {
                        val resp = JSONObject().apply {
                            put("success", false)
                            put("message", "Invalid PIN or access code")
                        }.toString()
                        sendJsonResponse(output, 401, resp)
                    }
                }

                path.startsWith("/api/touch") && method == "POST" -> {
                    val body = readBody(reader, contentLength)
                    if (settingsManager.allowRemoteControl.value) {
                        try {
                            val json = JSONObject(body)
                            val action = json.optString("action", "move")
                            val xRatio = json.optDouble("xRatio", 0.0).toFloat()
                            val yRatio = json.optDouble("yRatio", 0.0).toFloat()
                            val button = json.optInt("button", 0)
                            ScreenStreamManager.recordTouch(RemoteTouchEvent(action, xRatio, yRatio, button))
                        } catch (e: Exception) {}
                    }
                    sendJsonResponse(output, 200, """{"success":true}""")
                }

                path.startsWith("/api/key") && method == "POST" -> {
                    val body = readBody(reader, contentLength)
                    if (settingsManager.allowRemoteControl.value) {
                        try {
                            val json = JSONObject(body)
                            val keyType = json.optString("keyType", "action")
                            val actionName = json.optString("actionName", "")
                            val text = json.optString("text", "")
                            ScreenStreamManager.recordKey(RemoteKeyEvent(keyType, actionName, text))
                        } catch (e: Exception) {}
                    }
                    sendJsonResponse(output, 200, """{"success":true}""")
                }

                path.startsWith("/api/clipboard") -> {
                    if (method == "POST") {
                        val body = readBody(reader, contentLength)
                        if (settingsManager.allowClipboardSync.value) {
                            val json = JSONObject(body)
                            val text = json.optString("text", "")
                            if (text.isNotEmpty()) {
                                Handler(Looper.getMainLooper()).post {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    cm?.setPrimaryClip(ClipData.newPlainText("DeskFlow", text))
                                }
                            }
                        }
                        sendJsonResponse(output, 200, """{"success":true}""")
                    } else {
                        sendJsonResponse(output, 200, """{"text":""}""")
                    }
                }

                path.startsWith("/api/screenshot") -> {
                    val frame = ScreenStreamManager.getLatestFrame()
                    if (frame != null) {
                        val resp = "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: image/jpeg\r\n" +
                                "Content-Disposition: attachment; filename=\"deskflow_screen.jpg\"\r\n" +
                                "Content-Length: ${frame.size}\r\n" +
                                "Connection: close\r\n\r\n"
                        output.write(resp.toByteArray())
                        output.write(frame)
                        output.flush()
                    } else {
                        sendJsonResponse(output, 404, """{"error":"No frame ready"}""")
                    }
                }

                else -> {
                    sendJsonResponse(output, 404, """{"error":"Not Found"}""")
                }
            }
        } catch (e: Exception) {
            // Client closed connection
        } finally {
            ScreenStreamManager.unregisterClient(clientId)
            try { socket.close() } catch (e: Exception) {}
        }
    }

    private fun readBody(reader: BufferedReader, length: Int): String {
        if (length <= 0) return ""
        val charArray = CharArray(length)
        var totalRead = 0
        while (totalRead < length) {
            val read = reader.read(charArray, totalRead, length - totalRead)
            if (read == -1) break
            totalRead += read
        }
        return String(charArray, 0, totalRead)
    }

    private fun sendJsonResponse(output: OutputStream, code: Int, json: String) {
        val bytes = json.toByteArray(Charsets.UTF_8)
        val status = if (code == 200) "OK" else if (code == 401) "Unauthorized" else "Not Found"
        val response = "HTTP/1.1 $code $status\r\n" +
                "Content-Type: application/json; charset=utf-8\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
                "Access-Control-Allow-Headers: Content-Type\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n\r\n"
        output.write(response.toByteArray(Charsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    companion object {
        fun getLocalIpAddress(): String {
            try {
                val interfaces = NetworkInterface.getNetworkInterfaces()
                while (interfaces.hasMoreElements()) {
                    val networkInterface = interfaces.nextElement()
                    if (networkInterface.isLoopback || !networkInterface.isUp) continue
                    val addresses = networkInterface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val address = addresses.nextElement()
                        if (address is Inet4Address && !address.isLoopbackAddress) {
                            val ip = address.hostAddress
                            if (ip != null && (ip.startsWith("192.") || ip.startsWith("10.") || ip.startsWith("172."))) {
                                return ip
                            }
                        }
                    }
                }
            } catch (e: Exception) {}
            return "127.0.0.1"
        }
    }
}
