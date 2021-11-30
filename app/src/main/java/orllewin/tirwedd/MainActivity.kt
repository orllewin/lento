package orllewin.tirwedd

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.viewModels
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
import kotlinx.coroutines.launch
import orllewin.extensions.*
import orllewin.file_io.CameraIO
import orllewin.file_io.OppenFileIO
import orllewin.tirwedd.databinding.ActivityMainBinding
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
                            binding.shutter.activate()
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
                        toast(error)
                    }
                }
            }
        }

        viewModel.putImageProcessor(imageProcessor)

        binding.overflowContainer.setOnClickListener { OverflowMenu(this, binding.overflow).show() }

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

        binding.borderLayout.setOnClickListener { toggleBorderSelect() }
        setupBorderChips()

        binding.filmLayout.setOnClickListener { toggleFilmSelect() }
        setupFilmChips()

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
                else -> toast("External Storage permission required")
            }
        }
    }

    private fun requestCameraIOPermissions(){
        cameraIO.requestPermissions { granted ->
            when {
                granted -> initialiseCamera(aspectRatio)
                else -> toast("Camera permission required")
            }
        }
    }

    private fun toast(message: String?) = mainThread {
        message?.let{
            println(message)
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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
                toast("Tirwedd camera bind exception: $exc")
            }

            delayed(1250){
                binding.splashIcon.animate().alpha(0f).withEndAction {
                    binding.splashIcon.hide()
                }.duration = 500
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
        }, 100L)

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

    private fun toggleBorderSelect(){
        if(binding.borderGroup.isVisible){
            binding.borderGroup.animate().alpha(0f).withEndAction {
                binding.borderGroup.hide()
            }.duration = 250
        }else{
            binding.borderGroup.show()
            binding.borderGroup.alpha = 0f
            binding.borderGroup.animate().alpha(1f).duration = 250
        }
    }

    private fun toggleFilmSelect(){
        if(binding.filmGroup.isVisible){
            binding.filmGroup.animate().alpha(0f).withEndAction {
                binding.filmGroup.hide()
            }.duration = 250
        }else{
            binding.filmGroup.show()
            binding.filmGroup.alpha = 0f
            binding.filmGroup.animate().alpha(1f).duration = 250
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

    private fun setupBorderChips(){
        binding.borderGroup.setOnCheckedChangeListener{ _, checkedId ->
            when(checkedId){
                R.id.border_none -> {
                    binding.borderInner.setImageResource(R.drawable.vector_border_none)
                    imageProcessor.borderMode = AnamorphicPhotoProcessor.Border.None
                }
                R.id.border_black -> {
                    binding.borderInner.setImageResource(R.drawable.vector_border_black)
                    imageProcessor.borderMode = AnamorphicPhotoProcessor.Border.Black
                }
                R.id.border_white -> {
                    binding.borderInner.setImageResource(R.drawable.vector_border_white)
                    imageProcessor.borderMode = AnamorphicPhotoProcessor.Border.White
                }
            }
            toggleBorderSelect()
        }
    }

    private fun setupFilmChips(){
        binding.filmGroup.setOnCheckedChangeListener{ _, checkedId ->
            when(checkedId){
                R.id.film_none -> imageProcessor.filmResource = null
                R.id.film_delta_400 -> imageProcessor.filmResource = R.raw.ilford_delta_400
                R.id.film_fp_3000b -> imageProcessor.filmResource = R.raw.fuji_fp_3000b
            }
            toggleFilmSelect()
        }
    }

    private fun setupFlashChips(){
        binding.flashGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.chip_flash_on -> {
                    imageCapture?.flashMode = ImageCapture.FLASH_MODE_ON
                    binding.flashInner.setImageResource(R.drawable.vector_flash_on)
                }
                R.id.chip_flash_off -> {
                    imageCapture?.flashMode = ImageCapture.FLASH_MODE_OFF
                    binding.flashInner.setImageResource(R.drawable.vector_flash_off)
                }
                R.id.chip_flash_auto -> {
                    imageCapture?.flashMode = ImageCapture.FLASH_MODE_AUTO
                    binding.flashInner.setImageResource(R.drawable.vector_flash_auto)
                }
            }
            toggleFlashMode()
        }
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