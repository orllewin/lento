package orllewin.tirwedd

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.android.renderscript.Toolkit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.time.OffsetDateTime

class AnamorphicPhotoProcessor(context: Context, private val lifecycleScope: LifecycleCoroutineScope) {

    val exportedPreviewStateFlow = MutableStateFlow<Pair<Uri?, Bitmap?>?>(null)
    val errorStateFlow = MutableStateFlow<String?>(null)

    private val cacheDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    private val executor = ContextCompat.getMainExecutor(context)
    private val contentResolver = context.contentResolver

    val scaleFactor = 1.33f
    var useNativeToolkit = true
    var useLetterbox = true
    var letterBoxColour = Color.WHITE


    private var imageCapture: ImageCapture? = null

    fun setup(imageCapture: ImageCapture?){
        this.imageCapture = imageCapture
    }

    fun capturePhoto(){

        if(imageCapture == null){
            errorStateFlow.value = "ImageCapture not initialised"
            return
        }

        val cacheFile = File.createTempFile("tirwedd_temp_capture", ".jpg", cacheDir)

        val outputOptions = ImageCapture.OutputFileOptions.Builder(cacheFile).build()
        imageCapture!!.takePicture(
            outputOptions, executor, object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    errorStateFlow.value = "Tirwedd cameraX capture exception: ${exc.message}"
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    processCacheFile(cacheFile)
                }
            })
    }

    private fun processCacheFile(file: File){
        processBitmap(file, scaleFactor, useNativeToolkit) { desqueezedBitmap ->
            exportImage(desqueezedBitmap){ uri, error ->
                when {
                    error != null -> errorStateFlow.value = "Tirwedd save capture error: $error"
                    else -> {
                        println("Tirwedd save capture uri: $uri")
                        //todo - create smaller image with correct ratio...
                        val previewBitmap = Bitmap.createScaledBitmap(desqueezedBitmap, 200, 100, true)
                        exportedPreviewStateFlow.value = Pair(uri, previewBitmap)
                    }
                }
            }
        }
    }

    private fun exportImage(image: Bitmap, onExported: (uri: Uri?, error: String?) -> Unit){

        val values = ContentValues()

        val now = OffsetDateTime.now()

        val filename = "tirwedd_${deviceName()}_${now.toEpochSecond()}.jpg"

        values.put(MediaStore.Images.Media.TITLE, filename)
        values.put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        values.put(MediaStore.Images.Media.DATE_ADDED, now.toEpochSecond())
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Oppen")
            values.put(MediaStore.Images.Media.IS_PENDING, true)
        }

        val collection = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> MediaStore.Images.Media.getContentUri(
                MediaStore.VOLUME_EXTERNAL)
            else -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val uri = contentResolver.insert(collection, values)

        uri?.let {
            when {
                useLetterbox -> {
                    val bitmapWidth = image.width
                    val bitmapHeight = image.height
                    val frameHeight = (bitmapWidth/16) * 9
                    val letterBoxed = Bitmap.createBitmap(bitmapWidth, frameHeight, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(letterBoxed)
                    canvas.drawColor(letterBoxColour)
                    canvas.drawBitmap(image, 0f, (frameHeight - bitmapHeight)/2f, Paint())
                    this.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.use { os ->
                            letterBoxed.compress(Bitmap.CompressFormat.JPEG, 100, os)
                        }
                    }
                    letterBoxed.recycle()
                }
                else -> {
                    this.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.use { os ->
                            image.compress(Bitmap.CompressFormat.JPEG, 100, os)
                        }
                    }
                }
            }

            values.clear()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.Images.Media.IS_PENDING, false)
                this.contentResolver.update(uri, values, null, null)
            }

            onExported(uri, null)
        } ?: run {
            onExported(null, "MediaStore error: could not export image")
        }
    }


    /**
     * Desqueezes a photo from a File
     */
    fun processBitmap(file: File, scale: Float, useNativeToolkit: Boolean, onDesqueezed: (desqueezed: Bitmap) -> Unit){
        lifecycleScope.launch(Dispatchers.IO) {
            file.inputStream().use { inputStream ->
                val squeezedBitmap = BitmapFactory.decodeStream(inputStream)
                val targetWidth = (squeezedBitmap.width * scale).toInt()
                val targetHeight = squeezedBitmap.height

                if(useNativeToolkit){
                    val desqueezedBitmap = Toolkit.resize(squeezedBitmap, targetWidth, targetHeight)
                    squeezedBitmap.recycle()
                    onDesqueezed(desqueezedBitmap)
                }else{
                    val desqueezedBitmap = Bitmap.createScaledBitmap(squeezedBitmap, targetWidth, targetHeight, true)
                    squeezedBitmap.recycle()
                    onDesqueezed(desqueezedBitmap)
                }
            }
        }
    }

    fun processBitmap(resolver: ContentResolver, uri: Uri, scale: Float, useNativeToolkit: Boolean, onDesqueezed: (desqueezed: Bitmap) -> Unit){
        lifecycleScope.launch(Dispatchers.IO) {
            resolver.openInputStream(uri).use { inputStream ->
                val squeezedBitmap = BitmapFactory.decodeStream(inputStream)
                val targetWidth = (squeezedBitmap.width * scale).toInt()
                val targetHeight = squeezedBitmap.height

                if(useNativeToolkit){
                    val desqueezedBitmap = Toolkit.resize(squeezedBitmap, targetWidth, targetHeight)
                    squeezedBitmap.recycle()
                    onDesqueezed(desqueezedBitmap)
                }else{
                    val desqueezedBitmap = Bitmap.createScaledBitmap(squeezedBitmap, targetWidth, targetHeight, true)
                    squeezedBitmap.recycle()
                    onDesqueezed(desqueezedBitmap)
                }

            }
        }
    }

    private fun deviceName(): String = Build.MODEL.lowercase().replace(" ", "_")
}