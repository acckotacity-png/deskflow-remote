package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DeskFlowDao {
    @Query("SELECT * FROM remote_devices ORDER BY isFavorite DESC, lastConnected DESC")
    fun getAllDevices(): Flow<List<RemoteDeviceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: RemoteDeviceEntity): Long

    @Update
    suspend fun updateDevice(device: RemoteDeviceEntity)

    @Delete
    suspend fun deleteDevice(device: RemoteDeviceEntity)

    @Query("SELECT * FROM session_logs ORDER BY startTime DESC LIMIT 50")
    fun getAllLogs(): Flow<List<SessionLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SessionLogEntity): Long

    @Query("DELETE FROM session_logs")
    suspend fun clearLogs()
}
