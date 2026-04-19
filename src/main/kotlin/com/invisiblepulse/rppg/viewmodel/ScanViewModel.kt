package com.invisiblepulse.rppg.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.invisiblepulse.rppg.data.AppDatabase
import com.invisiblepulse.rppg.data.ScanRecord
import com.invisiblepulse.rppg.utils.CalibrationPrefs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ScanViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).scanDao()
    val calibPrefs = CalibrationPrefs(application)

    val allScans: StateFlow<List<ScanRecord>> = dao.getAllScans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveScan(record: ScanRecord) {
        viewModelScope.launch { dao.insert(record) }
    }

    fun deleteScan(record: ScanRecord) {
        viewModelScope.launch { dao.delete(record) }
    }

    fun deleteAllScans() {
        viewModelScope.launch { dao.deleteAll() }
    }

    fun weeklyStats(scans: List<ScanRecord>): Triple<Double, Double, Double> {
        val cutoff = System.currentTimeMillis() - 7 * 24 * 3600 * 1000L
        val week = scans.filter { it.timestamp > cutoff }
        if (week.isEmpty()) return Triple(0.0, 0.0, 0.0)
        return Triple(week.map { it.bpm }.average(), week.maxOf { it.bpm }, week.minOf { it.bpm })
    }
}
