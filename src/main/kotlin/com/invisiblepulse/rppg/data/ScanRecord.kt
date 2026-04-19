package com.invisiblepulse.rppg.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_records")
data class ScanRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bpm: Double,
    val bpSystolic: Int,
    val bpDiastolic: Int,
    val hrv: Double,
    val confidence: Double,
    val status: String,
    val timestamp: Long = System.currentTimeMillis()
)
