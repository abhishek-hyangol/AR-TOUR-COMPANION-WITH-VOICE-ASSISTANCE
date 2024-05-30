package org.tensorflow.lite.examples.objectdetection.audioUploader

import java.io.File

interface AudioUploader {
    suspend fun uploadAudio(file: File)
}