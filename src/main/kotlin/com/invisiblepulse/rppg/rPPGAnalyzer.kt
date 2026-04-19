package com.invisiblepulse.rppg

import android.graphics.Rect
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import org.jtransforms.fft.DoubleFFT_1D
import java.nio.ByteBuffer
import java.util.*
import kotlin.math.*

enum class MeasurementStatus {
    SEARCHING, STABILIZING, KEEP_STEADY, MEASURING
}

data class BpmResult(
    val bpm: Double,
    val confidence: Double,
    val signalValue: Double,
    val status: MeasurementStatus,
    val hrv: Double = 0.0,
    val faceBox: Rect? = null,
    val last15sSignal: List<Double> = emptyList()
)

class rPPGAnalyzer(private val onResult: (BpmResult) -> Unit) : ImageAnalysis.Analyzer {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder().setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST).build()
    )
    
    private val signalBuffer = mutableListOf<Double>()
    private val timestampBuffer = mutableListOf<Long>()
    private val bpmHistory = mutableListOf<Double>()
    private val peakIntervals = mutableListOf<Long>()
    private val BUFFER_SIZE = 128 
    private val fft = DoubleFFT_1D(BUFFER_SIZE.toLong())

    private var frameCounter = 0
    private var lastPeakTime: Long = 0
    private var lastFaceBox: Rect? = null
    private var lastForeheadROI: Rect? = null

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return
        frameCounter++
        
        if (frameCounter % 10 == 0 || lastForeheadROI == null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            detector.process(image)
                .addOnSuccessListener { faces ->
                    if (faces.isNotEmpty()) {
                        val face = faces[0]
                        lastFaceBox = face.boundingBox
                        lastForeheadROI = extractForeheadROI(face.boundingBox)
                        processFrameWithROI(imageProxy, lastForeheadROI!!)
                    } else {
                        lastFaceBox = null
                        lastForeheadROI = null
                        onResult(BpmResult(0.0, 0.0, 0.0, MeasurementStatus.SEARCHING))
                        imageProxy.close()
                    }
                }
                .addOnFailureListener { imageProxy.close() }
        } else {
            processFrameWithROI(imageProxy, lastForeheadROI!!)
        }
    }

    private fun processFrameWithROI(imageProxy: ImageProxy, roi: Rect) {
        val avgGreen = extractAverageGreen(imageProxy, roi)
        if (avgGreen > 0) {
            signalBuffer.add(avgGreen)
            timestampBuffer.add(System.currentTimeMillis())
            if (signalBuffer.size > BUFFER_SIZE) {
                signalBuffer.removeAt(0)
                timestampBuffer.removeAt(0)
            }
            if (signalBuffer.size >= BUFFER_SIZE) processSignal(avgGreen)
            else onResult(BpmResult(0.0, 0.0, avgGreen, MeasurementStatus.STABILIZING, faceBox = lastFaceBox))
        }
        imageProxy.close()
    }

    private fun extractForeheadROI(boundingBox: Rect): Rect {
        val w = boundingBox.width()
        val h = boundingBox.height()
        return Rect((boundingBox.left + w * 0.3).toInt(), (boundingBox.top + h * 0.1).toInt(), (boundingBox.right - w * 0.3).toInt(), (boundingBox.top + h * 0.25).toInt())
    }

    private fun extractAverageGreen(imageProxy: ImageProxy, roi: Rect): Double {
        val yPlane = imageProxy.planes[0].buffer
        val uPlane = imageProxy.planes[1].buffer
        val vPlane = imageProxy.planes[2].buffer
        val yStride = imageProxy.planes[0].rowStride
        val uvStride = imageProxy.planes[1].rowStride
        val uvPixelStride = imageProxy.planes[1].pixelStride

        var sumG = 0.0
        var count = 0
        val l = roi.left.coerceIn(0, imageProxy.width - 1)
        val t = roi.top.coerceIn(0, imageProxy.height - 1)
        val r = roi.right.coerceIn(0, imageProxy.width - 1)
        val b = roi.bottom.coerceIn(0, imageProxy.height - 1)

        for (y in t until b step 2) {
            for (x in l until r step 2) {
                val yi = y * yStride + x
                val uvi = (y / 2) * uvStride + (x / 2) * uvPixelStride
                if (yi < yPlane.capacity() && uvi < uPlane.capacity()) {
                    val yv = yPlane.get(yi).toInt() and 0xFF
                    val uv = (uPlane.get(uvi).toInt() and 0xFF) - 128
                    val vv = (vPlane.get(uvi).toInt() and 0xFF) - 128
                    sumG += yv - 0.3441 * uv - 0.7141 * vv
                    count++
                }
            }
        }
        return if (count > 0) sumG / count else 0.0
    }

    private fun processSignal(currentVal: Double) {
        val fps = (BUFFER_SIZE - 1) / ((timestampBuffer.last() - timestampBuffer.first()) / 1000.0)
        val mean = signalBuffer.average()
        val processed = DoubleArray(BUFFER_SIZE * 2)
        for (i in 0 until BUFFER_SIZE) {
            val win = 0.54 - 0.46 * cos(2.0 * PI * i / (BUFFER_SIZE - 1))
            processed[i] = (signalBuffer[i] - mean) * win
        }

        fft.realForward(processed)
        var maxP = -1.0; var totalP = 0.0; var peakF = 0.0; var bins = 0
        for (i in 1 until BUFFER_SIZE / 2) {
            val power = processed[2*i].pow(2) + processed[2*i+1].pow(2)
            val freq = i * fps / BUFFER_SIZE
            if (freq in 0.7..4.0) {
                totalP += power; bins++
                if (power > maxP) { maxP = power; peakF = freq }
            }
        }

        val conf = (maxP / (totalP / bins)).coerceIn(0.0, 10.0) / 10.0
        val bpm = peakF * 60.0
        bpmHistory.add(bpm); if (bpmHistory.size > 5) bpmHistory.removeAt(0)
        
        // HRV Peak Detection
        if (currentVal > mean && System.currentTimeMillis() - lastPeakTime > 450) {
            if (lastPeakTime != 0L) {
                peakIntervals.add(System.currentTimeMillis() - lastPeakTime)
                if (peakIntervals.size > 15) peakIntervals.removeAt(0)
            }
            lastPeakTime = System.currentTimeMillis()
        }
        val hrv = if (peakIntervals.size > 2) sqrt(peakIntervals.zipWithNext { a, b -> (a - b).toDouble().pow(2) }.average()) else 0.0

        val status = if (conf < 0.35) MeasurementStatus.KEEP_STEADY else MeasurementStatus.MEASURING
        onResult(BpmResult(bpmHistory.average(), conf, currentVal - mean, status, hrv, lastFaceBox, signalBuffer.toList()))
    }
}
