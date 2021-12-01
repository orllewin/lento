package orllewin.skiss

import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import orllewin.skiss.objects.Coord
import orllewin.skiss.objects.Vector

const val TWO_PI = 6.2831855f

abstract class SkissSyntaxBase {

    class SkissPaint(var active: Boolean = false): Paint(){
        fun draw(operation: () -> Unit){
            if(active) operation.invoke()
        }
    }

    private val fillPaint = SkissPaint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val strokePaint = SkissPaint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    var width = -1
    var height = -1

    var xT = 0//x translate
    var yT = 0//y translate

    var canvas: Canvas? = null

    fun translate(x: Int, y: Int){
        xT = x
        yT = y
    }

    fun background(color: Int){
        canvas?.drawColor(color)
    }

    fun smooth(){
        strokePaint.isAntiAlias = true
        fillPaint.isAntiAlias = true
    }

    fun noSmooth(){
        strokePaint.isAntiAlias = false
        fillPaint.isAntiAlias = false
    }

    fun stroke(color: String){
        strokePaint.apply {
            this.color = color(color)
            active = true
        }
    }

    fun strokeWeight(weight: Number){
        strokePaint.apply{
            strokeWidth = weight.toFloat()
        }
    }

    fun stroke(color: Int){
        strokePaint.apply {
            this.color = color
            active = true
        }
    }

    fun fill(color: String){
        fillPaint.apply {
            this.color = color(color)
            active = true
        }
    }

    fun fill(color: Int){
        fillPaint.apply{
            this.color = color
            active = true
        }
    }

    fun fill(color: Int, mode: BlendMode){
        fillPaint.apply{
            this.color = color
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> this.blendMode = mode
            }
            active = true
        }
    }

    fun noFill(){
        fillPaint.active = false
    }

    fun noStroke(){
        strokePaint.active = false
    }

    /**
     *
     * Drawing Operations
     *
     */

    fun line(x1: Number, y1: Number, x2: Number, y2: Number){
        strokePaint.draw {
            canvas?.drawLine(
                xT + x1.toFloat(),
                yT + y1.toFloat(),
                xT + x2.toFloat(),
                yT + y2.toFloat(), strokePaint)
        }
    }

    fun line(x1: Float, y1: Float, x2: Float, y2: Float){
        strokePaint.draw {
            canvas?.drawLine(
                xT + x1,
                yT + y1,
                xT + x2,
                yT + y2, strokePaint)
        }
    }

    fun line(a: Vector, b: Vector){
        strokePaint.draw {
            canvas?.drawLine(
                xT + a.x,
                yT + a.y,
                xT + b.x,
                yT + b.y, strokePaint)
        }
    }

    fun lines(lines: List<Pair<Coord, Coord>>){
        strokePaint.draw {
            canvas?.drawLines(lines.flatMap {
                arrayListOf(it.first.x + xT, it.first.y + yT, it.second.x + xT, it.second.y + yT)
            }.toFloatArray(), strokePaint)
        }
    }

    fun lines(lines: Array<Pair<Coord, Coord>>){
        strokePaint.draw {
            canvas?.drawLines(lines.flatMap {
                arrayListOf(it.first.x + xT, it.first.y + yT, it.second.x + xT, it.second.y + yT)
            }.toFloatArray(), strokePaint)
        }
    }

    fun circle(x: Number, y: Number, radius: Number){
        strokePaint.draw {
            canvas?.drawCircle(xT + x.toFloat(), yT + y.toFloat(), radius.toFloat(), strokePaint)
        }
        fillPaint.draw{
            canvas?.drawCircle(xT + x.toFloat(), yT + y.toFloat(), radius.toFloat(), fillPaint)
        }
    }

    fun circle(x: Float, y: Float, radius: Float){
        strokePaint.draw {
            canvas?.drawCircle(xT + x, yT + y, radius, strokePaint)
        }
        fillPaint.draw{
            canvas?.drawCircle(xT + x, yT + y, radius, fillPaint)
        }
    }

    fun square(x: Number, y: Number, diameter: Number){
        strokePaint.draw {
            canvas?.drawRect(
                xT + x.toFloat(),
                yT + y.toFloat(),
                (x.toFloat() + diameter.toFloat() + xT),
                (y.toFloat() + diameter.toFloat() + yT),  strokePaint)
        }
        fillPaint.draw{
            canvas?.drawRect(
                xT + x.toFloat(),
                yT + y.toFloat(),
                (x.toFloat() + diameter.toFloat() + xT),
                (y.toFloat() + diameter.toFloat() + yT),  fillPaint)
        }
    }

    fun square(x: Float, y: Float, diameter: Float){
        strokePaint.draw {
            canvas?.drawRect(
                xT + x,
                yT + y,
                (x + diameter + xT),
                (y + diameter + yT),  strokePaint)
        }
        fillPaint.draw{
            canvas?.drawRect(
                xT + x,
                yT + y,
                (x + diameter + xT),
                (y + diameter + yT),  fillPaint)
        }
    }

    fun points(points: List<Coord>){
        strokePaint.draw {
            canvas?.drawPoints(points.flatMap {
                arrayListOf(it.x + xT, it.y + yT)
            }.toFloatArray(), strokePaint)
        }
    }

    fun points(points: Array<Coord>){
        strokePaint.draw {
            canvas?.drawPoints(points.flatMap {
                arrayListOf(it.x + xT, it.y + yT)
            }.toFloatArray(), strokePaint)
        }
    }

    fun point(x: Int, y: Int) = point(x.toFloat(), y.toFloat())

    fun point(x: Float, y: Float){
        strokePaint.draw {
            canvas?.drawPoint(xT + x, yT + y, strokePaint)
        }
    }

    fun point(point: Coord){
        strokePaint.draw {
            canvas?.drawPoint(xT + point.x, yT + point.y, strokePaint)
        }
    }
}