package org.tensorflow.lite.examples.objectdetection.audioUploader

import android.content.Context
import android.os.Looper
import android.os.Handler
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import okhttp3.Request
import okhttp3.Response
import okhttp3.Callback
import okhttp3.Call
import okhttp3.RequestBody.Companion.asRequestBody

import org.json.JSONException
import org.json.JSONObject
import android.util.Base64
import org.tensorflow.lite.examples.objectdetection.MainActivity
import java.io.FileOutputStream


class AndroidAudioUploader(
    private val context: Context,
    private val mainActivity: MainActivity
): AudioUploader {

    private val serverUrl = "http://192.168.0.100:5111/get_audio/"

    override suspend fun uploadAudio(file: File) {

//        val client = OkHttpClient()
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS) // Setting connection timeout to 30 seconds
            .build()

        val requestBody: RequestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                file.name,
                file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
            ).build()

        val request: Request = Request.Builder()
            .url(serverUrl)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {

                val responseBody = response.body

                if (response.isSuccessful && responseBody != null) {
                    val responseData = responseBody.string()
                    try {
                        val jsonResponse = JSONObject(responseData)
                        val extractedText = jsonResponse.getString("text_from_audio")
                        val base64Audio = jsonResponse.getString("audio_file")

                        // Decode base64 audio data
                        val decodedAudio = Base64.decode(base64Audio, Base64.DEFAULT)

                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(context, " $extractedText", Toast.LENGTH_SHORT).show()
                        }

                        val audioFile = File(context.cacheDir, "audio.mp3")
                        // Write the decoded audio data to the file
                        FileOutputStream(audioFile).use { outputStream ->
                            outputStream.write(decodedAudio)
                        }
                        //calling playResponse() in mainActivity
                        mainActivity.playResponse(audioFile)

                    } catch (e: JSONException) {
                        e.printStackTrace()
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(context, "Error parsing server response", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    // Handle the unsuccessful response
                    Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "Server Response not received", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                // Handle the failure to communicate with the server
                Log.e("ServerRequest", "Server Request Failed: ${e.message}")

                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "Server Request Failed", Toast.LENGTH_SHORT).show()
                }
                e.printStackTrace()
            }
        })
    }


}

