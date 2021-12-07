package orllewin.lento

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import androidx.core.view.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.PreferenceManager
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import orllewin.extensions.*
import orllewin.file_io.CameraIO
import orllewin.file_io.OppenFileIO
import orllewin.lento.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding

    private var fileIO = OppenFileIO()
    private var cameraIO = CameraIO()
    private lateinit var imageProcessor: AnamorphicPhotoProcessor

    private var uri: Uri? = null

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    private var camera: Camera? = null

    //Level/Sensor
    private var sensorManager: SensorManager? = null
    private var gravityValues: FloatArray? = null
    private var magneticValues: FloatArray? = null
    private var levelListener: SensorEventListener? = null

    private var skiss: LevelSkiss? =null

    lateinit var config: CameraConfig

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        config = CameraConfig.get(this)

        cameraExecutor = Executors.newSingleThreadExecutor()

        fileIO.register(this)
        cameraIO.register(this)
        imageProcessor = AnamorphicPhotoProcessor(this, lifecycleScope)

        binding = ActivityMainBinding.inflate(layoutInflater)
        binding.viewmodel = viewModel
        setContentView(binding.root)

        lifecycleScope.launch {

            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch{
                    viewModel.hudMessageFlow.collect { hudMessage ->
                        binding.hudMessage.text = hudMessage
                    }
                }
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

        binding.overflowContainer.setOnClickListener {
            OverflowMenu(
                context = this,
                view = binding.overflow,
                timerActive = viewModel.isTimerActive,
                levelActive = skiss?.drawLevel ?: false,
                gridActive = skiss?.drawGrid ?: false,
                {
                    //onTogglelevel
                    skiss?.let {
                        when {
                            skiss!!.drawLevel -> {
                                skiss?.drawLevel = false
                                stopLevel()
                            }
                            else -> {
                                skiss?.drawLevel = true
                                startLevel()
                            }
                        }
                    }
                }, {
                    //onToggleTimer
                    viewModel.timerActive(!viewModel.isTimerActive)
                    when {
                        viewModel.isTimerActive -> binding.timerIcon.show()
                        else -> binding.timerIcon.hide()
                    }
                }, {
                    //onToggleGrid
                    skiss?.let {
                        skiss!!.drawGrid = !skiss!!.drawGrid
                    }

                }).show()
        }

        setupCameraMode()

        updateZoomLevelUI()
        binding.zoomLayout.setOnClickListener {
            when (config.zoomLevel) {
                1 -> config.setZoomLevel(this, 2)
                else -> config.setZoomLevel(this, 1)
            }

            updateZoomLevelUI()

            camera?.cameraControl?.setZoomRatio(config.zoomLevel.toFloat())
            autoFocus(null)
        }

        binding.cameraxViewFinder.setOnTouchListener { _, motionEvent ->
            autoFocus(motionEvent)
            true
        }

        binding.borderLayout.setOnClickListener { toggleBorderSelect() }
        setupBorderChips()

        updateLutModel()
        binding.filmLayout.setOnClickListener {
            FilmSelectionDialog(this){ resId, label ->
                config.setLut(this, resId, label ?: "")
                updateLutModel()
            }.show()
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
            else -> initialiseCamera()
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
                granted -> initialiseCamera()
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
        if(config.aspectRatioFlag == RATIO_4_3){
            config.setAspectRatio(this, RATIO_16_9)
        }else{
            config.setAspectRatio(this, RATIO_4_3)
        }

        initialiseCamera()
    }

    private fun initialiseCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            println("aspectRatio initialiseCamera()")
            config.logAspectRatio()

            val preview = Preview.Builder()
                .setTargetAspectRatio(config.aspectRatioFlag)
                .build()
            preview.setSurfaceProvider(binding.cameraxViewFinder.surfaceProvider)

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setTargetAspectRatio(config.aspectRatioFlag)
                .build()

            imageProcessor.setup(imageCapture)

            imageCapture?.flashMode = config.flashMode

            updateAspectRatioLabel()

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
                camera?.cameraControl?.setZoomRatio(config.zoomLevel.toFloat())
                autoFocus(null)
            } catch(exc: Exception) {
                toast("Lento camera bind exception: $exc")
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

        config = CameraConfig.get(this)

        when {
            config.isAnamorphic -> binding.cameraxViewFinder.scaleX = config.anamorphicScaleFactor
            else -> binding.cameraxViewFinder.scaleX = 1f
        }

        if(config.isFirstRun){
            config.hasRan(this)
            FirstRunDialog(this).show()
        }

        if(binding.levelSkiss.isVisible) startLevel()

        //Check if user has changed the 'hide anamorphic' settings
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        val anamorphicScaleFactor = prefs.getString("horizontal_scale_factor", "1.33")!!.toFloat()
        config.anamorphicScaleFactor = anamorphicScaleFactor
        imageProcessor.scaleFactor = anamorphicScaleFactor
        imageProcessor.useNativeToolkit = prefs.getBoolean("use_native_toolkit", true)

        val hideAnamorphicFeatures = prefs.getBoolean("hide_anamorphic_switch", false)
        config.setHideAnamnorphicFeatures(this, hideAnamorphicFeatures)
        when {
            hideAnamorphicFeatures -> {
                binding.modeSwitch.hide()
                binding.borderContainer.hide()
                setupStandardMode()
            }
            else -> {
                binding.modeSwitch.show()
                binding.borderContainer.show()
            }
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

    private fun setupCameraMode(){
        when {
            config.isAnamorphic -> {
                binding.modeSwitch.text = "Anamorphic"
                binding.modeSwitch.isChecked = true
                binding.cameraxViewFinder.scaleX = config.anamorphicScaleFactor
                imageProcessor.doDesqueeze = true
            }
            else -> setupStandardMode()
        }
        binding.modeSwitch.setOnCheckedChangeListener { _, checked ->
            config.setAnamorphic(this, checked)
            when {
                checked -> {
                    binding.modeSwitch.text = "Anamorphic"
                    binding.cameraxViewFinder.scaleX = config.anamorphicScaleFactor
                    imageProcessor.doDesqueeze = true
                }
                else -> {
                    binding.modeSwitch.text = "Standard"
                    binding.cameraxViewFinder.scaleX = 1f
                    imageProcessor.doDesqueeze = false
                }
            }
            updateAspectRatioLabel()
        }
    }

    private fun setupStandardMode() {
        binding.modeSwitch.text = "Standard"
        binding.modeSwitch.isChecked = false
        binding.cameraxViewFinder.scaleX = 1f
        imageProcessor.doDesqueeze = false
    }

    private fun toggleBorderSelect(){
        when {
            binding.borderGroup.isVisible -> {
                binding.borderGroup.animate().alpha(0f).withEndAction {
                    binding.borderGroup.hide()
                }.duration = 250
            }
            else -> {
                binding.borderGroup.show()
                binding.borderGroup.alpha = 0f
                binding.borderGroup.animate().alpha(1f).duration = 250
            }
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
        when(config.borderMode){
            AnamorphicPhotoProcessor.BorderNone -> {
                binding.borderInner.setImageResource(R.drawable.vector_border_none)
                imageProcessor.borderMode = AnamorphicPhotoProcessor.BorderNone
                binding.borderNone.isChecked = true
            }
            AnamorphicPhotoProcessor.BorderBlack -> {
                binding.borderInner.setImageResource(R.drawable.vector_border_black)
                imageProcessor.borderMode = AnamorphicPhotoProcessor.BorderBlack
                binding.borderBlack.isChecked = true
            }
            AnamorphicPhotoProcessor.BorderWhite -> {
                binding.borderInner.setImageResource(R.drawable.vector_border_white)
                imageProcessor.borderMode = AnamorphicPhotoProcessor.BorderWhite
                binding.borderWhite.isChecked = true
            }
        }
        binding.borderGroup.setOnCheckedChangeListener{ _, checkedId ->
            when(checkedId){
                R.id.border_none -> {
                    config.setBorderMode(this, AnamorphicPhotoProcessor.BorderNone)
                    binding.borderInner.setImageResource(R.drawable.vector_border_none)
                    imageProcessor.borderMode = AnamorphicPhotoProcessor.BorderNone
                }
                R.id.border_black -> {
                    config.setBorderMode(this, AnamorphicPhotoProcessor.BorderBlack)
                    binding.borderInner.setImageResource(R.drawable.vector_border_black)
                    imageProcessor.borderMode = AnamorphicPhotoProcessor.BorderBlack
                }
                R.id.border_white -> {
                    config.setBorderMode(this, AnamorphicPhotoProcessor.BorderWhite)
                    binding.borderInner.setImageResource(R.drawable.vector_border_white)
                    imageProcessor.borderMode = AnamorphicPhotoProcessor.BorderWhite
                }
            }
            toggleBorderSelect()
        }
    }

    private fun setupFlashChips(){
        when(config.flashMode){
            ImageCapture.FLASH_MODE_ON -> {
                binding.flashInner.setImageResource(R.drawable.vector_flash_on)
                binding.chipFlashOn.isChecked = true
            }
            ImageCapture.FLASH_MODE_OFF -> {
                binding.flashInner.setImageResource(R.drawable.vector_flash_off)
                binding.chipFlashOff.isChecked = true
            }
            ImageCapture.FLASH_MODE_AUTO -> {
                binding.flashInner.setImageResource(R.drawable.vector_flash_auto)
                binding.chipFlashAuto.isChecked = true
            }
        }

        binding.flashGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.chip_flash_on -> {
                    config.setFlashMode(this, ImageCapture.FLASH_MODE_ON)
                    binding.flashInner.setImageResource(R.drawable.vector_flash_on)
                }
                R.id.chip_flash_off -> {
                    config.setFlashMode(this, ImageCapture.FLASH_MODE_OFF)
                    binding.flashInner.setImageResource(R.drawable.vector_flash_off)
                }
                R.id.chip_flash_auto -> {
                    config.setFlashMode(this, ImageCapture.FLASH_MODE_AUTO)
                    binding.flashInner.setImageResource(R.drawable.vector_flash_auto)
                }
            }
            imageCapture?.flashMode = config.flashMode
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
            println("Lento cannot access camera: $e")
        }
    }

    private fun startLevel(){

        skiss = LevelSkiss(binding.levelSkiss)
        skiss?.start()

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        val accelerometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        levelListener = object: SensorEventListener{
            override fun onSensorChanged(event: SensorEvent?) {

                if(event == null) return

                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) gravityValues = event.values
                if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) magneticValues = event.values
                if (gravityValues != null &&  magneticValues != null) {
                    val R = FloatArray(9)
                    val I = FloatArray(9)
                    if(SensorManager.getRotationMatrix(R, I, gravityValues, magneticValues)){
                        val orientation = FloatArray(3)
                        SensorManager.getOrientation(R, orientation)
                        //val azimuth = orientation[0]
                        val pitch = orientation[1]
                        //val roll = orientation[2]
                        val pitchDegrees = Math.toDegrees(pitch.toDouble())
                        skiss?.setDegrees(pitchDegrees)
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, p1: Int) = Unit

        }

        sensorManager?.registerListener(levelListener, accelerometer, SensorManager.SENSOR_DELAY_UI);
        sensorManager?.registerListener(levelListener, magnetometer, SensorManager.SENSOR_DELAY_UI);

    }

    private fun stopLevel(){
        sensorManager?.unregisterListener(levelListener)
    }

    private fun updateZoomLevelUI(){
        if(config.zoomLevel == 1){
            binding.zoomInner.setImageResource(R.drawable.vector_zoom_in)
            binding.zoomLabel.text = "1x"
        }else{
            binding.zoomInner.setImageResource(R.drawable.vector_zoom_out)
            binding.zoomLabel.text = "2x"
        }
    }
    private fun updateAspectRatioLabel(){
        when {
            config.isAnamorphic -> {
                when (config.aspectRatioFlag) {
                    RATIO_4_3 -> binding.aspectRatioLabel.text = "16:9"
                    RATIO_16_9 -> binding.aspectRatioLabel.text = "2.4:1"
                    else -> binding.aspectRatioLabel.text = "Unknown ratio"
                }
            }
            else -> {
                when (config.aspectRatioFlag) {
                    RATIO_4_3 -> binding.aspectRatioLabel.text = "4:3"
                    RATIO_16_9 -> binding.aspectRatioLabel.text = "16:9"
                    else -> binding.aspectRatioLabel.text = "Unknown ratio"
                }
            }
        }
    }

    private fun updateLutModel(){
        imageProcessor.filmResource = config.lutResource
        imageProcessor.filmLabel = config.lutLabel
        binding.selectedFilmLabel.text = config.lutLabel
    }

    override fun onPause() {
        super.onPause()

        if(binding.levelSkiss.isVisible) stopLevel()
    }
}