package com.invisiblepulse.rppg.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.invisiblepulse.rppg.VitalPulseTheme
import com.invisiblepulse.rppg.utils.CalibrationPrefs

@Composable
fun SettingsScreen(
    calibPrefs: CalibrationPrefs,
    totalScans: Int,
    onDeleteAllScans: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCalibrationSection by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var calibrationSaved by remember { mutableStateOf(false) }
    var showValidationError by remember { mutableStateOf(false) }

    // Calibration inputs
    var measuredBpm by remember { mutableStateOf("") }
    var actualBpm by remember { mutableStateOf("") }
    var measuredSystolic by remember { mutableStateOf("") }
    var actualSystolic by remember { mutableStateOf("") }
    var measuredDiastolic by remember { mutableStateOf("") }
    var actualDiastolic by remember { mutableStateOf("") }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus Semua Data?") },
            text = { Text("Seluruh riwayat scan ($totalScans entri) akan dihapus permanen.") },
            confirmButton = {
                TextButton(onClick = { onDeleteAllScans(); showDeleteDialog = false }) {
                    Text("Hapus", color = VitalPulseTheme.Secondary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Batal") }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Text("Pengaturan", style = VitalPulseTheme.Typography.headlineMedium)
        Text("Kelola preferensi & kalibrasi perangkat.", style = VitalPulseTheme.Typography.bodyMedium, color = VitalPulseTheme.OnSurfaceVariant)
        Spacer(Modifier.height(24.dp))

        // Calibration Section
        SectionCard(title = "Kalibrasi Perangkat", icon = Icons.Default.Tune) {
            Text(
                "Kalibrasi membantu menyesuaikan estimasi BPM & tensi dengan alat ukur fisik Anda (tensimeter/oksimeter).",
                style = VitalPulseTheme.Typography.bodyMedium,
                color = VitalPulseTheme.OnSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            // Current calibration status
            CalibStatusRow(calibPrefs)

            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { showCalibrationSection = !showCalibrationSection; calibrationSaved = false; showValidationError = false },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    if (showCalibrationSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null, modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(if (showCalibrationSection) "Tutup Kalibrasi" else "Buka Kalibrasi")
            }

            if (showCalibrationSection) {
                Spacer(Modifier.height(16.dp))
                CalibrationForm(
                    measuredBpm, actualBpm, measuredSystolic, actualSystolic, measuredDiastolic, actualDiastolic,
                    onMeasuredBpm = { measuredBpm = it },
                    onActualBpm = { actualBpm = it },
                    onMeasuredSystolic = { measuredSystolic = it },
                    onActualSystolic = { actualSystolic = it },
                    onMeasuredDiastolic = { measuredDiastolic = it },
                    onActualDiastolic = { actualDiastolic = it }
                )
                Spacer(Modifier.height(12.dp))

                if (calibrationSaved) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(VitalPulseTheme.TertiaryContainer, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CheckCircle, null, tint = VitalPulseTheme.Tertiary, modifier = Modifier.size(18.dp))
                            Text("Kalibrasi berhasil disimpan!", style = VitalPulseTheme.Typography.labelMedium, color = VitalPulseTheme.Tertiary)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                if (showValidationError) {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(VitalPulseTheme.AmberContainer, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Info, null, tint = VitalPulseTheme.Amber, modifier = Modifier.size(16.dp))
                        Text("Isi semua field sebelum menyimpan.", style = VitalPulseTheme.Typography.labelSmall, color = Color(0xFF92400E))
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Button(
                    onClick = {
                        val mBpm = measuredBpm.toDoubleOrNull()
                        val aBpm = actualBpm.toDoubleOrNull()
                        val mSys = measuredSystolic.toIntOrNull()
                        val aSys = actualSystolic.toIntOrNull()
                        val mDia = measuredDiastolic.toIntOrNull()
                        val aDia = actualDiastolic.toIntOrNull()
                        if (mBpm == null || aBpm == null || mSys == null || aSys == null || mDia == null || aDia == null) {
                            showValidationError = true
                            return@Button
                        }
                        showValidationError = false
                        calibPrefs.calibrate(mBpm, aBpm, mSys, aSys, mDia, aDia)
                        calibrationSaved = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VitalPulseTheme.Primary)
                ) {
                    Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("SIMPAN KALIBRASI")
                }

                if (calibPrefs.isCalibrated) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = { calibPrefs.reset(); calibrationSaved = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.RestartAlt, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Reset Kalibrasi", color = VitalPulseTheme.OnSurfaceVariant)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Data management
        SectionCard(title = "Manajemen Data", icon = Icons.Default.Storage) {
            SettingsRow(Icons.Default.History, "Total Scan Tersimpan", "$totalScans entri")
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = VitalPulseTheme.Secondary),
                border = BorderStroke(1.dp, VitalPulseTheme.Secondary.copy(0.3f))
            ) {
                Icon(Icons.Default.DeleteForever, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Hapus Semua Riwayat")
            }
        }

        Spacer(Modifier.height(16.dp))

        // Info
        SectionCard(title = "Tentang Aplikasi", icon = Icons.Default.Info) {
            SettingsRow(Icons.Default.MonitorHeart, "Vital Pulse", "v1.0 — rPPG Engine")
            Spacer(Modifier.height(8.dp))
            Text(
                "Aplikasi ini menggunakan teknologi remote Photoplethysmography (rPPG) untuk menganalisis detak jantung melalui kamera. Estimasi tensi bersifat informatif dan bukan pengganti alat medis.",
                style = VitalPulseTheme.Typography.bodyMedium,
                color = VitalPulseTheme.OnSurfaceVariant
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun SectionCard(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(20.dp))
            .border(1.dp, VitalPulseTheme.Outline, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(VitalPulseTheme.PrimaryContainer, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = VitalPulseTheme.Primary, modifier = Modifier.size(18.dp))
            }
            Text(title, style = VitalPulseTheme.Typography.titleMedium)
        }
        Spacer(Modifier.height(14.dp))
        content()
    }
}

@Composable
fun CalibStatusRow(prefs: CalibrationPrefs) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(VitalPulseTheme.SurfaceContainer, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Status Kalibrasi", fontWeight = FontWeight.Medium)
        if (prefs.isCalibrated) {
            Box(
                modifier = Modifier
                    .background(VitalPulseTheme.TertiaryContainer, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("TERKALIBRASI", style = VitalPulseTheme.Typography.labelSmall, color = VitalPulseTheme.Tertiary)
            }
        } else {
            Box(
                modifier = Modifier
                    .background(VitalPulseTheme.AmberContainer, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("BELUM DIKALIBRASI", style = VitalPulseTheme.Typography.labelSmall, color = VitalPulseTheme.Amber)
            }
        }
    }

    if (prefs.isCalibrated) {
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OffsetChip("BPM ${if (prefs.bpmOffset >= 0) "+" else ""}${prefs.bpmOffset}", Modifier.weight(1f))
            OffsetChip("Sys ${if (prefs.systolicOffset >= 0) "+" else ""}${prefs.systolicOffset}", Modifier.weight(1f))
            OffsetChip("Dia ${if (prefs.diastolicOffset >= 0) "+" else ""}${prefs.diastolicOffset}", Modifier.weight(1f))
        }
    }
}

@Composable
fun OffsetChip(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(VitalPulseTheme.SurfaceContainer, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = VitalPulseTheme.Typography.labelSmall, color = VitalPulseTheme.OnSurfaceVariant)
    }
}

@Composable
fun CalibrationForm(
    measuredBpm: String, actualBpm: String,
    measuredSystolic: String, actualSystolic: String,
    measuredDiastolic: String, actualDiastolic: String,
    onMeasuredBpm: (String) -> Unit, onActualBpm: (String) -> Unit,
    onMeasuredSystolic: (String) -> Unit, onActualSystolic: (String) -> Unit,
    onMeasuredDiastolic: (String) -> Unit, onActualDiastolic: (String) -> Unit
) {
    Text("Langkah Kalibrasi:", style = VitalPulseTheme.Typography.labelMedium, color = VitalPulseTheme.OnSurface)
    Spacer(Modifier.height(4.dp))
    Text(
        "1. Lakukan scan rPPG\n2. Ukur dengan oksimeter & tensimeter fisik\n3. Masukkan nilai perbandingan di bawah",
        style = VitalPulseTheme.Typography.bodyMedium,
        color = VitalPulseTheme.OnSurfaceVariant
    )
    Spacer(Modifier.height(14.dp))

    // BPM Calibration
    CalibRow(
        label = "BPM",
        measuredLabel = "BPM Hasil Scan",
        actualLabel = "BPM dari Oksimeter",
        measured = measuredBpm,
        actual = actualBpm,
        onMeasured = onMeasuredBpm,
        onActual = onActualBpm
    )
    Spacer(Modifier.height(10.dp))

    // Systolic
    CalibRow(
        label = "Tensi Sistolik",
        measuredLabel = "Estimasi Sistolik",
        actualLabel = "Aktual (tensimeter)",
        measured = measuredSystolic,
        actual = actualSystolic,
        onMeasured = onMeasuredSystolic,
        onActual = onActualSystolic
    )
    Spacer(Modifier.height(10.dp))

    // Diastolic
    CalibRow(
        label = "Tensi Diastolik",
        measuredLabel = "Estimasi Diastolik",
        actualLabel = "Aktual (tensimeter)",
        measured = measuredDiastolic,
        actual = actualDiastolic,
        onMeasured = onMeasuredDiastolic,
        onActual = onActualDiastolic
    )
}

@Composable
fun CalibRow(
    label: String,
    measuredLabel: String, actualLabel: String,
    measured: String, actual: String,
    onMeasured: (String) -> Unit, onActual: (String) -> Unit
) {
    Text(label, style = VitalPulseTheme.Typography.labelMedium, color = VitalPulseTheme.OnSurface)
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = measured,
            onValueChange = onMeasured,
            label = { Text(measuredLabel, fontSize = 11.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
        )
        OutlinedTextField(
            value = actual,
            onValueChange = onActual,
            label = { Text(actualLabel, fontSize = 11.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
fun SettingsRow(icon: ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = VitalPulseTheme.OnSurfaceVariant, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(subtitle, style = VitalPulseTheme.Typography.bodyMedium, color = VitalPulseTheme.OnSurfaceVariant)
        }
    }
}
