package org.tensorflow.lite.examples.objectdetection.fragments

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.text.SpannableString
import android.text.style.StyleSpan
import android.util.Size
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
import androidx.core.app.ActivityCompat
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.max

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.google.android.gms.common.api.Api
import com.google.android.gms.common.internal.service.Common.API
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.util.LinkedList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import org.tensorflow.lite.examples.objectdetection.ObjectDetectorHelper
import org.tensorflow.lite.examples.objectdetection.R
import org.tensorflow.lite.examples.objectdetection.databinding.FragmentCameraBinding
import org.tensorflow.lite.task.gms.vision.detector.Detection
import org.tensorflow.lite.examples.objectdetection.MainActivity

class CameraFragment : Fragment(), ObjectDetectorHelper.DetectorListener {

    private val TAG = "CameraFragment"

    private var _fragmentCameraBinding: FragmentCameraBinding? = null

    private val fragmentCameraBinding
        get() = _fragmentCameraBinding!!

    private lateinit var objectDetectorHelper: ObjectDetectorHelper
    private lateinit var bitmapBuffer: Bitmap
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null

    /** Blocking camera operations are performed using this executor */
    private lateinit var cameraExecutor: ExecutorService

    override fun onResume() {
        super.onResume()
//         Make sure that all permissions are still present, since the
//         user could have removed them while the app was in paused state.
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            Navigation.findNavController(requireActivity(), R.id.fragment_container)
                .navigate(CameraFragmentDirections.actionCameraToPermissions())
        }
    }

    override fun onDestroyView() {
        _fragmentCameraBinding = null
        super.onDestroyView()

        // Shut down our background executor
        cameraExecutor.shutdown()
    }

    override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
    ): View {
        _fragmentCameraBinding = FragmentCameraBinding.inflate(inflater, container, false)

        return fragmentCameraBinding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        objectDetectorHelper = ObjectDetectorHelper(
            context = requireContext(),
            objectDetectorListener = this)


        // Attach listeners to UI control widgets
        initBottomSheetControls()

        val mainActivity = activity as? MainActivity
//        Mic button pressed
        fragmentCameraBinding.imageButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    mainActivity?.record_btn()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    mainActivity?.stop_record()
                    true
                }
                else -> false
            }
        }
        fragmentCameraBinding.imageButton3.setOnClickListener {
            mainActivity?.play_record()
        }
        fragmentCameraBinding.imageButton4.setOnClickListener {
            mainActivity?.stop_playing()
        }

    }

    private fun initBottomSheetControls() {
        // When clicked, lower detection score threshold floor
        fragmentCameraBinding.bottomSheetLayout.thresholdMinus.setOnClickListener {
            if (objectDetectorHelper.threshold >= 0.1) {
                objectDetectorHelper.threshold -= 0.1f
                updateControlsUi()
            }
        }

        // When clicked, raise detection score threshold floor
        fragmentCameraBinding.bottomSheetLayout.thresholdPlus.setOnClickListener {
            if (objectDetectorHelper.threshold <= 0.8) {
                objectDetectorHelper.threshold += 0.1f
                updateControlsUi()
            }
        }

        // When clicked, reduce the number of objects that can be detected at a time
        fragmentCameraBinding.bottomSheetLayout.maxResultsMinus.setOnClickListener {
            if (objectDetectorHelper.maxResults > 1) {
                objectDetectorHelper.maxResults--
                updateControlsUi()
            }
        }

        // When clicked, increase the number of objects that can be detected at a time
        fragmentCameraBinding.bottomSheetLayout.maxResultsPlus.setOnClickListener {
            if (objectDetectorHelper.maxResults < 5) {
                objectDetectorHelper.maxResults++
                updateControlsUi()
            }
        }

        // When clicked, decrease the number of threads used for detection
        fragmentCameraBinding.bottomSheetLayout.threadsMinus.setOnClickListener {
            if (objectDetectorHelper.numThreads > 1) {
                objectDetectorHelper.numThreads--
                updateControlsUi()
            }
        }

        // When clicked, increase the number of threads used for detection
        fragmentCameraBinding.bottomSheetLayout.threadsPlus.setOnClickListener {
            if (objectDetectorHelper.numThreads < 4) {
                objectDetectorHelper.numThreads++
                updateControlsUi()
            }
        }

        // When clicked, change the underlying hardware used for inference. Current options are CPU
        // GPU, and NNAPI
        fragmentCameraBinding.bottomSheetLayout.spinnerDelegate.setSelection(0, false)
        fragmentCameraBinding.bottomSheetLayout.spinnerDelegate.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    objectDetectorHelper.currentDelegate = p2
                    updateControlsUi()
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    /* no op */
                }
            }

        // When clicked, change the underlying model used for object detection
        fragmentCameraBinding.bottomSheetLayout.spinnerModel.setSelection(0, false)
        fragmentCameraBinding.bottomSheetLayout.spinnerModel.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    objectDetectorHelper.currentModel = p2
                    updateControlsUi()
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    /* no op */
                }
            }
    }

    // Update the values displayed in the bottom sheet. Reset detector.
    private fun updateControlsUi() {
        fragmentCameraBinding.bottomSheetLayout.maxResultsValue.text =
            objectDetectorHelper.maxResults.toString()
        fragmentCameraBinding.bottomSheetLayout.thresholdValue.text =
            String.format("%.2f", objectDetectorHelper.threshold)
        fragmentCameraBinding.bottomSheetLayout.threadsValue.text =
            objectDetectorHelper.numThreads.toString()

        // Needs to be cleared instead of reinitialized because the GPU
        // delegate needs to be initialized on the thread using it when applicable
        objectDetectorHelper.clearObjectDetector()
        fragmentCameraBinding.overlay.clear()
    }

    // Initialize CameraX, and prepare to bind the camera use cases
    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            {
                // CameraProvider
                cameraProvider = cameraProviderFuture.get()

                // Build and bind the camera use cases
                bindCameraUseCases()
            },
            ContextCompat.getMainExecutor(requireContext())
        )
    }

    // Declare and bind preview, capture and analysis use cases
    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {

        // CameraProvider
        val cameraProvider =
            cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

        // CameraSelector - makes assumption that we're only using the back camera
        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

        // Preview. Only using the 4:3 ratio because this is the closest to our models
        preview =
            Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
                .build()

        // ImageAnalysis. Using RGBA 8888 to match how our models work
        imageAnalyzer =
            ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                // The analyzer can then be assigned to the instance
                .also {
                    it.setAnalyzer(cameraExecutor) { image ->
                        if (!::bitmapBuffer.isInitialized) {
                            // The image rotation and RGB image buffer are initialized only once
                            // the analyzer has started running
                            bitmapBuffer = Bitmap.createBitmap(
                                image.width,
                                image.height,
                                Bitmap.Config.ARGB_8888
                            )
                        }

                        detectObjects(image)
                    }
                }

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)

            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(fragmentCameraBinding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun detectObjects(image: ImageProxy) {
        // Copy out RGB bits to the shared bitmap buffer
        image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }

        val imageRotation = image.imageInfo.rotationDegrees
        // Pass Bitmap and rotation to the object detector helper for processing and detection
        objectDetectorHelper.detect(bitmapBuffer, imageRotation)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation = fragmentCameraBinding.viewFinder.display.rotation
    }

    // Update UI after objects have been detected. Extracts original image height/width
    // to scale and place bounding boxes properly through OverlayView
    override fun onResults(
        image : Bitmap,
      results: MutableList<Detection>?,
      inferenceTime: Long,
      imageHeight: Int,
      imageWidth: Int
    ) {
        activity?.runOnUiThread {
            fragmentCameraBinding.bottomSheetLayout.inferenceTimeVal.text =
                String.format("%d ms", inferenceTime)

            // Pass necessary information to OverlayView for drawing on the canvas
            fragmentCameraBinding.overlay.setResults(
                results ?: LinkedList<Detection>(),
                imageHeight,
                imageWidth
            )

            // Mlrecog: take image within bounding box and
            fragmentCameraBinding.overlay.setOnTouchListener(object : View.OnTouchListener {
                override fun onTouch(v: View, event: MotionEvent): Boolean {



                    val matrix = Matrix()

                    matrix.postRotate(-270F)

                    val scaledBitmap = Bitmap.createScaledBitmap(image, image.width, image.height, true)

                    val rotatedBitmap = Bitmap.createBitmap(
                        scaledBitmap,
                        0,
                        0,
                        scaledBitmap.width,
                        scaledBitmap.height,
                        matrix,
                        true
                    )


                    val xValue = event.getX()
                    val  yValue = event.getY()
                    var top = 0.0f
                    var bottom = 0.0f
                    var right = 0.0f
                    var left = 0.0f
                    var croppedHeight = 0.0f
                    var croppedWidth = 0.0f
                    val width = v.width
                    val height = v.height

                    val  scaleFactor = (max(width * 1f / imageWidth, height * 1f / imageHeight))
                    var tf = false
                    if (results != null && results.size > 0) {
                        val result = results[0]
                        val boundingBox = result.boundingBox
                        top = boundingBox.top
                        bottom = boundingBox.bottom
                        left = boundingBox.left
                        right = boundingBox.right

                        if( top > 0 && bottom > 0 && left > 0 && right > 0) {

                            croppedHeight = abs(((bottom) - (top)))
                            if (croppedHeight + top >= image.width){
                                croppedHeight = image.width - top
                            }
                            croppedWidth = abs(((right) - (left)))
                            if (croppedWidth + left >= image.height){
                                croppedWidth = image.height - left
                            }

                            val rect = RectF(left * scaleFactor, top * scaleFactor, right * scaleFactor, bottom * scaleFactor)
                            tf = rect.contains(xValue, yValue)
                        }
                    }

                    if (tf){

                        val croppedBitmap = Bitmap.createBitmap(rotatedBitmap, abs(left.toInt()), abs(top.toInt()), croppedWidth.toInt(), croppedHeight.toInt())
                        showAndSaveImagePopup(croppedBitmap)
                    }


                    return v?.onTouchEvent(event) ?: true
                }
            })

            // Force a redraw
            fragmentCameraBinding.overlay.invalidate()
        }
    }

    interface API {
        @Multipart
        @POST("predict")
        fun sendData(@Part("ltlg") string: RequestBody, @Part image: MultipartBody.Part ): Call<ResponseBody>
    }

    private fun showAndSaveImagePopup( bitmap: Bitmap) {


        val builder = AlertDialog.Builder(context)
        val imageView = ImageView(context)
        imageView.setPadding(20,50,20,20)
        imageView.setImageBitmap(bitmap)
        val gradientDrawable = GradientDrawable()
        gradientDrawable.shape = GradientDrawable.RECTANGLE
        gradientDrawable.setColor(Color.BLACK)
        imageView.background = gradientDrawable
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        layout.addView(imageView)


        val progressBar = ProgressBar(context)
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)
        layoutParams.setMargins(20,50,20,50) // here you set the margin you want
        layoutParams.gravity = Gravity.CENTER
        progressBar.layoutParams = layoutParams
        layout.addView(progressBar)
        builder.setView(layout)

        // circular button to replace MoreDetails button
        fun dpToPx(dp: Int): Int {
            val density = resources.displayMetrics.density
            return (dp * density).toInt()
        }

        val cardView = CardView(requireContext())
        val cardLayoutParams = LinearLayout.LayoutParams(dpToPx(50), dpToPx(50))
        cardLayoutParams.topMargin = dpToPx(10)
        cardLayoutParams.gravity = Gravity.CENTER_HORIZONTAL
        cardView.layoutParams = cardLayoutParams
        cardView.cardElevation = dpToPx(4).toFloat()
        cardView.radius = dpToPx(60).toFloat()

        val myButton = ImageButton(context)
        myButton.id = View.generateViewId()
        myButton.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        myButton.setBackgroundResource(R.drawable.circular_btn_shape)
        myButton.contentDescription = resources.getString(R.string.recorder_button)
        myButton.scaleType = ImageView.ScaleType.CENTER_CROP
        myButton.setImageResource(R.drawable.mic)

        cardView.addView(myButton)

// Add cardView to a parent view
        layout.addView(cardView)



        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()

        dialog.setOnShowListener {

            val cancelButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)

            var location = getDeviceLocation()

            val retrofit = Retrofit.Builder()
                .baseUrl("http://192.168.0.100:5111/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service = retrofit.create(CameraFragment.API::class.java)
            val imageBytes = stream.toByteArray()
            val requestFile = RequestBody.create( "image/jpeg"?.toMediaTypeOrNull(), imageBytes)
            val fileimg = MultipartBody.Part.createFormData("file", "image.jpg", requestFile)

            val ltlg = RequestBody.create("text/plain"?.toMediaTypeOrNull(), location)
            val startTime = System.currentTimeMillis()
            val call = service.sendData(ltlg, fileimg)
            call.enqueue(object : Callback<ResponseBody> {
                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    if (response.isSuccessful) {

                        val endTime = System.currentTimeMillis()
                        val elapsedTime = endTime - startTime
                        Toast.makeText(context, "$elapsedTime ms", Toast.LENGTH_SHORT).show()

                        val imageData = response.body()!!.bytes()
                        val bmp = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)

                        val responseData = response.headers()
                        if (response.body() != null) {
                            val imgView: ImageView = (layout.getChildAt(0)) as ImageView

                            imgView.setImageBitmap(bmp)
                            layout.removeView(progressBar)

                            val infoTextTitle = TextView(context)
                            val spanTitle = "Predicted Data"
                            val spannableString = SpannableString(spanTitle)
                            spannableString.setSpan(StyleSpan(Typeface.BOLD), 0, spanTitle.length, 0)
                            infoTextTitle.text = spannableString
                            infoTextTitle.setPadding(80, 65, 20, 20)

                            layout.addView(infoTextTitle)
                            val infoTextView = TextView(context)
                            val info =  "Name: " + responseData.get("predname").toString()
                                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
//                            + "\nPred Conf: " + ((responseData.get("predconf").toString().toFloat() * 100).toInt()).toString() + " %"  + "\nInf Time: " + responseData.get("inftime").toString()
                            infoTextView.setText(info)
                            infoTextView.setPadding(80, 10, 20, 20)
                            layout.addView(infoTextView)


                            val myButton = Button(context)
                            myButton.setTextColor(Color.rgb(183,121,0))
//                            myButton.setTextSize(20f)
                            myButton.text = "  More Detail  "
                            myButton.isAllCaps = false
                            val shape = GradientDrawable()
                            shape.shape = GradientDrawable.RECTANGLE
                            shape.setColor(Color.WHITE)
                            shape.cornerRadius = 18F
                            myButton.background = shape
                            layout.addView(myButton)
                            val layoutParamss = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT)
                            layoutParamss.setMargins(75, 20, 20, 20)
                            layoutParamss.gravity = Gravity.START
                            myButton.layoutParams = layoutParamss

                        }

                    } else {
                        layout.removeView(progressBar)

                        val infoTextTitle = TextView(context)
                        val spanTitle = "No Monument Found"
                        val spannableString = SpannableString(spanTitle)
                        spannableString.setSpan(StyleSpan(Typeface.BOLD), 0, spanTitle.length, 0)
                        infoTextTitle.text = spannableString
                        infoTextTitle.setPadding(0, 80, 0, 80)
                        val infoTextTitleLayoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT)
                        infoTextTitleLayoutParams.gravity = Gravity.CENTER
                        infoTextTitle.layoutParams = infoTextTitleLayoutParams
                        layout.addView(infoTextTitle)
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    layout.removeView(progressBar)
                    val infoTextTitle = TextView(context)
                    val spanTitle = "Server Error or Check Internet Connection"
                    val spannableString = SpannableString(spanTitle)
                    spannableString.setSpan(StyleSpan(Typeface.BOLD), 0, spanTitle.length, 0)
                    infoTextTitle.text = spannableString
                    infoTextTitle.setPadding(0, 80, 0, 80)
                    val infoTextTitleLayoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT)
                    infoTextTitleLayoutParams.gravity = Gravity.CENTER
                    infoTextTitle.layoutParams = infoTextTitleLayoutParams
                    layout.addView(infoTextTitle)
                }
            })


            cancelButton.setOnClickListener {
                dialog.dismiss()
            }

        }

        dialog.setCancelable(false)
        dialog.show()

    }



    private fun getDeviceLocation(): String {
        // Get the current device location

        val location = getLastKnownLocation()
        if(location != null) {
            val locn = "(" + String.format("%.5f", location.latitude) + "," + String.format(
                "%.5f",
                location.longitude
            ) + ")"

            return locn
        }else{

            Toast.makeText(requireContext(), "Location Gave Null", Toast.LENGTH_SHORT).show()
            return "(27.6714,85.4293)"
        }
    }


    var mLocationManager: LocationManager? = null


    private fun getLastKnownLocation(): Location? {
        mLocationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = mLocationManager!!.getProviders(true)
        var bestLocation: Location? = null
        for (provider in providers) {
            val l = if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(requireContext(), "Location Permission Not Granted", Toast.LENGTH_SHORT).show()
                return Location("")
            }else {
                mLocationManager!!.getLastKnownLocation(provider) ?: continue
            }
            if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                // Found best last known location: %s", l);
                bestLocation = l
            }
        }
        return bestLocation
    }



    override fun onError(error: String) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onInitialized() {
        objectDetectorHelper.setupObjectDetector()
        // Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Wait for the views to be properly laid out
        fragmentCameraBinding.viewFinder.post {
            // Set up the camera and its use cases
            setUpCamera()
        }

        fragmentCameraBinding.progressCircular.visibility = View.GONE
    }

}



