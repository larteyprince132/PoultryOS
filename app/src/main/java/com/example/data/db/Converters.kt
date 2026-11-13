package com.example.data.db

import androidx.room.TypeConverter
import com.example.data.model.SyncStatus

class Converters {
    @TypeConverter
    fun fromSyncStatus(status: SyncStatus?): String {
        return status?.name ?: SyncStatus.PENDING.name
    }

    @TypeConverter
    fun toSyncStatus(value: String?): SyncStatus {
        return try {
            if (value != null) SyncStatus.valueOf(value) else SyncStatus.PENDING
        } catch (e: Exception) {
            SyncStatus.PENDING
        }
    }
}
