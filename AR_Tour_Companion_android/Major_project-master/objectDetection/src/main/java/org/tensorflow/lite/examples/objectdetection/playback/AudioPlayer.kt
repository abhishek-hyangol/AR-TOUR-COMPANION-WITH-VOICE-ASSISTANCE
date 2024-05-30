package org.tensorflow.lite.examples.objectdetection.playback

import java.io.File

interface AudioPlayer {
    fun playFile(file: File)
    fun stop()
}