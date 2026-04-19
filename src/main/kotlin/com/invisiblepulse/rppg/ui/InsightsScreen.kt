package com.invisiblepulse.rppg.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.invisiblepulse.rppg.VitalPulseTheme
import com.invisiblepulse.rppg.data.ScanRecord
import com.invisiblepulse.rppg.utils.BPEstimator

@Composable
fun InsightsScreen(
    scans: List<ScanRecord>,
    modifier: Modifier = Modifier
) {
    val cutoff7d = System.currentTimeMillis() - 7 * 24 * 3600 * 1000L
    val cutoff30d = System.currentTimeMillis() - 30 * 24 * 3600 * 1000L
    val week = scans.filter { it.timestamp > cutoff7d }
    val month = scans.filter { it.timestamp > cutoff30d }

    val avgBpm = if (week.isNotEmpty()) week.map { it.bpm }.average() else 0.0
    val avgHrv = if (week.isNotEmpty()) week.map { it.hrv }.average() else 0.0
    val dominantStatus = week.groupBy { it.status }.maxByOrNull { it.value.size }?.key ?: "NORMAL"
    val normalCount = week.count { it.status == "NORMAL" }
    val healthScore = if (week.isEmpty()) 0 else ((normalCount.toDouble() / week.size) * 100).toInt()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(24.dp))

        Text("CLINICAL SUMMARY", style = VitalPulseTheme.Typography.labelMedium, color = VitalPulseTheme.Primary)
        Spacer(Modifier.height(4.dp))
        Text("Insights Kesehatan", style = VitalPulseTheme.Typography.headlineMedium)
        Spacer(Modifier.height(20.dp))

        if (scans.isEmpty()) {
            EmptyInsightsCard()
        } else {
            // Health Score Hero
            HealthScoreCard(healthScore, dominantStatus, week.size)
            Spacer(Modifier.height(16.dp))

            // Weekly BPM chart
            if (week.isNotEmpty()) {
                BpmTrendCard(week)
                Spacer(Modifier.height(16.dp))
            }

            // Daily averages grid
            DailyAveragesSection(avgBpm, avgHrv, month)
            Spacer(Modifier.height(16.dp))

            // Recommendation based on dominant status
            InsightRecommendationCard(dominantStatus, healthScore)
            Spacer(Modifier.height(16.dp))

            // Health tips
            HealthTipsSection()
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun EmptyInsightsCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(VitalPulseTheme.SurfaceContainer, RoundedCornerShape(24.dp))
            .padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Analytics, null, tint = VitalPulseTheme.OutlineVariant, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(12.dp))
            Text("Belum ada data insight", fontWeight = FontWeight.Bold, color = VitalPulseTheme.OnSurfaceVariant)
            Text("Lakukan minimal 1 scan untuk melihat insights.", style = VitalPulseTheme.Typography.bodyMedium, color = VitalPulseTheme.OnSurfaceVariant)
        }
    }
}

@Composable
fun HealthScoreCard(score: Int, status: String, totalScans: Int) {
    val scoreColor = when {
        score >= 80 -> VitalPulseTheme.Tertiary
        score >= 60 -> VitalPulseTheme.Primary
        score >= 40 -> VitalPulseTheme.Amber
        else        -> VitalPulseTheme.Secondary
    }
    val grade = when {
        score >= 80 -> "A"
        score >= 60 -> "B"
        score >= 40 -> "C"
        else        -> "D"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(VitalPulseTheme.OnSurface, RoundedCornerShape(28.dp))
            .padding(28.dp)
    ) {
        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    Text("KESEHATAN KARDIO KESELURUHAN", style = VitalPulseTheme.Typography.labelSmall, color = Color.White.copy(0.6f))
                    Spacer(Modifier.height(8.dp))
                    Text("Skor Mingguan", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(4.dp))
                    Text("Berdasarkan $totalScans scan terakhir", style = VitalPulseTheme.Typography.bodyMedium, color = Color.White.copy(0.6f))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$score", fontSize = 52.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Text("%", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(0.6f), modifier = Modifier.padding(bottom = 8.dp))
                    }
                    Box(
                        modifier = Modifier
                            .background(scoreColor.copy(0.25f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("GRADE $grade", style = VitalPulseTheme.Typography.labelSmall, color = scoreColor)
                    }
                }
            }
        }
    }
}

@Composable
fun BpmTrendCard(scans: List<ScanRecord>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(24.dp))
            .border(1.dp, VitalPulseTheme.Outline, RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Tren BPM (7 Hari)", style = VitalPulseTheme.Typography.titleMedium)
                Text("${scans.size} SCAN", style = VitalPulseTheme.Typography.labelSmall, color = VitalPulseTheme.OnSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))

            // Simple line chart
            val bpmValues = scans.takeLast(20).map { it.bpm.toFloat() }
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth().height(90.dp)) {
                if (bpmValues.size < 2) return@Canvas
                val max = bpmValues.max()
                val min = bpmValues.min()
                val range = (max - min).coerceAtLeast(1f)
                val path = Path()
                val step = size.width / (bpmValues.size - 1).coerceAtLeast(1)
                bpmValues.forEachIndexed { i, v ->
                    val x = i * step
                    val y = size.height - ((v - min) / range) * size.height * 0.85f - size.height * 0.05f
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, VitalPulseTheme.Primary, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
                // dots
                bpmValues.forEachIndexed { i, v ->
                    val x = i * step
                    val y = size.height - ((v - min) / range) * size.height * 0.85f - size.height * 0.05f
                    drawCircle(VitalPulseTheme.Primary, 4.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, y))
                }
            }
        }
    }
}

@Composable
fun DailyAveragesSection(avgBpm: Double, avgHrv: Double, monthScans: List<ScanRecord>) {
    val restingBpm = monthScans.filter { it.status == "NORMAL" || it.status == "LOW" }
        .map { it.bpm }.average().takeIf { !it.isNaN() } ?: 0.0
    val peakBpm = monthScans.maxOfOrNull { it.bpm } ?: 0.0

    Text("Rata-rata Harian", style = VitalPulseTheme.Typography.headlineSmall)
    Spacer(Modifier.height(12.dp))

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AverageRow(Icons.Default.Bedtime, "HR Istirahat", "%.0f BPM".format(restingBpm), VitalPulseTheme.Tertiary, "Stabil")
        AverageRow(Icons.Default.MonitorHeart, "HR Rata-rata", "%.0f BPM".format(avgBpm), VitalPulseTheme.Primary, "Mingguan")
        AverageRow(Icons.Default.FitnessCenter, "HR Puncak", "%.0f BPM".format(peakBpm), VitalPulseTheme.Secondary, "30 hari")
        AverageRow(Icons.Default.Timeline, "HRV Rata-rata", "%.0f ms".format(avgHrv), VitalPulseTheme.Amber, "Mingguan")
    }
}

@Composable
fun AverageRow(icon: ImageVector, label: String, value: String, color: Color, badge: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, VitalPulseTheme.Outline, RoundedCornerShape(16.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(label, style = VitalPulseTheme.Typography.labelSmall, color = VitalPulseTheme.OnSurfaceVariant)
                Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
        Box(
            modifier = Modifier
                .background(VitalPulseTheme.SurfaceContainer, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(badge, style = VitalPulseTheme.Typography.labelSmall, color = VitalPulseTheme.OnSurfaceVariant)
        }
    }
}

@Composable
fun InsightRecommendationCard(status: String, score: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(VitalPulseTheme.PrimaryContainer, RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(VitalPulseTheme.Primary, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Column {
                Text("Insight Kardio", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = VitalPulseTheme.OnSurface)
                Spacer(Modifier.height(6.dp))
                Text(
                    BPEstimator.recommendation(status),
                    style = VitalPulseTheme.Typography.bodyMedium,
                    color = VitalPulseTheme.OnSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun HealthTipsSection() {
    Text("Tips Kesehatan", style = VitalPulseTheme.Typography.headlineSmall)
    Spacer(Modifier.height(12.dp))

    val tips = listOf(
        Icons.Default.Analytics to "Memahami HRV" to "HRV tinggi umumnya menandakan kondisi kardiovaskular yang baik dan ketahanan terhadap stres.",
        Icons.Default.Air to "Saturasi Oksigen" to "Pertahankan SpO2 antara 95%–100%. Level konsisten di bawah 94% memerlukan konsultasi medis.",
        Icons.Default.Bedtime to "Tidur & Detak Jantung" to "HR istirahat yang rendah (50–60 BPM) seringkali tanda kebugaran jantung yang baik.",
        Icons.Default.DirectionsRun to "Olahraga Teratur" to "30 menit aktivitas aerobik per hari dapat menurunkan HR istirahat rata-rata 5–10 BPM dalam 4 minggu."
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        tips.forEach { (iconLabel, desc) ->
            val (icon, title) = iconLabel
            TipCard(icon, title, desc)
        }
    }
}

@Composable
fun TipCard(icon: ImageVector, title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(18.dp))
            .border(1.dp, VitalPulseTheme.Outline, RoundedCornerShape(18.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(VitalPulseTheme.SurfaceContainer, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = VitalPulseTheme.Primary, modifier = Modifier.size(20.dp))
        }
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
            Text(desc, style = VitalPulseTheme.Typography.bodyMedium, color = VitalPulseTheme.OnSurfaceVariant)
        }
    }
}
