package org.tensorflow.lite.examples.objectdetection

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.tensorflow.lite.examples.objectdetection.audioUploader.AndroidAudioUploader
import org.tensorflow.lite.examples.objectdetection.databinding.ActivityMainBinding
import org.tensorflow.lite.examples.objectdetection.fragments.CameraFragment
import org.tensorflow.lite.examples.objectdetection.playback.AndroidAudioPlayer
import org.tensorflow.lite.examples.objectdetection.record.AndroidAudioRecorder
import java.io.File
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity(){

    private lateinit var activityMainBinding: ActivityMainBinding
    private var camfragment: CameraFragment? = null

    private val recorder by lazy {
        AndroidAudioRecorder(applicationContext)
    }

    private val player by lazy {
        AndroidAudioPlayer(applicationContext)
    }

    private  var audioFile: File? = null

    private val uploader by lazy {
        AndroidAudioUploader(applicationContext,this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)

        //request permission to record audio from user
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            9
        )

        //open the CameraFragment when the MainActivity starts
         camfragment = CameraFragment()

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, camfragment!!)
            .addToBackStack(null) // allows the user to navigate back to the previous fragment by pressing the back button.
            .commit()

    }

    override fun onBackPressed() {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            // Workaround for Android Q memory leak issue in IRequestFinishCallback$Stub.
            // (https://issuetracker.google.com/issues/139738913)
            finishAfterTransition()
        } else {
            super.onBackPressed()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        camfragment = null // Setting the reference to null
    }

    fun record_btn(){
        Toast.makeText(this, "Recording", Toast.LENGTH_SHORT).show()
        File(cacheDir, "audio.mp3").also {
            recorder.start(it)
            audioFile = it
        }
    }
    fun stop_record(){
        recorder.stop()
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                uploader.uploadAudio(audioFile!!)
            }
        }
    }

    fun play_record(){
        player.playFile(audioFile?:return)
    }

    fun stop_playing(){
        player.stop()
    }

    fun playResponse(file: File){
        player.playFile(file)
    }
}
