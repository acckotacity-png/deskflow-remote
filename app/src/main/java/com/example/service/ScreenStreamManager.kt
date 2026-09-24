package com.example.service

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.example.model.ConnectedClient
import com.example.model.RemoteKeyEvent
import com.example.model.RemoteTouchEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

object ScreenStreamManager {
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    @Volatile
    private var latestJpegFrame: ByteArray? = null

    private val _frameSharedFlow = MutableSharedFlow<ByteArray>(extraBufferCapacity = 2)
    val frameSharedFlow: SharedFlow<ByteArray> = _frameSharedFlow.asSharedFlow()

    private val _connectedClients = MutableStateFlow<Map<String, ConnectedClient>>(emptyMap())
    val connectedClients: StateFlow<Map<String, ConnectedClient>> = _connectedClients.asStateFlow()

    private val _isRealScreenCapturing = MutableStateFlow(false)
    val isRealScreenCapturing: StateFlow<Boolean> = _isRealScreenCapturing.asStateFlow()

    val totalBytesTransferred = AtomicLong(0L)

    private val touchRippleList = ConcurrentHashMap<Long, Pair<Float, Float>>()

    var onDispatchTouch: ((RemoteTouchEvent) -> Unit)? = null
    var onDispatchKey: ((RemoteKeyEvent) -> Unit)? = null

    private var simulationJob: Job? = null

    init {
        startSimulationLoopIfNeeded()
    }

    fun startSimulationLoopIfNeeded() {
        if (simulationJob?.isActive == true) return
        simulationJob = scope.launch {
            val width = 720
            val height = 1520
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

            var frameCount = 0
            while (isActive) {
                if (!_isRealScreenCapturing.value) {
                    frameCount++
                    drawSimulatedScreen(canvas, paint, width, height, timeFormat.format(Date()), frameCount)
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 75, stream)
                    val bytes = stream.toByteArray()
                    updateFrame(bytes)
                }
                delay(40) // ~25 fps
            }
        }
    }

    private fun drawSimulatedScreen(
        canvas: Canvas,
        paint: Paint,
        width: Int,
        height: Int,
        currentTime: String,
        frameTick: Int
    ) {
        // Background - sleek dark phone desktop
        canvas.drawColor(Color.parseColor("#0F172A"))

        // Top Status Bar
        paint.color = Color.parseColor("#1E293B")
        canvas.drawRect(0f, 0f, width.toFloat(), 70f, paint)

        // Time in status bar
        paint.color = Color.WHITE
        paint.textSize = 28f
        paint.isFakeBoldText = true
        canvas.drawText(currentTime, 32f, 48f, paint)

        // Wi-Fi and Battery Icons text
        paint.textSize = 24f
        paint.isFakeBoldText = false
        canvas.drawText("5G  100% ⚡", width - 170f, 48f, paint)

        // Main Widget / Clock Card
        paint.color = Color.parseColor("#1E293B")
        val widgetRect = RectF(40f, 120f, width - 40f, 380f)
        canvas.drawRoundRect(widgetRect, 28f, 28f, paint)

        paint.color = Color.parseColor("#00E5FF")
        paint.textSize = 58f
        paint.isFakeBoldText = true
        canvas.drawText(currentTime, 70f, 220f, paint)

        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 28f
        paint.isFakeBoldText = false
        canvas.drawText("DeskFlow Remote Display", 70f, 275f, paint)

        paint.color = Color.parseColor("#10B981")
        paint.textSize = 22f
        canvas.drawText("● HOST BROADCAST ACTIVE", 70f, 330f, paint)

        // Remote Access Instructions Card
        val infoRect = RectF(40f, 420f, width - 40f, 760f)
        paint.color = Color.parseColor("#131A2B")
        canvas.drawRoundRect(infoRect, 28f, 28f, paint)

        paint.color = Color.parseColor("#FF5722")
        paint.textSize = 34f
        paint.isFakeBoldText = true
        canvas.drawText("Live Interactive Screen", 70f, 480f, paint)

        paint.color = Color.WHITE
        paint.textSize = 26f
        paint.isFakeBoldText = false
        canvas.drawText("1. Click or tap anywhere to send touches", 70f, 540f, paint)
        canvas.drawText("2. Use dock buttons below for Back/Home", 70f, 600f, paint)
        canvas.drawText("3. Real-time sub-second remote control", 70f, 660f, paint)
        canvas.drawText("4. Start Screen Share in app for live OS capture", 70f, 715f, paint)

        // Animated Activity Indicator
        val pulse = (Math.sin(frameTick * 0.1) * 15).toFloat()
        paint.color = Color.parseColor("#00E5FF")
        canvas.drawCircle(width / 2f, 900f, 50f + pulse, paint)

        paint.color = Color.WHITE
        paint.textSize = 28f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("DeskFlow Live Link", width / 2f, 1020f, paint)
        paint.textAlign = Paint.Align.LEFT

        // Draw Touch Ripples
        val now = System.currentTimeMillis()
        val it = touchRippleList.entries.iterator()
        while (it.hasNext()) {
            val entry = it.next()
            val age = now - entry.key
            if (age > 600) {
                it.remove()
            } else {
                val radius = (age * 0.2f)
                val alpha = (255 * (1f - age / 600f)).toInt()
                paint.color = Color.argb(alpha, 255, 87, 34)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 6f
                canvas.drawCircle(entry.value.first * width, entry.value.second * height, radius, paint)
                paint.style = Paint.Style.FILL
            }
        }
    }

    fun setRealScreenCapturing(active: Boolean) {
        _isRealScreenCapturing.value = active
    }

    fun updateFrame(jpegBytes: ByteArray) {
        latestJpegFrame = jpegBytes
        _frameSharedFlow.tryEmit(jpegBytes)
        totalBytesTransferred.addAndGet(jpegBytes.size.toLong())
    }

    fun getLatestFrame(): ByteArray? = latestJpegFrame

    fun recordTouch(event: RemoteTouchEvent) {
        touchRippleList[System.currentTimeMillis()] = Pair(event.xRatio, event.yRatio)
        onDispatchTouch?.invoke(event)
    }

    fun recordKey(event: RemoteKeyEvent) {
        onDispatchKey?.invoke(event)
    }

    fun registerClient(client: ConnectedClient) {
        val current = _connectedClients.value.toMutableMap()
        current[client.id] = client
        _connectedClients.value = current
    }

    fun authenticateClient(clientId: String) {
        val current = _connectedClients.value.toMutableMap()
        current[clientId]?.let {
            current[clientId] = it.copy(isAuthenticated = true)
            _connectedClients.value = current
        }
    }

    fun unregisterClient(clientId: String) {
        val current = _connectedClients.value.toMutableMap()
        current.remove(clientId)
        _connectedClients.value = current
    }

    fun clearAllClients() {
        _connectedClients.value = emptyMap()
    }
}
