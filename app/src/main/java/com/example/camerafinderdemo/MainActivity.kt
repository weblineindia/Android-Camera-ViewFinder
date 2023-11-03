package com.example.camerafinderdemo

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.hardware.camera2.*
import android.hardware.camera2.CameraDevice.StateCallback
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.Surface
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.viewfinder.CameraViewfinder
import androidx.camera.viewfinder.ViewfinderSurfaceRequest
import androidx.camera.viewfinder.internal.utils.futures.FutureCallback
import androidx.camera.viewfinder.internal.utils.futures.Futures
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import java.util.*


@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    lateinit var cameraViewfinder: CameraViewfinder
    lateinit var manager: CameraManager

    lateinit var cameraDevice: CameraDevice
    lateinit var cameraStateCallback: StateCallback
    lateinit var captureSessionCallback: CameraCaptureSession.StateCallback
    lateinit var cameraCaptureSession: CameraCaptureSession

    lateinit var captureSurface: Surface

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        initialization()
        cameraCallbacks()
        checkAndRequestPermission()
        openCamera()
    }

    /**
     * View Initialization
     */
    private fun initialization() {
        cameraViewfinder = findViewById(R.id.view_finder)
        manager = getSystemService(CAMERA_SERVICE) as CameraManager
    }

    /**
     * Camera state callbacks
     */
    private fun cameraCallbacks() {
        // CameraDevice.StateCallback to set CameraDevice object
        cameraStateCallback = object : StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                Toast.makeText(applicationContext, "onOpened", Toast.LENGTH_SHORT).show()
                cameraDevice = camera
            }

            override fun onDisconnected(camera: CameraDevice) {
                Toast.makeText(applicationContext, "onDisconnected", Toast.LENGTH_SHORT)
                    .show()
            }

            override fun onError(camera: CameraDevice, error: Int) {
                Toast.makeText(applicationContext, "onError", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Request Camera permissions
     */
    private fun checkAndRequestPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 101);
        }
    }

    /**
     * Open camera view
     */
    @SuppressLint("MissingPermission")
    private fun openCamera() {
        manager.openCamera(manager.cameraIdList[0], cameraStateCallback, Handler())
        startCamera()
    }

    /**
     * Capture session callbacks once camera rendered
     */
    private fun captureSessionCallback() {
        // CameraCaptureSession.StateCallback to set CameraCaptureSession object
        captureSessionCallback = object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                cameraCaptureSession = session
                try {
                    val previewRequestBuilder: CaptureRequest.Builder = cameraDevice
                        .createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                    previewRequestBuilder.addTarget(captureSurface)
                    cameraCaptureSession.setRepeatingRequest(
                        previewRequestBuilder.build(),
                        null, null
                    )
                } catch (e: Exception) {
                    Log.d("sessionCallback", "capture session configure error : ${e.stackTrace}")
                }

            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                Log.d("sessionCallback", "capture session configure error")
            }
        }
    }

    /**
     * Method to start camera
     */
    @SuppressLint("RestrictedApi")
    fun startCamera() {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val height = displayMetrics.heightPixels
        val width = displayMetrics.widthPixels
        val previewResolution = Size(width, height)

        val cameraId = manager.cameraIdList[0]
        val chars: CameraCharacteristics = manager.getCameraCharacteristics(cameraId)
        val viewfinderSurfaceRequest = ViewfinderSurfaceRequest(previewResolution, chars)
        val surfaceListenableFuture = cameraViewfinder.requestSurfaceAsync(viewfinderSurfaceRequest)

        captureSessionCallback()

        Futures.addCallback(surfaceListenableFuture, object : FutureCallback<Surface> {
            @RequiresApi(Build.VERSION_CODES.P)
            override fun onSuccess(surface: Surface) {
                //create a CaptureSession using this surface as usual
                captureSurface = surface
                val outputConfiguration = OutputConfiguration(captureSurface)

                val sessionConfig = SessionConfiguration(
                    SessionConfiguration.SESSION_REGULAR,
                    listOf(outputConfiguration),
                    ContextCompat.getMainExecutor(this@MainActivity),
                    captureSessionCallback
                )
                cameraDevice.createCaptureSession(sessionConfig)
            }

            override fun onFailure(t: Throwable) { /* something went wrong */
            }
        }, ContextCompat.getMainExecutor(this@MainActivity))

    }
}