package orllewin.tirwedd

import android.graphics.Canvas
import android.graphics.Color
import orllewin.skiss.Skiss
import orllewin.skiss.SkissView
import orllewin.skiss.objects.Vector
import kotlin.math.cos
import kotlin.math.sin

class LevelSkiss (view: SkissView): Skiss(view) {

    var degrees: Double? = null

    lateinit var center: Vector

    override fun setup(width: Int, height: Int) {
        stroke(Color.WHITE)
        strokeWeight(5)

        center = Vector(width/2, height/2)
    }

    override fun update(canvas: Canvas) {
        degrees?.let{
            val x1: Double = center.x + (cos(Math.toRadians(degrees!!)) * 200)
            val y1: Double = center.y + (sin(Math.toRadians(degrees!!)) * 200)
            line(center.x, center.y, x1, y1)

            val x2: Double = center.x - (sin(Math.toRadians(90-degrees!!)) * 200)
            val y2: Double = center.y - (cos(Math.toRadians(90-degrees!!)) * 200)
            line(center.x, center.y, x2, y2)
        }

        line(0, center.y, 50, center.y)
        line(width, center.y, width-50, center.y)
    }

    fun setDegrees(degrees: Double){
        this.degrees = degrees
    }
}