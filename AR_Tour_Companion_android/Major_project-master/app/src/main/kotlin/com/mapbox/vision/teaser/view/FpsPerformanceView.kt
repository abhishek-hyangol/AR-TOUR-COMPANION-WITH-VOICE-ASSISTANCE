package com.mapbox.vision.teaser.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.mapbox.vision.mobile.core.models.FrameStatistics
import com.mapbox.vision.teaser.R
import com.mapbox.vision.teaser.databinding.ViewFpsPerformanceBinding
import java.util.concurrent.TimeUnit

class FpsPerformanceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyle, defStyleRes) {

    companion object {
        private const val SESSION_TIME_FORMAT = "Duration: %02d:%02d:%02d"
    }

    // Add a property to hold the generated view binding object
    private val binding: ViewFpsPerformanceBinding =
        ViewFpsPerformanceBinding.inflate(LayoutInflater.from(context), this)

    private var sumSegmentationDetectionFps = 0f
    private var sumCoreUpdatesFps = 0f

    private var countSegmentationDetectionFps = 0L
    private var countCoreUpdatesFps = 0L

//    init {
//        LayoutInflater.from(context).inflate(R.layout.view_fps_performance, this, true)
//    }

    @SuppressLint("SetTextI18n")
    fun showInfo(frameStatistics: FrameStatistics) {
        with(frameStatistics) {
            if (segmentationDetectionFps > 0) {
                sumSegmentationDetectionFps += segmentationDetectionFps
                binding.mergeModelFps.text =
                    "MM: ${segmentationDetectionFps.round()}  AVG: ${(sumSegmentationDetectionFps / ++countSegmentationDetectionFps).round()}"
            }

            if (coreUpdateFps > 0) {
                sumCoreUpdatesFps += coreUpdateFps
                binding.coreUpdateFps.text =
                    "CU: ${coreUpdateFps.round()}  AVG: ${(sumCoreUpdatesFps / ++countCoreUpdatesFps).round()}"
            }
        }
    }

    fun resetAverageFps() {
        sumSegmentationDetectionFps = 0f
        sumCoreUpdatesFps = 0f

        countSegmentationDetectionFps = 0L
        countCoreUpdatesFps = 0L
    }

    fun setCalibrationProgress(calibrationProgress: Float) {
        binding.calibrationProgress.text = context.getString(
            R.string.calibration_progress,
            (calibrationProgress * 100).toInt()
        )
    }

    fun setTimestamp(timestamp: Long) {
        val hours = TimeUnit.MILLISECONDS.toHours(timestamp) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timestamp) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timestamp) % 60
        binding.sessionTime.text = String.format(SESSION_TIME_FORMAT, hours, minutes, seconds)
    }

    private fun Float.round() = String.format("%.2f", this)
}
