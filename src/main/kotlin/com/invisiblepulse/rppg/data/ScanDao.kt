package com.invisiblepulse.rppg.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {

    @Query("SELECT * FROM scan_records ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<ScanRecord>>

    @Insert
    suspend fun insert(record: ScanRecord): Long

    @Delete
    suspend fun delete(record: ScanRecord)

    @Query("DELETE FROM scan_records")
    suspend fun deleteAll()

    @Query("SELECT AVG(bpm) FROM scan_records WHERE timestamp > :since")
    suspend fun averageBpmSince(since: Long): Double?

    @Query("SELECT MAX(bpm) FROM scan_records WHERE timestamp > :since")
    suspend fun maxBpmSince(since: Long): Double?

    @Query("SELECT MIN(bpm) FROM scan_records WHERE timestamp > :since")
    suspend fun minBpmSince(since: Long): Double?

    @Query("SELECT COUNT(*) FROM scan_records")
    suspend fun totalCount(): Int
}
