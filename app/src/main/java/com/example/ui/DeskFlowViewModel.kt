package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.DeskFlowApp
import com.example.client.RemoteViewerClient
import com.example.data.SettingsManager
import com.example.data.local.RemoteDeviceEntity
import com.example.data.local.SessionLogEntity
import com.example.model.ConnectedClient
import com.example.model.HostServerState
import com.example.model.QualityPreset
import com.example.server.DeskFlowHttpServer
import com.example.service.DeskFlowAccessibilityService
import com.example.service.ScreenShareService
import com.example.service.ScreenStreamManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DeskFlowViewModel(application: Application) : AndroidViewModel(application) {

    private val app = getApplication<DeskFlowApp>()
    private val dao = app.database.deskFlowDao()
    val settings: SettingsManager = app.settingsManager
    val httpServer: DeskFlowHttpServer = app.httpServer
    val viewerClient = RemoteViewerClient()

    val savedDevices: StateFlow<List<RemoteDeviceEntity>> = dao.getAllDevices()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val sessionLogs: StateFlow<List<SessionLogEntity>> = dao.getAllLogs()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _hostState = MutableStateFlow(
        HostServerState(
            isRunning = false,
            port = settings.serverPort.value,
            localIp = DeskFlowHttpServer.getLocalIpAddress(),
            remoteId = settings.remoteId.value,
            pinCode = settings.pinCode.value
        )
    )
    val hostState: StateFlow<HostServerState> = _hostState.asStateFlow()

    val connectedClients: StateFlow<Map<String, ConnectedClient>> = ScreenStreamManager.connectedClients

    private val _activeSessionStartTime = MutableStateFlow(0L)
    val activeSessionStartTime: StateFlow<Long> = _activeSessionStartTime.asStateFlow()

    init {
        // Observe screen capture state
        viewModelScope.launch {
            ScreenStreamManager.isRealScreenCapturing.collect { capturing ->
                _hostState.value = _hostState.value.copy(isScreenCaptureActive = capturing)
            }
        }
        // Observe settings PIN changes
        viewModelScope.launch {
            settings.pinCode.collect { pin ->
                _hostState.value = _hostState.value.copy(pinCode = pin)
            }
        }
        viewModelScope.launch {
            settings.serverPort.collect { port ->
                _hostState.value = _hostState.value.copy(port = port)
            }
        }
        // Hourly PIN rotation ticker
        viewModelScope.launch {
            while (true) {
                settings.checkAndRotateHourlyPin()
                kotlinx.coroutines.delay(60000L) // Check every minute
            }
        }
    }

    fun startHostServer(onNeedScreenCapture: () -> Unit) {
        val port = settings.serverPort.value
        val started = httpServer.start(port)
        if (started) {
            val ip = DeskFlowHttpServer.getLocalIpAddress()
            _hostState.value = _hostState.value.copy(
                isRunning = true,
                localIp = ip,
                port = port
            )
            _activeSessionStartTime.value = System.currentTimeMillis()
            onNeedScreenCapture()
        }
    }

    fun stopHostServer(context: Context) {
        httpServer.stop()
        val stopServiceIntent = Intent(context, ScreenShareService::class.java).apply {
            action = ScreenShareService.ACTION_STOP
        }
        context.startService(stopServiceIntent)
        _hostState.value = _hostState.value.copy(isRunning = false, isScreenCaptureActive = false)

        val duration = (System.currentTimeMillis() - _activeSessionStartTime.value) / 1000
        if (duration > 5) {
            viewModelScope.launch {
                dao.insertLog(
                    SessionLogEntity(
                        targetName = "Web / Remote Clients",
                        address = "${_hostState.value.localIp}:${_hostState.value.port}",
                        direction = "INCOMING",
                        durationSeconds = duration,
                        dataTransferredMb = (ScreenStreamManager.totalBytesTransferred.get() / (1024.0 * 1024.0))
                    )
                )
            }
        }
    }

    fun refreshNetworkIp() {
        val ip = DeskFlowHttpServer.getLocalIpAddress()
        _hostState.value = _hostState.value.copy(localIp = ip)
    }

    fun regeneratePin(): String {
        return settings.regeneratePin()
    }

    fun updateQualityPreset(preset: QualityPreset) {
        settings.setQualityPreset(preset)
    }

    fun toggleRemoteControl(enabled: Boolean) {
        settings.setAllowRemoteControl(enabled)
    }

    fun toggleClipboard(enabled: Boolean) {
        settings.setAllowClipboardSync(enabled)
    }

    fun saveUnattendedAccess(enabled: Boolean, pass: String) {
        settings.setUnattendedAccess(enabled, pass)
    }

    // Client Actions
    fun connectToRemote(address: String, pin: String, onComplete: (Boolean, String) -> Unit) {
        viewerClient.connect(address, pin) { success, msg ->
            if (success) {
                viewModelScope.launch {
                    dao.insertDevice(
                        RemoteDeviceEntity(
                            name = "Remote System ($address)",
                            hostAddress = address,
                            accessPin = pin,
                            lastConnected = System.currentTimeMillis()
                        )
                    )
                }
            }
            onComplete(success, msg)
        }
    }

    fun disconnectRemoteClient() {
        viewerClient.disconnect()
    }

    fun saveDevice(device: RemoteDeviceEntity) {
        viewModelScope.launch {
            dao.insertDevice(device)
        }
    }

    fun toggleFavorite(device: RemoteDeviceEntity) {
        viewModelScope.launch {
            dao.updateDevice(device.copy(isFavorite = !device.isFavorite))
        }
    }

    fun deleteDevice(device: RemoteDeviceEntity) {
        viewModelScope.launch {
            dao.deleteDevice(device)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            dao.clearLogs()
        }
    }

    fun openAccessibilitySettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: Exception) {}
    }

    fun isAccessibilityEnabled(): Boolean {
        return DeskFlowAccessibilityService.isServiceRunning
    }
}
