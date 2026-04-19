package com.invisiblepulse.rppg.utils

object BPEstimator {

    /**
     * Rough estimation model: BPM + HRV → estimated BP.
     * NOT medically validated. Requires calibration against a real device for meaningful results.
     * Formula based on simplified cardiac output relationship.
     */
    fun estimate(
        bpm: Double,
        hrv: Double = 0.0,
        systolicOffset: Int = 0,
        diastolicOffset: Int = 0
    ): Pair<Int, Int> {
        val bpmDelta = bpm - 70.0
        val hrvFactor = if (hrv > 0) (-0.05 * (hrv - 50.0)).coerceIn(-5.0, 5.0) else 0.0

        val systolic = (120.0 + 0.5 * bpmDelta + hrvFactor + systolicOffset)
            .toInt().coerceIn(60, 220)
        val diastolic = (80.0 + 0.3 * bpmDelta + (hrvFactor * 0.6) + diastolicOffset)
            .toInt().coerceIn(40, 140)
        return systolic to diastolic
    }

    fun classify(bpm: Double, systolic: Int): String = when {
        bpm < 50 || systolic < 90           -> "LOW"
        bpm <= 100 && systolic in 90..120   -> "NORMAL"
        bpm <= 120 || systolic in 121..140  -> "ELEVATED"
        else                                 -> "HIGH"
    }

    fun recommendation(status: String): String = when (status) {
        "LOW"      -> "Detak jantung di bawah normal (Bradikardia). Pastikan hidrasi cukup dan istirahat. Konsultasi dokter jika berlanjut lebih dari 1 jam."
        "NORMAL"   -> "Bacaan kardiovaskular dalam rentang sehat. Pertahankan pola hidup aktif dan tidur cukup."
        "ELEVATED" -> "Bacaan sedikit meningkat. Kurangi kafein, hindari stres, dan ukur ulang setelah 10 menit istirahat."
        "HIGH"     -> "Bacaan tinggi (Takikardia/Hipertensi estimasi). Segera istirahat dan hindari aktivitas berat. Konsultasi tenaga medis profesional."
        else       -> "Terus pantau tanda vital Anda secara teratur."
    }

    fun statusLabel(status: String): String = when (status) {
        "LOW"      -> "RENDAH"
        "NORMAL"   -> "NORMAL"
        "ELEVATED" -> "MENINGKAT"
        "HIGH"     -> "TINGGI"
        else       -> status
    }
}
