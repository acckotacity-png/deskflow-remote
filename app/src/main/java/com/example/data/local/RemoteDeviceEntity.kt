package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "remote_devices")
data class RemoteDeviceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val hostAddress: String, // e.g. "192.168.1.55:8080" or 9-digit ID
    val accessPin: String = "",
    val deviceType: String = "PC", // "PC", "Mobile", "Mac", "Linux"
    val lastConnected: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)

@Entity(tableName = "session_logs")
data class SessionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val targetName: String,
    val address: String,
    val direction: String, // "INCOMING" (hosted) or "OUTGOING" (connected to remote)
    val startTime: Long = System.currentTimeMillis(),
    val durationSeconds: Long = 0,
    val dataTransferredMb: Double = 0.0,
    val status: String = "Completed" // "Completed", "Disconnected", "Rejected"
)
