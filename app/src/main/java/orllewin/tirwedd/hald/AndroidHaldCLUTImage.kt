package orllewin.tirwedd.hald

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.RawRes
import orllewin.file_io.ImageIO
import orllewin.haldclut.HaldImage

/**
 * Reads a HaldCLUT image from Android's Raw resource directory
 * Only one row at a time from the bitmap is held in memory to prevent OOM exceptions
 */
class AndroidHaldCLUTImage(private val context: Context, @RawRes val resourceId: Int): HaldImage() {

    var bitmap: Bitmap? = null

    private val imageIO = ImageIO()

    private var _width = -1
    private var _height = -1

    init {
        bitmap = imageIO.bitmap(context, resourceId)

        if(bitmap == null) throw Exception("AndroidHaldCLUTImage: Could not open bitmap")

        _width = bitmap!!.width
        _height = bitmap!!.height
    }

    override var width: Int
        get() = _width
        set(value) = Unit

    override var height: Int
        get() = _height
        set(value) = Unit

    override fun getPixel(x: Int, y: Int): Int {
        return bitmap!!.getPixel(x, y)
    }

    override fun setPixel(x: Int, y: Int, colour: Int) = Unit
}