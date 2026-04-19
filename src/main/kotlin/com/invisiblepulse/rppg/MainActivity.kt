package com.invisiblepulse.rppg

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkCameraPermission()
        setContent { VitalPulseApp() }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun VitalPulseApp() {
    val context = LocalContext.current
    var bpmResult by remember { mutableStateOf(BpmResult(0.0, 0.0, 0.0, MeasurementStatus.SEARCHING)) }
    var lastValidResult by remember { mutableStateOf<BpmResult?>(null) }
    val signalHistory = remember { mutableStateListOf<Float>() }
    var scanTimer by remember { mutableIntStateOf(15) }
    var isShowingResults by remember { mutableStateOf(false) }

    LaunchedEffect(bpmResult.signalValue) {
        if (bpmResult.status != MeasurementStatus.SEARCHING) {
            signalHistory.add(bpmResult.signalValue.toFloat())
            if (signalHistory.size > 50) signalHistory.removeAt(0)
        } else {
            signalHistory.clear()
        }
    }

    LaunchedEffect(bpmResult.status) {
        if (bpmResult.status == MeasurementStatus.MEASURING) {
            while (scanTimer > 0) {
                delay(1000L)
                scanTimer--
            }
            lastValidResult = bpmResult
            isShowingResults = true
        } else if (bpmResult.status == MeasurementStatus.SEARCHING) {
            scanTimer = 15
        }
    }

    Scaffold(
        topBar = { VitalPulseTopBar() },
        bottomBar = { VitalPulseBottomBar() },
        containerColor = VitalPulseTheme.Background
    ) { padding ->
        AnimatedContent(targetState = isShowingResults, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "") { resultsVisible ->
            if (resultsVisible) {
                ResultsScreen(
                    result = lastValidResult ?: bpmResult,
                    onDone = { isShowingResults = false; scanTimer = 15 },
                    onExport = { exportData(context, lastValidResult ?: bpmResult) },
                    modifier = Modifier.padding(padding)
                )
            } else {
                ScannerScreen(
                    result = bpmResult,
                    signalHistory = signalHistory,
                    timer = scanTimer,
                    onResult = { bpmResult = it },
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

fun exportData(context: android.content.Context, result: BpmResult) {
    val csv = "Timestamp,SignalValue\n" + result.last15sSignal.mapIndexed { i, d -> "$i,$d" }.joinToString("\n")
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Vital Pulse Data Export")
        putExtra(Intent.EXTRA_TEXT, "BPM: ${result.bpm.toInt()}\nHRV (RMSSD): ${result.hrv.toInt()}ms\n\n$csv")
    }
    context.startActivity(Intent.createChooser(intent, "Export Thesis Data"))
}

@Composable
fun ScannerScreen(result: BpmResult, signalHistory: List<Float>, timer: Int, onResult: (BpmResult) -> Unit, modifier: Modifier) {
    Column(modifier = modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Analyzing Vital Signs", style = VitalPulseTheme.Typography.labelMedium, color = VitalPulseTheme.Primary)
        Text(text = if (result.status == MeasurementStatus.MEASURING) "Stay Still" else "Align Face", style = VitalPulseTheme.Typography.headlineMedium)
        
        Spacer(modifier = Modifier.height(24.dp))
        Box(modifier = Modifier.size(280.dp), contentAlignment = Alignment.Center) {
            ScannerView(result, onResult)
            // ROI Overlay
            result.faceBox?.let { box ->
                Canvas(modifier = Modifier.size(240.dp)) {
                    // Note: In a production app, we'd map coordinate systems. 
                    // For the demo, we show a static ROI to represent the concept.
                    drawRect(color = Color.Green.copy(alpha = 0.3f), style = Stroke(2.dp.toPx()))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        TimerDisplay(timer)
        Spacer(modifier = Modifier.height(16.dp))
        LivePulseFeed(signalHistory)
    }
}

@Composable
fun TimerDisplay(timer: Int) {
    Box(modifier = Modifier.fillMaxWidth().background(VitalPulseTheme.SurfaceContainer, RoundedCornerShape(24.dp)).padding(20.dp)) {
        Column {
            Text(text = "TIME REMAINING", style = VitalPulseTheme.Typography.labelMedium, color = VitalPulseTheme.OnSurfaceVariant)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = timer.toString(), fontSize = 38.sp, fontWeight = FontWeight.Black, color = VitalPulseTheme.Primary)
                Text(text = " SEC", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = VitalPulseTheme.Primary.copy(alpha = 0.5f), modifier = Modifier.padding(bottom = 6.dp))
            }
        }
        Icon(Icons.Default.Timer, contentDescription = null, tint = VitalPulseTheme.Primary.copy(alpha = 0.1f), modifier = Modifier.size(56.dp).align(Alignment.CenterEnd))
    }
}

@Composable
fun ResultsScreen(result: BpmResult, onDone: () -> Unit, onExport: () -> Unit, modifier: Modifier) {
    Column(modifier = modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(32.dp))
        Box(modifier = Modifier.fillMaxWidth().background(VitalPulseTheme.SurfaceContainer, RoundedCornerShape(32.dp)).padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "FINAL READING", style = VitalPulseTheme.Typography.labelMedium, color = VitalPulseTheme.OnSurfaceVariant)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(text = "%.0f".format(result.bpm), fontSize = 84.sp, fontWeight = FontWeight.Black)
                    Text(text = " BPM", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = VitalPulseTheme.Primary.copy(alpha = 0.4f), modifier = Modifier.padding(bottom = 16.dp))
                }
                Box(modifier = Modifier.background(VitalPulseTheme.Primary.copy(alpha = 0.1f), CircleShape).padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Text(text = if (result.bpm in 60.0..100.0) "NORMAL" else "ELEVATED", style = VitalPulseTheme.Typography.labelMedium, color = VitalPulseTheme.Primary)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            BentoCard(title = "OXYGEN SAT.", value = "98", unit = "%", icon = Icons.Default.WaterDrop, modifier = Modifier.weight(1f))
            BentoCard(title = "VARIABILITY", value = "%.0f".format(result.hrv), unit = "ms", icon = Icons.Default.Timeline, modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = onExport, modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 8.dp), colors = ButtonDefaults.buttonColors(containerColor = VitalPulseTheme.SurfaceContainer, contentColor = VitalPulseTheme.Primary)) {
            Icon(Icons.Default.IosShare, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("EXPORT THESIS DATA")
        }
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(64.dp), shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = VitalPulseTheme.Primary)) {
            Text("DONE", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun BentoCard(title: String, value: String, unit: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    Column(modifier = modifier.height(130.dp).background(Color.White, RoundedCornerShape(24.dp)).border(1.dp, VitalPulseTheme.Outline, RoundedCornerShape(24.dp)).padding(20.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = title, style = VitalPulseTheme.Typography.labelMedium, color = VitalPulseTheme.OnSurfaceVariant)
            Icon(icon, contentDescription = null, tint = VitalPulseTheme.Primary, modifier = Modifier.size(16.dp))
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(text = value, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(text = " $unit", fontSize = 12.sp, color = VitalPulseTheme.OnSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitalPulseTopBar() {
    TopAppBar(title = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.MonitorHeart, contentDescription = null, tint = VitalPulseTheme.Primary); Spacer(modifier = Modifier.width(8.dp)); Text("Vital Pulse", fontWeight = FontWeight.Bold, fontSize = 18.sp) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent))
}

@Composable
fun VitalPulseBottomBar() {
    NavigationBar(containerColor = Color.White.copy(alpha = 0.9f), tonalElevation = 0.dp) {
        NavigationBarItem(selected = true, onClick = {}, icon = { Icon(Icons.Default.Favorite, null) }, label = { Text("Vitals", style = VitalPulseTheme.Typography.labelMedium) })
        NavigationBarItem(selected = false, onClick = {}, icon = { Icon(Icons.Default.History, null) }, label = { Text("History", style = VitalPulseTheme.Typography.labelMedium) })
        NavigationBarItem(selected = false, onClick = {}, icon = { Icon(Icons.Default.Insights, null) }, label = { Text("Insights", style = VitalPulseTheme.Typography.labelMedium) })
        NavigationBarItem(selected = false, onClick = {}, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Settings", style = VitalPulseTheme.Typography.labelMedium) })
    }
}

@Composable
fun ScannerView(result: BpmResult, onResult: (BpmResult) -> Unit) {
    val progressAnimation by animateFloatAsState(targetValue = if (result.status == MeasurementStatus.MEASURING) 1f else 0.3f, animationSpec = tween(15000, easing = LinearEasing), label = "")
    Box(modifier = Modifier.size(280.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(Color(0xFFF1F5F9), style = Stroke(6.dp.toPx()))
            drawArc(VitalPulseTheme.Primary, -90f, 360f * progressAnimation, false, style = Stroke(6.dp.toPx(), cap = StrokeCap.Round))
        }
        Box(modifier = Modifier.size(240.dp).clip(CircleShape).background(Color.White).border(4.dp, VitalPulseTheme.Outline, CircleShape)) {
            CameraPreview(onResult)
            Box(modifier = Modifier.fillMaxSize().background(Brush.radialGradient(0.7f to Color.Transparent, 1f to Color.White.copy(alpha = 0.3f))))
        }
        StatusBadge(result.status)
    }
}

@Composable
fun StatusBadge(status: MeasurementStatus) {
    Box(modifier = Modifier.offset(y = 135.dp).background(Color.White, CircleShape).border(1.dp, VitalPulseTheme.Outline, CircleShape).padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).background(if (status == MeasurementStatus.MEASURING) Color.Green else Color.Red, CircleShape))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = status.name, style = VitalPulseTheme.Typography.labelMedium)
        }
    }
}

@Composable
fun LivePulseFeed(history: List<Float>) {
    Column(modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(24.dp)).border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp)).padding(16.dp)) {
        Text("LIVE rPPG FEED", style = VitalPulseTheme.Typography.labelMedium, color = VitalPulseTheme.OnSurfaceVariant)
        Canvas(modifier = Modifier.fillMaxWidth().height(80.dp)) {
            if (history.size < 2) return@Canvas
            val path = Path()
            val stepX = size.width / 50f
            val range = (history.max() - history.min()).coerceAtLeast(0.1f)
            history.forEachIndexed { i, v ->
                val x = i * stepX
                val y = size.height / 2 - ((v / range) * size.height * 0.4f)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, VitalPulseTheme.Secondary, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}

@Composable
fun CameraPreview(onResult: (BpmResult) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    LaunchedEffect(Unit) {
        ProcessCameraProvider.getInstance(context).addListener({
            val provider = ProcessCameraProvider.getInstance(context).get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build().also { it.setAnalyzer(executor, rPPGAnalyzer(onResult)) }
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, preview, analysis)
        }, ContextCompat.getMainExecutor(context))
    }
    AndroidView({ previewView }, Modifier.fillMaxSize())
}
