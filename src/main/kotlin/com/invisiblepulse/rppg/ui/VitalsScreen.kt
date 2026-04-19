package com.invisiblepulse.rppg.ui

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.invisiblepulse.rppg.BpmResult
import com.invisiblepulse.rppg.MeasurementStatus
import com.invisiblepulse.rppg.VitalPulseTheme
import com.invisiblepulse.rppg.rPPGAnalyzer
import java.util.concurrent.Executors

@Composable
fun VitalsScreen(
    result: BpmResult,
    signalHistory: List<Float>,
    timer: Int,
    onResult: (BpmResult) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))

        Text(
            "ANALYZING VITAL SIGNS",
            style = VitalPulseTheme.Typography.labelMedium,
            color = VitalPulseTheme.Primary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = when (result.status) {
                MeasurementStatus.SEARCHING   -> "Arahkan Wajah ke Kamera"
                MeasurementStatus.STABILIZING -> "Mendeteksi Wajah..."
                MeasurementStatus.KEEP_STEADY -> "Tahan Diam"
                MeasurementStatus.MEASURING   -> "Jangan Bergerak"
            },
            style = VitalPulseTheme.Typography.headlineMedium
        )

        Spacer(Modifier.height(28.dp))
        ScannerView(result, onResult)

        Spacer(Modifier.height(32.dp))
        TimerCard(timer, result.status)

        Spacer(Modifier.height(16.dp))
        LiveSignalCard(signalHistory)

        if (result.status == MeasurementStatus.SEARCHING) {
            Spacer(Modifier.height(20.dp))
            Text(
                "Pastikan wajah berada di area terang dan kamera menghadap langsung.",
                style = VitalPulseTheme.Typography.bodyMedium,
                color = VitalPulseTheme.OnSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun ScannerView(result: BpmResult, onResult: (BpmResult) -> Unit) {
    val progress by animateFloatAsState(
        targetValue = when (result.status) {
            MeasurementStatus.MEASURING -> 1f
            MeasurementStatus.STABILIZING, MeasurementStatus.KEEP_STEADY -> 0.3f
            else -> 0f
        },
        animationSpec = if (result.status == MeasurementStatus.MEASURING)
            tween(15000, easing = LinearEasing) else tween(500),
        label = "progress"
    )

    Box(modifier = Modifier.size(280.dp), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(Color(0xFFF1F5F9), style = Stroke(6.dp.toPx()))
            drawArc(
                VitalPulseTheme.Primary, -90f, 360f * progress, false,
                style = Stroke(6.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Box(
            modifier = Modifier
                .size(248.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(3.dp, VitalPulseTheme.OutlineVariant, CircleShape)
        ) {
            CameraPreview(onResult)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(0.65f to Color.Transparent, 1f to Color.White.copy(0.2f))
                    )
            )
            // Dashed face guide oval
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                drawOval(
                    color = VitalPulseTheme.Primary.copy(alpha = 0.25f),
                    topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.18f, size.height * 0.08f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.64f, size.height * 0.75f),
                    style = Stroke(2.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 8f)))
                )
            }
        }

        // Status badge
        Box(
            modifier = Modifier
                .offset(y = 138.dp)
                .background(Color.White, CircleShape)
                .border(1.dp, VitalPulseTheme.Outline, CircleShape)
                .padding(horizontal = 14.dp, vertical = 7.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val dotColor = when (result.status) {
                    MeasurementStatus.MEASURING   -> Color(0xFF22C55E)
                    MeasurementStatus.SEARCHING   -> Color(0xFFEF4444)
                    else                           -> VitalPulseTheme.Amber
                }
                Box(modifier = Modifier.size(8.dp).background(dotColor, CircleShape))
                Text(
                    result.status.name.replace("_", " "),
                    style = VitalPulseTheme.Typography.labelSmall,
                    color = VitalPulseTheme.OnSurface
                )
            }
        }
    }
}

@Composable
fun TimerCard(timer: Int, status: MeasurementStatus) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(VitalPulseTheme.SurfaceContainer, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column {
            Text("WAKTU TERSISA", style = VitalPulseTheme.Typography.labelMedium, color = VitalPulseTheme.OnSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    if (status == MeasurementStatus.MEASURING || status == MeasurementStatus.KEEP_STEADY) "$timer"
                    else "--",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    color = VitalPulseTheme.Primary
                )
                Text(
                    "  DETIK",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = VitalPulseTheme.Primary.copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
        }
        Icon(
            Icons.Default.Timer, null,
            tint = VitalPulseTheme.Primary.copy(alpha = 0.08f),
            modifier = Modifier.size(56.dp).align(Alignment.CenterEnd)
        )
    }
}

@Composable
fun LiveSignalCard(history: List<Float>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(20.dp))
            .border(1.dp, VitalPulseTheme.Outline, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Text("LIVE rPPG FEED", style = VitalPulseTheme.Typography.labelMedium, color = VitalPulseTheme.OnSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth().height(80.dp)) {
            if (history.size < 2) return@Canvas
            val path = Path()
            val step = size.width / 50f
            val max = history.max()
            val min = history.min()
            val range = (max - min).coerceAtLeast(0.01f)
            history.forEachIndexed { i, v ->
                val x = i * step
                val y = size.height / 2f - ((v - min) / range - 0.5f) * size.height * 0.8f
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, VitalPulseTheme.Secondary, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
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
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build().also { it.setAnalyzer(executor, rPPGAnalyzer(onResult)) }
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, preview, analysis)
        }, ContextCompat.getMainExecutor(context))
    }
    AndroidView({ previewView }, Modifier.fillMaxSize())
}
