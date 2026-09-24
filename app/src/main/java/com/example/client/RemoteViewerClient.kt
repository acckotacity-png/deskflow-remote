package com.example.client

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.example.model.RemoteKeyEvent
import com.example.model.RemoteTouchEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class RemoteViewerClient {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var clientJob: Job? = null
    private var pingJob: Job? = null

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // infinite for stream
        .build()

    private val restClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    private val _currentBitmap = MutableStateFlow<Bitmap?>(null)
    val currentBitmap: StateFlow<Bitmap?> = _currentBitmap.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _fps = MutableStateFlow(0)
    val fps: StateFlow<Int> = _fps.asStateFlow()

    private val _latencyMs = MutableStateFlow(0L)
    val latencyMs: StateFlow<Long> = _latencyMs.asStateFlow()

    private var activeBaseUrl: String = ""

    fun connect(rawAddress: String, pin: String, onAuthResult: (Boolean, String) -> Unit) {
        disconnect()
        _isConnecting.value = true
        _errorMessage.value = null

        val formattedAddress = when {
            rawAddress.startsWith("http://") || rawAddress.startsWith("https://") -> rawAddress
            rawAddress.contains(":") -> "http://$rawAddress"
            else -> "http://$rawAddress:8080"
        }.trimEnd('/')

        activeBaseUrl = formattedAddress

        scope.launch {
            // First authenticate if PIN is provided
            if (pin.isNotEmpty()) {
                val authSuccess = authenticateWithHost(formattedAddress, pin)
                if (!authSuccess.first) {
                    _isConnecting.value = false
                    _errorMessage.value = authSuccess.second
                    onAuthResult(false, authSuccess.second)
                    return@launch
                }
            }
            onAuthResult(true, "Connected")
            startMjpegStream(formattedAddress)
            startPingLoop(formattedAddress)
        }
    }

    private fun authenticateWithHost(baseUrl: String, pin: String): Pair<Boolean, String> {
        return try {
            val json = JSONObject().apply { put("pin", pin) }.toString()
            val body = json.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$baseUrl/api/auth")
                .post(body)
                .build()

            val response = restClient.newCall(request).execute()
            val respString = response.body?.string() ?: ""
            val respJson = JSONObject(respString)
            val success = respJson.optBoolean("success", false)
            val message = respJson.optString("message", if (success) "Success" else "Auth Failed")
            Pair(success, message)
        } catch (e: Exception) {
            Pair(true, "Assuming direct access") // fallback if auth not enforced
        }
    }

    private fun startMjpegStream(baseUrl: String) {
        clientJob = scope.launch {
            try {
                val request = Request.Builder()
                    .url("$baseUrl/stream.mjpeg")
                    .header("Accept", "multipart/x-mixed-replace")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    _errorMessage.value = "Server returned code: ${response.code}"
                    _isConnecting.value = false
                    return@launch
                }

                _isConnected.value = true
                _isConnecting.value = false

                val stream = BufferedInputStream(response.body?.byteStream())
                var framesInSecond = 0
                var lastFpsCheck = System.currentTimeMillis()

                val boundary = "--frame"
                val boundaryBytes = boundary.toByteArray()

                while (isActive) {
                    val frameBytes = readJpegFrame(stream, boundaryBytes)
                    if (frameBytes != null && frameBytes.isNotEmpty()) {
                        val bitmap = BitmapFactory.decodeByteArray(frameBytes, 0, frameBytes.size)
                        if (bitmap != null) {
                            _currentBitmap.value = bitmap
                            framesInSecond++
                        }
                    }

                    val now = System.currentTimeMillis()
                    if (now - lastFpsCheck >= 1000) {
                        _fps.value = framesInSecond
                        framesInSecond = 0
                        lastFpsCheck = now
                    }
                }
            } catch (e: Exception) {
                if (isActive) {
                    Log.e("RemoteViewer", "Stream read error", e)
                    _errorMessage.value = "Connection lost: ${e.localizedMessage}"
                    _isConnected.value = false
                    _isConnecting.value = false
                }
            }
        }
    }

    private fun startPingLoop(baseUrl: String) {
        pingJob = scope.launch {
            while (isActive) {
                val start = System.currentTimeMillis()
                try {
                    val request = Request.Builder().url("$baseUrl/api/status").build()
                    val response = restClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        _latencyMs.value = System.currentTimeMillis() - start
                    }
                } catch (e: Exception) {}
                delay(2000)
            }
        }
    }

    private fun readJpegFrame(stream: BufferedInputStream, boundaryBytes: ByteArray): ByteArray? {
        val buffer = ByteArrayOutputStream()
        var matched = 0

        // Skip until boundary
        while (true) {
            val b = stream.read()
            if (b == -1) return null
            if (b == boundaryBytes[matched].toInt()) {
                matched++
                if (matched == boundaryBytes.size) break
            } else {
                matched = if (b == boundaryBytes[0].toInt()) 1 else 0
            }
        }

        // Skip headers until \r\n\r\n
        var crlfCount = 0
        while (true) {
            val b = stream.read()
            if (b == -1) return null
            if (b == '\r'.code || b == '\n'.code) {
                crlfCount++
                if (crlfCount == 4) break
            } else {
                crlfCount = 0
            }
        }

        // Read JPEG data until next boundary or EOF
        var prev = 0
        var foundSOI = false
        while (true) {
            val b = stream.read()
            if (b == -1) break

            // Detect SOI 0xFF 0xD8
            if (!foundSOI && prev == 0xFF && b == 0xD8) {
                foundSOI = true
                buffer.write(0xFF)
                buffer.write(0xD8)
                prev = b
                continue
            }

            if (foundSOI) {
                buffer.write(b)
                // Detect EOI 0xFF 0xD9
                if (prev == 0xFF && b == 0xD9) {
                    break
                }
            }
            prev = b
        }

        return if (buffer.size() > 0) buffer.toByteArray() else null
    }

    fun sendTouchEvent(action: String, xRatio: Float, yRatio: Float, button: Int = 0) {
        if (!_isConnected.value || activeBaseUrl.isEmpty()) return
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("action", action)
                    put("xRatio", xRatio.toDouble())
                    put("yRatio", yRatio.toDouble())
                    put("button", button)
                }.toString()
                val body = json.toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("$activeBaseUrl/api/touch")
                    .post(body)
                    .build()
                restClient.newCall(request).execute().close()
            } catch (e: Exception) {}
        }
    }

    fun sendKeyEvent(keyType: String, actionName: String = "", text: String = "") {
        if (!_isConnected.value || activeBaseUrl.isEmpty()) return
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("keyType", keyType)
                    put("actionName", actionName)
                    put("text", text)
                }.toString()
                val body = json.toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("$activeBaseUrl/api/key")
                    .post(body)
                    .build()
                restClient.newCall(request).execute().close()
            } catch (e: Exception) {}
        }
    }

    fun disconnect() {
        clientJob?.cancel()
        clientJob = null
        pingJob?.cancel()
        pingJob = null
        _isConnected.value = false
        _isConnecting.value = false
        _currentBitmap.value = null
        _errorMessage.value = null
    }
}
