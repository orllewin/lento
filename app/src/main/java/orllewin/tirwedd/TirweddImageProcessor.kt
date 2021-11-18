package orllewin.tirwedd

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.android.renderscript.Toolkit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class TirweddImageProcessor(private val lifecycleScope: LifecycleCoroutineScope) {
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
}