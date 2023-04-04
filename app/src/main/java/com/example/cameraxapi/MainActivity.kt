package com.example.cameraxapi

import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.camera.camera2.internal.compat.workaround.TargetAspectRatio.RATIO_4_3
import androidx.camera.core.*
import androidx.camera.core.ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
import androidx.camera.core.impl.ImageCaptureConfig
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.cameraxapi.databinding.ActivityMainBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var  imageCapture:ImageCapture?=null
    private lateinit var outputDirectory: File

    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()

        if(allPermissionGranted()){
            startCamera()
            Toast.makeText(this,
                "We have Permission",
                Toast.LENGTH_SHORT).show()
        } else{
            ActivityCompat.requestPermissions(
                this, Constants.REQUIRED_PERMISSIONS,
                Constants.REQUEST_CODE_PERMISSIONS
            )
        }

        binding.btnTakePhoto.setOnClickListener{
            takePhoto()
        }
    }
    private fun getOutputDirectory(): File{
        val mediaDir = externalMediaDirs.firstOrNull()?.let{mFile->
            File(mFile,resources.getString(R.string.app_name)).apply {
                mkdirs()
            }
        }

        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    private fun takePhoto(){

        val imageCapture = imageCapture?: return
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(Constants.FILE_NAME_FORMAT,
                Locale.getDefault())
                .format(System.currentTimeMillis())+".jpg")
        val outputOption = ImageCapture
            .OutputFileOptions
            .Builder(photoFile)
            .build()

        imageCapture.takePicture(
            outputOption, ContextCompat.getMainExecutor(this),
            object :ImageCapture.OnImageSavedCallback{
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {


                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo Saved"

                    Toast.makeText(
                        this@MainActivity,
                        "$msg $savedUri",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(Constants.TAG,
                        "onError: ${exception.message}",
                        exception)
                }

            }
        )
    }


    private fun startCamera(){

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({

            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
//            val preview = Preview.Builder()
//                .build()
//                .also{mPreview->
//                    mPreview.setSurfaceProvider(
//                        binding.viewFinder.surfaceProvider
//                    )
//
//                }

            imageCapture = ImageCapture.Builder()
//                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetResolution(Size(6120, 8160))
                .setCaptureMode(CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {

                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    this, cameraSelector, imageCapture, //preview
                )

            }catch(e:Exception){
                Log.d(Constants.TAG, "startCamera Fail:", e)
            }

        }, ContextCompat.getMainExecutor(this))

    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == Constants.REQUEST_CODE_PERMISSIONS){
            if(allPermissionGranted()){
                startCamera()
            }else {
                Toast.makeText(this,
                    "Permission not granted",
                    Toast.LENGTH_SHORT).show()

                finish()
            }
        }
    }

    private fun allPermissionGranted()=
        Constants.REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                baseContext, it
            ) == PackageManager.PERMISSION_GRANTED
        }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

}