package com.invisiblepulse.rppg.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.invisiblepulse.rppg.BpmResult
import com.invisiblepulse.rppg.VitalPulseTheme
import com.invisiblepulse.rppg.data.ScanRecord
import com.invisiblepulse.rppg.utils.BPEstimator

@Composable
fun ResultsScreen(
    result: BpmResult,
    bpmOffset: Int,
    systolicOffset: Int,
    diastolicOffset: Int,
    onSave: (ScanRecord) -> Unit,
    onScanAgain: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bpm = (result.bpm + bpmOffset).coerceIn(30.0, 250.0)
    val (systolic, diastolic) = BPEstimator.estimate(bpm, result.hrv, systolicOffset, diastolicOffset)
    val status = BPEstimator.classify(bpm, systolic)
    val statusColor = VitalPulseTheme.statusColor(status)
    val statusContainerColor = VitalPulseTheme.statusContainerColor(status)
    var saveToHistory by remember { mutableStateOf(true) }
    var saved by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))

        // BPM Hero Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(VitalPulseTheme.SurfaceContainer, RoundedCornerShape(28.dp))
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("HASIL PEMINDAIAN", style = VitalPulseTheme.Typography.labelMedium, color = VitalPulseTheme.OnSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "%.0f".format(bpm),
                        fontSize = 80.sp,
                        fontWeight = FontWeight.Black,
                        color = VitalPulseTheme.OnSurface
                    )
                    Text(
                        " BPM",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = VitalPulseTheme.Primary.copy(alpha = 0.4f),
                        modifier = Modifier.padding(bottom = 14.dp)
                    )
                }
                // Status badge
                Box(
                    modifier = Modifier
                        .background(statusContainerColor, CircleShape)
                        .padding(horizontal = 18.dp, vertical = 7.dp)
                ) {
                    Text(BPEstimator.statusLabel(status), style = VitalPulseTheme.Typography.labelMedium, color = statusColor)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Metrics row
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                label = "Est. TENSI",
                value = "$systolic/$diastolic",
                unit = "mmHg",
                icon = Icons.Default.Favorite,
                modifier = Modifier.weight(1f),
                disclaimer = true
            )
            MetricCard(
                label = "VARIABILITAS",
                value = "%.0f".format(result.hrv),
                unit = "ms HRV",
                icon = Icons.Default.ShowChart,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                label = "KEPERCAYAAN",
                value = "%.0f".format(result.confidence * 100),
                unit = "%",
                icon = Icons.Default.BarChart,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label = "KATEGORI HR",
                value = BPEstimator.statusLabel(status),
                unit = "",
                icon = Icons.Default.MonitorHeart,
                valueColor = statusColor,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))

        // Recommendation card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(20.dp))
                .border(1.dp, VitalPulseTheme.Outline, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(VitalPulseTheme.PrimaryContainer, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, null, tint = VitalPulseTheme.Primary, modifier = Modifier.size(22.dp))
                }
                Column {
                    Text("Rekomendasi Kesehatan", style = VitalPulseTheme.Typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        BPEstimator.recommendation(status),
                        style = VitalPulseTheme.Typography.bodyMedium,
                        color = VitalPulseTheme.OnSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // BP disclaimer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(VitalPulseTheme.AmberContainer, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Info, null, tint = VitalPulseTheme.Amber, modifier = Modifier.size(18.dp).padding(top = 1.dp))
                Text(
                    "Estimasi tensi dihitung dari pola BPM + HRV — bukan pengganti tensimeter fisik. Gunakan fitur Kalibrasi di Settings untuk hasil lebih akurat.",
                    style = VitalPulseTheme.Typography.labelSmall,
                    color = Color(0xFF92400E),
                    lineHeight = 15.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Save toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(VitalPulseTheme.SurfaceContainer, RoundedCornerShape(16.dp))
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.History, null, tint = VitalPulseTheme.OnSurfaceVariant, modifier = Modifier.size(20.dp))
                Text("Simpan ke Riwayat", style = VitalPulseTheme.Typography.titleMedium)
            }
            Switch(
                checked = saveToHistory,
                onCheckedChange = { saveToHistory = it },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = VitalPulseTheme.Primary)
            )
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                if (saveToHistory && !saved) {
                    onSave(ScanRecord(
                        bpm = bpm,
                        bpSystolic = systolic,
                        bpDiastolic = diastolic,
                        hrv = result.hrv,
                        confidence = result.confidence,
                        status = status
                    ))
                    saved = true
                }
                onScanAgain()
            },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VitalPulseTheme.Primary)
        ) {
            Text("SELESAI", fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
fun MetricCard(
    label: String,
    value: String,
    unit: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    valueColor: Color = VitalPulseTheme.OnSurface,
    disclaimer: Boolean = false
) {
    Column(
        modifier = modifier
            .height(if (disclaimer) 120.dp else 110.dp)
            .background(Color.White, RoundedCornerShape(20.dp))
            .border(1.dp, VitalPulseTheme.Outline, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = VitalPulseTheme.Typography.labelSmall, color = VitalPulseTheme.OnSurfaceVariant)
            Icon(icon, null, tint = VitalPulseTheme.Primary, modifier = Modifier.size(16.dp))
        }
        Column {
            Text(value, fontSize = if (value.length > 6) 20.sp else 26.sp, fontWeight = FontWeight.Bold, color = valueColor)
            if (unit.isNotEmpty()) Text(unit, style = VitalPulseTheme.Typography.labelSmall, color = VitalPulseTheme.OnSurfaceVariant)
            if (disclaimer) Text("estimasi", fontSize = 9.sp, color = VitalPulseTheme.Amber, fontWeight = FontWeight.Bold)
        }
    }
}
