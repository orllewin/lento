package orllewin.haldclut

import kotlin.math.pow
import kotlin.math.roundToInt

class HaldClut(
    private val haldImage: HaldImage,
    private val destImage: HaldImage){

    private var level: Int = 0
    private var cubeSize: Int = 0
    private var depth: Int

    init {
        if(haldImage.width == haldImage.height){
            level = cubicRoot(haldImage.width)
            println("HaldCLUT Level: $level")
            cubeSize = level * level

            val side = haldImage.width.toDouble()
            val lutRoot = (side * side).pow(1.0 / 3.0)
            depth = (256.toDouble()/lutRoot).roundToInt()
            println("HaldCLUT Depth: $depth")
        }else{
            throw ArithmeticException("Image must be square")
        }
    }

    fun process(){
        val height = destImage.height
        for(y in 0 until height){
            for(x in 0 until destImage.width){
                val pixel = destImage.getPixel(x, y)
                destImage.setPixel(x, y, getHaldClutPixel(pixel))
            }
        }
    }

    private fun getHaldClutPixel(pixel: Int): Int{
        val red = (pixel shr 16 and 0xFF)
        val green = (pixel shr 8 and 0xFF)
        val blue = (pixel shr 0 and 0xFF)

        val x = red / depth
        val y = green /  depth
        val z = blue / depth

        val hX = x + (y % level * cubeSize)
        val hY = (y / level) + (z * level)

        return haldImage.getPixel(hX, hY)
    }

    private fun cubicRoot(edgeSize:Int): Int {
        return edgeSize.toDouble().pow(1/3.toDouble()).roundToInt()
    }
}