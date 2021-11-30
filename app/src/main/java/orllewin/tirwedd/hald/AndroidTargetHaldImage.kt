package orllewin.tirwedd.hald

import android.content.Context
import android.graphics.Bitmap
import orllewin.file_io.ImageIO
import orllewin.haldclut.HaldImage
import java.io.File

class AndroidTargetHaldImage(val file: File): HaldImage() {

    var bitmap: Bitmap? = null

    private val imageIO = ImageIO()

    private var rowBitmap: Bitmap? = null
    private var rowIndex = -1

    private var _width = -1
    private var _height = -1

    init {
        bitmap = imageIO.bitmap(file)

        if(bitmap == null) throw Exception("AndroidTargetHaldImage: Could not open bitmap")

        _width = bitmap!!.width
        _height = bitmap!!.height
    }

    override var width: Int
        get() = _width
        set(_) = Unit
    override var height: Int
        get() = _height
        set(_) = Unit

    override fun getPixel(x: Int, y: Int): Int {
        return bitmap!!.getPixel(x, y)
    }
    override fun setPixel(x: Int, y: Int, colour: Int) {
        try {
            bitmap!!.setPixel(x, y, colour)
        }catch(ise: IllegalStateException){
            println("Exception seting pixel: $x x $y on bitmap of dimens: $_width x $_height")
            throw ise
        }
    }
}