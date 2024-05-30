package com.mapbox.vision.teaser.view

import android.content.Context
import android.text.format.DateUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.SeekBar
//import com.mapbox.vision.teaser.R
import com.mapbox.vision.teaser.databinding.ViewPlaybackSeekBarBinding

class PlaybackSeekBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyle, defStyleRes) {

//    init {
//        LayoutInflater.from(context).inflate(R.layout.view_playback_seek_bar, this, true)
//    }

    // Add a property to hold the generated view binding object
    private val binding: ViewPlaybackSeekBarBinding =
    ViewPlaybackSeekBarBinding.inflate(LayoutInflater.from(context), this)


    var onSeekBarChangeListener: SeekBar.OnSeekBarChangeListener? = null
        get() = field
        set(value) {
            binding.seekBar.setOnSeekBarChangeListener(value)
        }

    fun setProgress(seconds: Float) {
        binding.playbackPositionTimeText.text = DateUtils.formatElapsedTime(seconds.toLong())
        binding.seekBar.progress = seconds.toInt()
    }

    fun setDuration(seconds: Float) {
        binding.playbackDurationTimeText.text = DateUtils.formatElapsedTime(seconds.toLong())
        binding.seekBar.max = seconds.toInt()
    }
}
