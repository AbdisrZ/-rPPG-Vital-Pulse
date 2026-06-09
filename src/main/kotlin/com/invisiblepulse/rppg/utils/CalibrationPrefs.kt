package com.invisiblepulse.rppg.utils

import android.content.Context

class CalibrationPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("calibration_prefs", Context.MODE_PRIVATE)

    var bpmOffset: Int
        get() = prefs.getInt("bpm_offset", 0)
        set(v) = prefs.edit().putInt("bpm_offset", v).apply()

    var systolicOffset: Int
        get() = prefs.getInt("systolic_offset", 0)
        set(v) = prefs.edit().putInt("systolic_offset", v).apply()

    var diastolicOffset: Int
        get() = prefs.getInt("diastolic_offset", 0)
        set(v) = prefs.edit().putInt("diastolic_offset", v).apply()

    var isCalibrated: Boolean
        get() = prefs.getBoolean("is_calibrated", false)
        set(v) = prefs.edit().putBoolean("is_calibrated", v).apply()

    fun calibrate(
        measuredBpm: Double,
        actualBpm: Double,
        measuredSystolic: Int,
        actualSystolic: Int,
        measuredDiastolic: Int,
        actualDiastolic: Int
    ) {
        bpmOffset = (actualBpm - measuredBpm).toInt().coerceIn(-30, 30)
        systolicOffset = (actualSystolic - measuredSystolic).coerceIn(-40, 40)
        diastolicOffset = (actualDiastolic - measuredDiastolic).coerceIn(-30, 30)
        isCalibrated = true
    }

    fun reset() {
        prefs.edit()
            .remove("bpm_offset")
            .remove("systolic_offset")
            .remove("diastolic_offset")
            .remove("is_calibrated")
            .apply()
    }
}
