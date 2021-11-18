package orllewin.tirwedd

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import java.io.File
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.appcompat.widget.PopupMenu
import androidx.camera.core.*
import androidx.camera.core.AspectRatio.RATIO_16_9
import androidx.camera.core.AspectRatio.RATIO_4_3
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.preference.PreferenceManager
import oppen.stracka.AnimationEndListener
import orllewin.extensions.*
import orllewin.file_io.CameraIO
import orllewin.file_io.OppenFileIO
import orllewin.tirwedd.databinding.ActivityMainBinding
import java.time.OffsetDateTime
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cacheFile: File

    private var aspectRatio = RATIO_16_9

    private var fileIO = OppenFileIO()
    private var cameraIO = CameraIO()
    private lateinit var imageProcessor: TirweddImageProcessor

    private var uri: Uri? = null
    private var bitmap: Bitmap? = null

    //CameraX
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    private var camera: Camera? = null
    private var zoomRatio = 1f


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()

        fileIO.register(this)
        cameraIO.register(this)
        imageProcessor = TirweddImageProcessor(lifecycleScope)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.overflow.setOnClickListener {
            val popup = PopupMenu(this@MainActivity, binding.overflow)

            val settingsItem = popup.menu.add("Settings")
            settingsItem.setOnMenuItemClickListener {
                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                true
            }

            val quitItem = popup.menu.add("Quit")
            quitItem.setOnMenuItemClickListener {
                this@MainActivity.finishAffinity()
                true
            }

            popup.show()
        }

        binding.cameraInner.setOnClickListener {

            binding.cameraInner.animate().scaleXBy(0.2f).scaleYBy(0.2f).setDuration(250).withEndAction {
                binding.cameraInner.scaleX = 1f
                binding.cameraInner.scaleY = 1f
            } .start()

            capturePhoto()
        }

        binding.zoomLayout.setOnClickListener {
            zoomRatio = when (zoomRatio) {
                1f -> {
                    binding.zoomInner.setImageResource(R.drawable.vector_zoom_out)
                    binding.zoomLabel.text = "2x"
                    2f
                }
                else -> {
                    binding.zoomInner.setImageResource(R.drawable.vector_zoom_in)
                    binding.zoomLabel.text = "1x"
                    1f
                }
            }

            camera?.cameraControl?.setZoomRatio(zoomRatio)
            autoFocus(null)
        }


        binding.cameraxViewFinder.setOnTouchListener { view, motionEvent ->
            autoFocus(motionEvent)
            true
        }

        binding.flashLayout.setOnClickListener { toggleFlashMode() }
        setupFlashChips()

        binding.aspectRatioLayout.setOnClickListener { toggleAspectRatio() }

        binding.previewImageCard.setOnClickListener {
            Intent().run {
                action = Intent.ACTION_VIEW
                setDataAndType(uri, "image/*")
                startActivity(this)
            }
        }

        when {
            !fileIO.hasPermissions(this) -> requestFileIOPermissions()
            !cameraIO.hasPermissions(this) -> requestCameraIOPermissions()
            else -> initialiseCamera(aspectRatio)
        }
    }

    private fun createCacheFile(): File?{
        return when (val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)) {
            null -> null
            else -> {
                File.createTempFile("stracka_temp_capture", ".jpg", storageDir).apply {
                    cacheFile = this
                }
            }
        }
    }

    private fun requestFileIOPermissions(){
        fileIO.requestPermissions { granted ->
            when {
                granted -> if(!cameraIO.hasPermissions(this)) requestCameraIOPermissions()
                else -> snack("External Storage permission required")
            }
        }
    }

    private fun requestCameraIOPermissions(){
        cameraIO.requestPermissions { granted ->
            when {
                granted -> initialiseCamera(aspectRatio)
                else -> snack("Camera permission required")
            }
        }
    }

    private fun snack(message: String) = Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()

    private fun exportImage(image: Bitmap, onExported: (uri: Uri?, error: String?) -> Unit){

        val values = ContentValues()

        val now = OffsetDateTime.now()
        val filename = "stracka_desqueezed_${now.toEpochSecond()}.jpg"

        values.put(MediaStore.Images.Media.TITLE, filename)
        values.put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        values.put(MediaStore.Images.Media.DATE_ADDED, now.toEpochSecond())
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Oppen")
            values.put(MediaStore.Images.Media.IS_PENDING, true)
        }

        val collection = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            else -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        uri = this.contentResolver.insert(collection, values)

        uri?.let {
            val useLetterbox = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("use_letterbox", false)
            when {
                useLetterbox -> {
                    val bitmapWidth = image.width
                    val bitmapHeight = image.height
                    val frameHeight = (bitmapWidth/16) * 9
                    val letterBoxed = Bitmap.createBitmap(bitmapWidth, frameHeight, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(letterBoxed)
                    when (PreferenceManager.getDefaultSharedPreferences(this).getString("letterbox_colour", "black")) {
                        "black" -> canvas.drawColor(Color.BLACK)
                        else -> canvas.drawColor(Color.WHITE)
                    }
                    canvas.drawBitmap(image, 0f, (frameHeight - bitmapHeight)/2f, Paint())
                    this.contentResolver.openOutputStream(uri!!)?.use { outputStream ->
                        outputStream.use { os ->
                            letterBoxed.compress(Bitmap.CompressFormat.JPEG, 100, os)
                        }
                    }
                    letterBoxed.recycle()
                }
                else -> {
                    this.contentResolver.openOutputStream(uri!!)?.use { outputStream ->
                        outputStream.use { os ->
                            image.compress(Bitmap.CompressFormat.JPEG, 100, os)
                        }
                    }
                }
            }

            values.clear()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.Images.Media.IS_PENDING, false)
                this.contentResolver.update(uri!!, values, null, null)
            }

            onExported(uri, null)
        } ?: run {
            onExported(null, "MediaStore error: could not export image")
        }
    }

    private fun toggleAspectRatio(){
        aspectRatio = when (aspectRatio) {
            RATIO_16_9 -> RATIO_4_3
            else -> RATIO_16_9
        }

        initialiseCamera(aspectRatio)
    }

    private fun initialiseCamera(aspectRatio: Int) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            val preview = Preview.Builder()
                .setTargetAspectRatio(aspectRatio)
                .build()
            preview.setSurfaceProvider(binding.cameraxViewFinder.surfaceProvider)

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setTargetAspectRatio(aspectRatio)
                .build()

            imageCapture?.flashMode = ImageCapture.FLASH_MODE_OFF

            when(aspectRatio){
                RATIO_16_9 -> binding.aspectRatioLabel.text = "2.40:1"
                RATIO_4_3 -> binding.aspectRatioLabel.text = "16:9"
            }

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
                autoFocus(null)
            } catch(exc: Exception) {
                println("Stracka camera bind exception: $exc")
                mainThread {
                    snack("Stracka camera bind exception: $exc")
                }
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun capturePhoto(){
        //Shutter flash
        shutter()

        val imageCapture = imageCapture ?: return
        val cacheFile = createCacheFile()

        if(cacheFile == null){
            snack("Could not create cache file for photo")
            return
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(cacheFile).build()
        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    println("Stracka cameraX capture exception: ${exc.message}")
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    println("Stracka cameraX image saved, processing image")
                    processCacheFile()
                }
            })
    }

    private fun processCacheFile(){

        val scaleFactor = PreferenceManager.getDefaultSharedPreferences(this).getString("horizontal_scale_factor", "1.33")!!.toFloat()
        val useNativeToolkit = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("use_native_toolkit", true)

        imageProcessor.processBitmap(cacheFile, scaleFactor, useNativeToolkit) { scaledBitmap ->
            mainThread {
                bitmap = scaledBitmap
                bitmap?.let{
                    binding.previewImage.setImageBitmap(bitmap)

                    exportImage(bitmap!!){ uri,  error ->
                        when {
                            error != null -> println("Stracka save capture error: $error")
                            else -> println("Stracka save capture uri: $uri")
                        }
                    }
                }

            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.root.postDelayed({
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, binding.root).let { controller ->
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }, 500L)

        val scaleFactor = PreferenceManager.getDefaultSharedPreferences(this).getString("horizontal_scale_factor", "1.33")!!.toFloat()
        binding.cameraxViewFinder.scaleX = scaleFactor

        if(getBooleanPref("is_first_run", true)){
            putBooleanPref("is_first_run", false)
            FirstRunDialog(this).show()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                capturePhoto()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun toggleFlashMode(){
        if(binding.flashGroup.isVisible){
            binding.flashGroup.animate().alpha(0f).withEndAction {
                binding.flashGroup.hide()
            }.duration = 250
        }else{
            binding.flashGroup.show()
            binding.flashGroup.alpha = 0f
            binding.flashGroup.animate().alpha(1f).duration = 250
        }
    }

    private fun setupFlashChips(){
        binding.flashGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.chip_flash_on -> {
                    imageCapture?.flashMode = ImageCapture.FLASH_MODE_ON
                    binding.flashInner.setImageResource(R.drawable.vector_flash_on)
                    toggleFlashMode()
                }
                R.id.chip_flash_off -> {
                    imageCapture?.flashMode = ImageCapture.FLASH_MODE_OFF
                    binding.flashInner.setImageResource(R.drawable.vector_flash_off)
                    toggleFlashMode()
                }
                R.id.chip_flash_auto -> {
                    imageCapture?.flashMode = ImageCapture.FLASH_MODE_AUTO
                    binding.flashInner.setImageResource(R.drawable.vector_flash_auto)
                    toggleFlashMode()
                }
            }
        }
    }

    private fun shutter(){
        binding.shutter.alpha = 0f
        binding.shutter.show()

        binding.shutter.animate().alpha(1f).setDuration(50L).setListener(AnimationEndListener{
            hideShutter()
        }).start()
    }

    private fun hideShutter(){
        binding.shutter.animate().alpha(1f).setDuration(100L).setListener(AnimationEndListener{
            binding.shutter.hide()
        }).start()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun autoFocus(event: MotionEvent?){
        val factory: MeteringPointFactory = SurfaceOrientedMeteringPointFactory(
            binding.cameraxViewFinder.width.toFloat(), binding.cameraxViewFinder.height.toFloat()
        )

        val x = event?.x ?: binding.cameraxViewFinder.width/2f
        val y = event?.y ?: binding.cameraxViewFinder.height/2f

        val autoFocusPoint = factory.createPoint(x, y)
        try {
            camera?.cameraControl?.startFocusAndMetering(
                FocusMeteringAction.Builder(autoFocusPoint, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AWB).apply {
                    disableAutoCancel()
                }.build()
            )
        } catch (e: CameraInfoUnavailableException) {
            println("Stracka cannot access camera: $e")
        }
    }
}