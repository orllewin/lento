package orllewin.skiss


import android.content.res.Resources
import android.graphics.Color
import android.util.Log
import androidx.annotation.ColorInt
import kotlin.random.Random

val screenWidth = Resources.getSystem().displayMetrics.widthPixels
val screenHeight = Resources.getSystem().displayMetrics.heightPixels

fun random(start: Number, end: Number): Float{
    return Random.nextLong(start.toLong(), end.toLong()).toFloat()
}

fun random(number: Number): Float{
    if(number.toFloat() <= 0) return 0F
    return Random.nextLong(0L, number.toLong()).toFloat()
}

@ColorInt
fun color(color: String): Int = Color.parseColor(color)

@ColorInt
fun invertColor(@ColorInt color: Int): Int =
    Color.rgb(255 - Color.red(color), 255 - Color.green(color), 255 - Color.blue(color))

@ColorInt
fun saturate(color: Int, saturation: Double): Int {
    // Values from: https://www.w3.org/TR/filter-effects-1/#feColorMatrixElement , type="saturate"
    val red = (0.213 + 0.787 * saturation) * Color.red(color) + (0.715 - 0.715 * saturation) * Color.green(color) + (0.072 - 0.072 * saturation) * Color.blue(color)
    val green = (0.213 - 0.213 * saturation) * Color.red(color) + (0.715 + 0.285 * saturation) * Color.green(color) + (0.072 - 0.072 * saturation) * Color.blue(color)
    val blue = (0.213 - 0.213 * saturation) * Color.red(color) + (0.715 - 0.715 * saturation) * Color.green(color) + (0.072 + 0.928 * saturation) * Color.blue(color)
    return Color.rgb(red.coerceIn(0.0, 255.0).toInt(), green.coerceIn(0.0, 255.0).toInt(), blue.coerceIn(0.0, 255.0).toInt())
}

@ColorInt
fun greyColor(grey: Float): Int{
    val greyInt = grey.toInt()
    return rgb(greyInt, greyInt, greyInt)
}

@ColorInt
fun greyColor(grey: Int): Int = rgb(grey, grey, grey)

@ColorInt
fun rgb(red: Int, green: Int, blue: Int): Int =
    -16777216 or (red.coerceIn(0, 255) shl 16) or (green.coerceIn(0, 255) shl 8) or blue.coerceIn(0, 255)

@ColorInt
fun rgb(red: Float, green: Float, blue: Float): Int =
    -0x1000000 or ((red * 255.0f + 0.5f).toInt() shl 16) or ((green * 255.0f + 0.5f).toInt() shl 8) or (blue * 255.0f + 0.5f).toInt()

fun map(value: Float, start1: Float, stop1: Float, start2: Float, stop2: Float): Float{
    return start2 + (stop2 - start2) * ((value - start1) / (stop1 - start1))
}

//This is an extremely naive implementation
fun lerpColor(startColor: Int, endColor: Int, amount: Float): Int{
    val sR = Color.red(startColor)
    val sG = Color.green(startColor)
    val sB = Color.blue(startColor)

    val eR = Color.red(endColor)
    val eG = Color.green(endColor)
    val eB = Color.blue(endColor)

    val r = lerp(sR.toFloat(), eR.toFloat(), amount)
    val g = lerp(sG.toFloat(), eG.toFloat(), amount)
    val b = lerp(sB.toFloat(), eB.toFloat(), amount)
    return rgbColour(r.toInt(), g.toInt(), b.toInt())
}

private fun lerp(start: Float, end: Float, amount: Float): Float {
    return start * (1-amount) + end * amount
}

fun rgbColour(r: Int, g: Int, b: Int): Int {
    val red = r shl 16 and 0x00FF0000
    val green = g shl 8 and 0x0000FF00
    val blue = b and 0x000000FF
    return -0x1000000 or red or green or blue
}

fun l(message: String){
    Log.d("SKISS", message)
}

fun e(message: String){
    Log.e("SKISS", message)
}
