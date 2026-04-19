package com.invisiblepulse.rppg.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.invisiblepulse.rppg.VitalPulseTheme
import com.invisiblepulse.rppg.data.ScanRecord
import com.invisiblepulse.rppg.utils.BPEstimator
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    scans: List<ScanRecord>,
    weeklyStats: Triple<Double, Double, Double>,
    onDelete: (ScanRecord) -> Unit,
    onDeleteAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Hapus Semua Riwayat?") },
            text = { Text("Seluruh data scan akan dihapus permanen.") },
            confirmButton = {
                TextButton(onClick = { onDeleteAll(); showDeleteAllDialog = false }) {
                    Text("Hapus", color = VitalPulseTheme.Secondary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) { Text("Batal") }
            }
        )
    }

    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)) {
        item {
            // Header
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Text("WEEKLY PERFORMANCE", style = VitalPulseTheme.Typography.labelMedium, color = VitalPulseTheme.Primary)
                    Spacer(Modifier.height(4.dp))
                    Text("Riwayat Detak Jantung", style = VitalPulseTheme.Typography.headlineMedium)
                }
                if (scans.isNotEmpty()) {
                    IconButton(onClick = { showDeleteAllDialog = true }) {
                        Icon(Icons.Default.DeleteSweep, null, tint = VitalPulseTheme.OnSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // Weekly stats
        if (weeklyStats.first > 0) {
            item {
                WeeklyStatsCard(weeklyStats)
                Spacer(Modifier.height(20.dp))
            }
        }

        if (scans.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.History, null, tint = VitalPulseTheme.OutlineVariant, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Belum ada riwayat scan", color = VitalPulseTheme.OnSurfaceVariant, fontWeight = FontWeight.Medium)
                        Text("Lakukan scan pertama Anda!", style = VitalPulseTheme.Typography.labelMedium, color = VitalPulseTheme.OnSurfaceVariant)
                    }
                }
            }
        } else {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Scan Terbaru", style = VitalPulseTheme.Typography.headlineSmall)
                    Text("${scans.size} ENTRI", style = VitalPulseTheme.Typography.labelSmall, color = VitalPulseTheme.OnSurfaceVariant)
                }
                Spacer(Modifier.height(12.dp))
            }
            items(scans, key = { it.id }) { scan ->
                ScanHistoryItem(scan = scan, onDelete = { onDelete(scan) })
                Spacer(Modifier.height(10.dp))
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
fun WeeklyStatsCard(stats: Triple<Double, Double, Double>) {
    val (avg, max, min) = stats
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(VitalPulseTheme.SurfaceContainer, RoundedCornerShape(24.dp))
            .padding(24.dp)
    ) {
        Column {
            Text("RATA-RATA 7 HARI", style = VitalPulseTheme.Typography.labelSmall, color = VitalPulseTheme.OnSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("%.0f".format(avg), fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = VitalPulseTheme.Primary)
                Text("  BPM", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = VitalPulseTheme.OnSurfaceVariant, modifier = Modifier.padding(bottom = 10.dp))
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatChip(label = "Tertinggi", value = "%.0f".format(max), color = VitalPulseTheme.Secondary, modifier = Modifier.weight(1f))
                StatChip(label = "Terendah", value = "%.0f".format(min), color = VitalPulseTheme.Tertiary, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun StatChip(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, VitalPulseTheme.Outline, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Text(label.uppercase(), style = VitalPulseTheme.Typography.labelSmall, color = VitalPulseTheme.OnSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text("$value BPM", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun ScanHistoryItem(scan: ScanRecord, onDelete: () -> Unit) {
    val statusColor = VitalPulseTheme.statusColor(scan.status)
    val statusContainerColor = VitalPulseTheme.statusContainerColor(scan.status)
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(18.dp))
            .border(1.dp, VitalPulseTheme.Outline, RoundedCornerShape(18.dp))
            .clickable { expanded = !expanded }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(statusContainerColor, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Favorite, null, tint = statusColor, modifier = Modifier.size(20.dp))
                }
                Column {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("%.0f".format(scan.bpm), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                        Text("BPM", style = VitalPulseTheme.Typography.labelSmall, color = VitalPulseTheme.OnSurfaceVariant, modifier = Modifier.padding(bottom = 3.dp))
                    }
                    Text(formatTimestamp(scan.timestamp), style = VitalPulseTheme.Typography.labelSmall, color = VitalPulseTheme.OnSurfaceVariant)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .background(statusContainerColor, CircleShape)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(BPEstimator.statusLabel(scan.status), style = VitalPulseTheme.Typography.labelSmall, color = statusColor)
                }
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    null, tint = VitalPulseTheme.OnSurfaceVariant, modifier = Modifier.size(20.dp)
                )
            }
        }

        if (expanded) {
            HorizontalDivider(color = VitalPulseTheme.Outline, thickness = 0.5.dp)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DetailItem("Est. Tensi", "${scan.bpSystolic}/${scan.bpDiastolic} mmHg", Modifier.weight(1f))
                DetailItem("HRV", "%.0f ms".format(scan.hrv), Modifier.weight(1f))
                DetailItem("Akurasi", "%.0f%%".format(scan.confidence * 100), Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp), tint = VitalPulseTheme.Secondary)
                    Spacer(Modifier.width(4.dp))
                    Text("Hapus", color = VitalPulseTheme.Secondary, style = VitalPulseTheme.Typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = VitalPulseTheme.Typography.labelSmall, color = VitalPulseTheme.OnSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

private fun formatTimestamp(ts: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - ts
    return when {
        diff < 60_000 -> "Baru saja"
        diff < 3_600_000 -> "${diff / 60_000} menit lalu"
        diff < 86_400_000 -> "Hari ini • ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))}"
        diff < 172_800_000 -> "Kemarin • ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))}"
        else -> SimpleDateFormat("dd MMM • HH:mm", Locale.getDefault()).format(Date(ts))
    }
}
