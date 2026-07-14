package com.invisiblepulse.rppg

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.invisiblepulse.rppg.ui.*
import com.invisiblepulse.rppg.viewmodel.ScanViewModel
import kotlinx.coroutines.delay

enum class AppTab { VITALS, HISTORY, INSIGHTS, SETTINGS }

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
        val viewModel = ViewModelProvider(this)[ScanViewModel::class.java]
        setContent { VitalPulseApp(viewModel) }
    }
}

@Composable
fun VitalPulseApp(viewModel: ScanViewModel) {
    val scans by viewModel.allScans.collectAsState()

    var currentTab by remember { mutableStateOf(AppTab.VITALS) }
    var bpmResult by remember { mutableStateOf(BpmResult(0.0, 0.0, 0.0, MeasurementStatus.SEARCHING)) }
    val latestBpmResult by rememberUpdatedState(bpmResult)
    var completedResult by remember { mutableStateOf<BpmResult?>(null) }
    val signalHistory = remember { mutableStateListOf<Float>() }
    var scanTimer by remember { mutableIntStateOf(15) }
    var isShowingResults by remember { mutableStateOf(false) }

    // Accumulate live signal for waveform
    LaunchedEffect(bpmResult.signalValue) {
        if (bpmResult.status != MeasurementStatus.SEARCHING) {
            signalHistory.add(bpmResult.signalValue.toFloat())
            if (signalHistory.size > 50) signalHistory.removeAt(0)
        } else {
            signalHistory.clear()
        }
    }

    // Countdown timer when measuring
    LaunchedEffect(bpmResult.status) {
        if (bpmResult.status == MeasurementStatus.MEASURING) {
            scanTimer = 15
            while (scanTimer > 0 && bpmResult.status == MeasurementStatus.MEASURING) {
                delay(1000L)
                scanTimer--
            }
            if (scanTimer == 0) {
                completedResult = latestBpmResult
                isShowingResults = true
            }
        } else if (bpmResult.status == MeasurementStatus.SEARCHING) {
            scanTimer = 15
            isShowingResults = false
        }
    }

    val weeklyStats = remember(scans) { viewModel.weeklyStats(scans) }

    Scaffold(
        topBar = { VitalPulseTopBar(currentTab) },
        bottomBar = { VitalPulseBottomBar(currentTab) { currentTab = it } },
        containerColor = VitalPulseTheme.Background
    ) { padding ->

        AnimatedContent(
            targetState = currentTab,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
            label = "tab_transition",
            modifier = Modifier.padding(padding)
        ) { tab ->
            when (tab) {
                AppTab.VITALS -> {
                    if (isShowingResults && completedResult != null) {
                        ResultsScreen(
                            result = completedResult!!,
                            bpmOffset = viewModel.calibPrefs.bpmOffset,
                            systolicOffset = viewModel.calibPrefs.systolicOffset,
                            diastolicOffset = viewModel.calibPrefs.diastolicOffset,
                            onSave = { record -> viewModel.saveScan(record) },
                            onScanAgain = {
                                isShowingResults = false
                                scanTimer = 15
                                completedResult = null
                            }
                        )
                    } else {
                        VitalsScreen(
                            result = bpmResult,
                            signalHistory = signalHistory,
                            timer = scanTimer,
                            onResult = { bpmResult = it }
                        )
                    }
                }
                AppTab.HISTORY -> HistoryScreen(
                    scans = scans,
                    weeklyStats = weeklyStats,
                    onDelete = { viewModel.deleteScan(it) },
                    onDeleteAll = { viewModel.deleteAllScans() }
                )
                AppTab.INSIGHTS -> InsightsScreen(scans = scans)
                AppTab.SETTINGS -> SettingsScreen(
                    calibPrefs = viewModel.calibPrefs,
                    totalScans = scans.size,
                    onDeleteAllScans = { viewModel.deleteAllScans() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitalPulseTopBar(tab: AppTab) {
    val title = when (tab) {
        AppTab.VITALS   -> "Vital Pulse"
        AppTab.HISTORY  -> "Vital Pulse"
        AppTab.INSIGHTS -> "Vital Pulse"
        AppTab.SETTINGS -> "Vital Pulse"
    }
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.MonitorHeart, null, tint = VitalPulseTheme.Primary)
                Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = VitalPulseTheme.Background)
    )
}

@Composable
fun VitalPulseBottomBar(current: AppTab, onSelect: (AppTab) -> Unit) {
    NavigationBar(
        containerColor = Color.White.copy(alpha = 0.95f),
        tonalElevation = 0.dp,
        modifier = Modifier
            .background(Color.White.copy(0.95f))
    ) {
        val items = listOf(
            AppTab.VITALS   to (Icons.Default.Favorite    to "Vitals"),
            AppTab.HISTORY  to (Icons.Default.History     to "Riwayat"),
            AppTab.INSIGHTS to (Icons.Default.Analytics   to "Insights"),
            AppTab.SETTINGS to (Icons.Default.Settings    to "Settings")
        )
        items.forEach { (tab, pair) ->
            val (icon, label) = pair
            NavigationBarItem(
                selected = current == tab,
                onClick = { onSelect(tab) },
                icon = { Icon(icon, null) },
                label = { Text(label, style = VitalPulseTheme.Typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = VitalPulseTheme.Primary,
                    selectedTextColor = VitalPulseTheme.Primary,
                    indicatorColor = VitalPulseTheme.PrimaryContainer,
                    unselectedIconColor = VitalPulseTheme.OnSurfaceVariant,
                    unselectedTextColor = VitalPulseTheme.OnSurfaceVariant
                )
            )
        }
    }
}
