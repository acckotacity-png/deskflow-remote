package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.QualityPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Random

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("deskflow_prefs", Context.MODE_PRIVATE)

    private val _remoteId = MutableStateFlow(getOrGenerateRemoteId())
    val remoteId: StateFlow<String> = _remoteId.asStateFlow()

    private val _pinCode = MutableStateFlow(getOrGeneratePin())
    val pinCode: StateFlow<String> = _pinCode.asStateFlow()

    private val _serverPort = MutableStateFlow(prefs.getInt("server_port", 8080))
    val serverPort: StateFlow<Int> = _serverPort.asStateFlow()

    private val _allowRemoteControl = MutableStateFlow(prefs.getBoolean("allow_remote_control", true))
    val allowRemoteControl: StateFlow<Boolean> = _allowRemoteControl.asStateFlow()

    private val _allowClipboardSync = MutableStateFlow(prefs.getBoolean("allow_clipboard_sync", true))
    val allowClipboardSync: StateFlow<Boolean> = _allowClipboardSync.asStateFlow()

    private val _qualityPreset = MutableStateFlow(
        QualityPreset.valueOf(prefs.getString("quality_preset", QualityPreset.BALANCED.name) ?: QualityPreset.BALANCED.name)
    )
    val qualityPreset: StateFlow<QualityPreset> = _qualityPreset.asStateFlow()

    private val _unattendedPassword = MutableStateFlow(prefs.getString("unattended_password", "") ?: "")
    val unattendedPassword: StateFlow<String> = _unattendedPassword.asStateFlow()

    private val _enableUnattendedAccess = MutableStateFlow(prefs.getBoolean("enable_unattended_access", false))
    val enableUnattendedAccess: StateFlow<Boolean> = _enableUnattendedAccess.asStateFlow()

    private fun getOrGenerateRemoteId(): String {
        var id = prefs.getString("remote_id", null)
        if (id == null) {
            val r = Random()
            val p1 = 100 + r.nextInt(900)
            val p2 = 100 + r.nextInt(900)
            val p3 = 100 + r.nextInt(900)
            id = "$p1 $p2 $p3"
            prefs.edit().putString("remote_id", id).apply()
        }
        return id
    }

    private fun getOrGeneratePin(): String {
        val lastTime = prefs.getLong("pin_generated_at", 0L)
        val now = System.currentTimeMillis()
        var pin = prefs.getString("pin_code", null)
        if (pin == null || now - lastTime > 3600000L) { // 1 hour
            val r = Random()
            pin = String.format("%04d", r.nextInt(10000))
            prefs.edit().putString("pin_code", pin).putLong("pin_generated_at", now).apply()
        }
        return pin
    }

    fun regeneratePin(): String {
        val r = Random()
        val newPin = String.format("%04d", r.nextInt(10000))
        prefs.edit().putString("pin_code", newPin).putLong("pin_generated_at", System.currentTimeMillis()).apply()
        _pinCode.value = newPin
        return newPin
    }

    fun checkAndRotateHourlyPin(): String {
        val lastTime = prefs.getLong("pin_generated_at", 0L)
        val now = System.currentTimeMillis()
        if (now - lastTime > 3600000L) {
            return regeneratePin()
        }
        return _pinCode.value
    }

    fun setServerPort(port: Int) {
        prefs.edit().putInt("server_port", port).apply()
        _serverPort.value = port
    }

    fun setAllowRemoteControl(allow: Boolean) {
        prefs.edit().putBoolean("allow_remote_control", allow).apply()
        _allowRemoteControl.value = allow
    }

    fun setAllowClipboardSync(allow: Boolean) {
        prefs.edit().putBoolean("allow_clipboard_sync", allow).apply()
        _allowClipboardSync.value = allow
    }

    fun setQualityPreset(preset: QualityPreset) {
        prefs.edit().putString("quality_preset", preset.name).apply()
        _qualityPreset.value = preset
    }

    fun setUnattendedAccess(enabled: Boolean, password: String) {
        prefs.edit()
            .putBoolean("enable_unattended_access", enabled)
            .putString("unattended_password", password)
            .apply()
        _enableUnattendedAccess.value = enabled
        _unattendedPassword.value = password
    }
}
