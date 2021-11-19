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
import android.widget.Toast
import androidx.activity.viewModels
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.PreferenceManager
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import oppen.stracka.AnimationEndListener
import orllewin.extensions.*
import orllewin.file_io.CameraIO
import orllewin.file_io.OppenFileIO
import orllewin.tirwedd.databinding.ActivityMainBinding
import java.time.OffsetDateTime
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding

    private var aspectRatio = RATIO_16_9

    private var fileIO = OppenFileIO()
    private var cameraIO = CameraIO()
    private lateinit var imageProcessor: AnamorphicPhotoProcessor

    private var uri: Uri? = null

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
        imageProcessor = AnamorphicPhotoProcessor(this, lifecycleScope)

        binding = ActivityMainBinding.inflate(layoutInflater)
        binding.viewmodel = viewModel
        setContentView(binding.root)

        lifecycleScope.launch {

            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.takePhotoStateFlow.asStateFlow().collect { captureTrigger ->
                        if (captureTrigger) {
                            binding.previewProgress.show()
                            shutter()
                            binding.cameraInner.animate().scaleXBy(0.2f).scaleYBy(0.2f)
                                .setDuration(250).withEndAction {
                                    binding.cameraInner.scaleX = 1f
                                    binding.cameraInner.scaleY = 1f
                                }.start()
                        }
                    }
                }

                launch{
                    imageProcessor.exportedPreviewStateFlow.asStateFlow().collect { preview ->
                        preview?.let {
                            mainThread {
                                uri = preview.first
                                binding.previewImage.setImageBitmap(preview.second)
                                binding.previewProgress.hide()
                            }
                        }
                    }
                }

                launch{
                    imageProcessor.errorStateFlow.asStateFlow().collect { error ->
                        error?.let {
                            mainThread {
                                Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }

        viewModel.putImageProcessor(imageProcessor)

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

            imageProcessor.setup(imageCapture)

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
                viewModel.capturePhoto()
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