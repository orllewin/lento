package orllewin.lento

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
import androidx.core.graphics.drawable.RoundedBitmapDrawable
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.android.renderscript.Toolkit
import kotlinx.coroutines.flow.MutableStateFlow
import orllewin.lento.lut.Lut
import orllewin.lento.lut.UnrealLutToolkit
import java.io.File
import java.time.OffsetDateTime

class AnamorphicPhotoProcessor(val context: Context, private val lifecycleScope: LifecycleCoroutineScope) {

    val exportedPreviewStateFlow = MutableStateFlow<Pair<Uri?, RoundedBitmapDrawable?>?>(null)
    val capturePreviewStateFlow = MutableStateFlow<Bitmap?>(null)
    val errorStateFlow = MutableStateFlow<String?>(null)

    private val cacheDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    private val executor = ContextCompat.getMainExecutor(context)
    private val contentResolver = context.contentResolver

    val toolkit = UnrealLutToolkit()

    var scaleFactor = 1.33f
    var useNativeToolkit = true
    var doDesqueeze: Boolean = true
    var filmResource: Int? = null
    var filmLabel: String? = null

    companion object{
        const val BorderNone = 0
        const val BorderBlack = 1
        const val BorderWhite = 2
    }

    var borderMode = BorderNone


    private var imageCapture: ImageCapture? = null

    fun setup(imageCapture: ImageCapture?){
        this.imageCapture = imageCapture
    }

    //1. Take the photo
    fun capturePhoto(){

        if(imageCapture == null){
            errorStateFlow.value = "ImageCapture not initialised"
            return
        }

        val cacheFile = File.createTempFile("lento_temp_capture", ".jpg", cacheDir)

        val outputOptions = ImageCapture.OutputFileOptions.Builder(cacheFile).build()
        imageCapture?.takePicture(
            outputOptions, executor, object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    errorStateFlow.value = "Lento cameraX capture exception: ${exc.message}"
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    resize(cacheFile)
                }
            })
    }

    private fun applyLut(source: Bitmap): Bitmap?{
        return when {
            filmResource != null && filmResource != -1 -> {
                toolkit.loadLut(context, Lut(filmLabel!!, filmResource!!, false))
                val filtered = toolkit.process(source)
                source.recycle()
                filtered
            }
            else -> source
        }
    }

    //3. Resize
    private fun resize(file: File){
        when {
            doDesqueeze -> {
                desqueeze(file, scaleFactor, useNativeToolkit) { desqueezedBitmap ->
                    createCapturePreview(desqueezedBitmap)
                    val filtered = applyLut(desqueezedBitmap)
                    exportImage(filtered){ uri, error ->
                        when {
                            error != null -> errorStateFlow.value = "Lento save capture error: $error"
                            else -> generatePreview(uri, filtered!!)//todo - add null check
                        }
                    }
                }
            }
            else -> {
                val bitmap = BitmapFactory.decodeStream(file.inputStream())
                createCapturePreview(bitmap)
                val filtered = applyLut(bitmap)
                exportImage(filtered){ uri, error ->
                    when {
                        error != null -> errorStateFlow.value = "Lento save capture error: $error"
                        else -> generatePreview(uri, filtered!!)//todo - add null check
                    }
                }
            }
        }
    }

    private fun createCapturePreview(bitmap: Bitmap){
        val captureWidth = bitmap.width
        val captureHeight = bitmap.height

        val height = captureHeight/(captureWidth/200)
        val capturePreviewBitmap = Bitmap.createScaledBitmap(bitmap, 200, height, true)
        capturePreviewStateFlow.value = capturePreviewBitmap
    }

    //4. Build small preview
    private fun generatePreview(uri: Uri?, bitmap: Bitmap){
        if(uri == null){
            println("generatePreview() missing file Uri")
            return
        }
        println("Lento save capture uri: $uri")
        val filteredWidth = bitmap.width
        val filteredHeight = bitmap.height

        val height = filteredHeight/(filteredWidth/200)
        val previewBitmap = Bitmap.createScaledBitmap(bitmap, 200, height, true)
        val rounded = RoundedBitmapDrawableFactory.create(context.resources, previewBitmap)
        rounded.isCircular = true
        exportedPreviewStateFlow.value = Pair(uri, rounded)

        bitmap.recycle()
    }

    private fun exportImage(image: Bitmap?, onExported: (uri: Uri?, error: String?) -> Unit){

        if(image == null) {
            onExported(null, "Null Bitmap - did LUT process fail?")
            return
        }

        val values = ContentValues()

        val now = OffsetDateTime.now()

        val filename = when (filmLabel) {
            null -> "lento_${deviceName()}_${now.toEpochSecond()}.jpg"
            else -> "lento_${filmLabel!!.lowercase().replace(" ", "_")}_${deviceName()}_${now.toEpochSecond()}.jpg"
        }

        values.put(MediaStore.Images.Media.TITLE, filename)
        values.put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        values.put(MediaStore.Images.Media.DATE_ADDED, now.toEpochSecond())
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Lento")
            values.put(MediaStore.Images.Media.IS_PENDING, true)
        }

        val collection = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            else -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val uri = contentResolver.insert(collection, values)

        uri?.let {
            if(borderMode == BorderNone){
                this.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.use { os ->
                        image.compress(Bitmap.CompressFormat.JPEG, 100, os)
                    }
                }
            }else{
                val bitmapWidth = image.width
                val bitmapHeight = image.height
                val frameHeight = (bitmapWidth/16) * 9
                val letterBoxed = Bitmap.createBitmap(bitmapWidth, frameHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(letterBoxed)

                val color = when (borderMode) {
                    BorderWhite -> Color.WHITE
                    else -> Color.BLACK
                }

                canvas.drawColor(color)
                canvas.drawBitmap(image, 0f, (frameHeight - bitmapHeight)/2f, Paint())
                this.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.use { os ->
                        letterBoxed.compress(Bitmap.CompressFormat.JPEG, 100, os)
                    }
                }
                letterBoxed.recycle()
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

    private fun desqueeze(file: File, scale: Float, nativeToolkit: Boolean, onDesqueezed: (desqueezed: Bitmap) -> Unit){
        file.inputStream().use { inputStream ->
            val squeezedBitmap = BitmapFactory.decodeStream(inputStream)

            val targetWidth = (squeezedBitmap.width * scale).toInt()
            val targetHeight = squeezedBitmap.height

            when {
                nativeToolkit -> {
                    val desqueezedBitmap = Toolkit.resize(squeezedBitmap, targetWidth, targetHeight)
                    squeezedBitmap.recycle()
                    onDesqueezed(desqueezedBitmap)
                }
                else -> {
                    val desqueezedBitmap = Bitmap.createScaledBitmap(squeezedBitmap, targetWidth, targetHeight, true)
                    squeezedBitmap.recycle()
                    onDesqueezed(desqueezedBitmap)
                }
            }
        }
    }


    private fun deviceName(): String = Build.MODEL.lowercase().replace(" ", "_")
}