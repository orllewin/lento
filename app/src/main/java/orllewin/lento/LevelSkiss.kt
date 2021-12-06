package orllewin.lento

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
    lateinit var levelTarget: Vector
    lateinit var levelLeftEdge: Vector

    private val hudColour = Color.WHITE
    private val levelGoodColour = Color.parseColor("#00dd33")
    private val levelBadColour = Color.parseColor("#CA473F")

    var drawGrid = true
    var drawLevel = true

    override fun setup(width: Int, height: Int) {
        stroke(Color.WHITE)
        strokeWeight(5)

        center = Vector(width/2, height/2)
        levelTarget = Vector((width/2)-200, height/2)
        levelLeftEdge = Vector(-1, -1)
    }

    override fun update(canvas: Canvas) {

        if(drawLevel) {
            strokeWeight(5)

            when {
                levelTarget.distance(levelLeftEdge) < 10 -> stroke(levelGoodColour)
                else -> stroke(levelBadColour)
            }

            degrees?.let {
                val x1: Double = center.x + (cos(Math.toRadians(degrees!!)) * 200)
                val y1: Double = center.y + (sin(Math.toRadians(degrees!!)) * 200)
                line(center.x, center.y, x1, y1)

                val x2: Double = center.x - (sin(Math.toRadians(90 - degrees!!)) * 200)
                val y2: Double = center.y - (cos(Math.toRadians(90 - degrees!!)) * 200)
                levelLeftEdge.x = x2.toFloat()
                levelLeftEdge.y = y2.toFloat()
                line(center.x, center.y, x2, y2)
            }

            stroke(hudColour)
            line(center.x - 250, center.y, center.x - 200, center.y)
            line(center.x + 200, center.y, center.x + 250, center.y)

        }

        if(drawGrid){
            strokeWeight(1)
            stroke(hudColour)
            line(0, height/3, width, height/3)
            line(0, (height/3) * 2, width, (height/3) * 2)
            line(width/3, 0, width/3, height)
            line((width/3) * 2, 0, (width/3) * 2, height)
        }
    }

    fun setDegrees(degrees: Double){
        this.degrees = degrees
    }
}