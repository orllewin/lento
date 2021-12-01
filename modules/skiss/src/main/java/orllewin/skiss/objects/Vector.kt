package orllewin.skiss.objects

import orllewin.skiss.random
import kotlin.math.sqrt

data class Vector(var x: Float, var y: Float) {

    constructor(x: Number, y: Number) : this(x.toFloat(), y.toFloat())
    constructor(coord: Coord) : this(coord.x, coord.y)

    companion object{
        fun dot(v1: Vector, v2: Vector): Float {
            return v1.x * v2.x + v1.y * v2.y
        }

        fun randomDirection(): Vector {
            val direction = Vector(random(-10f, 10f), random(-10f, 10f))
            direction.normalise()
            return direction
        }

        fun randomPosition(width: Number, height: Number): Vector {
            return Vector(
                random(
                    0f,
                    width
                ), random(0f, height)
            )
        }

        fun direction(start: Vector, end: Vector): Vector {
            return Vector(
                end.x - start.x,
                end.y - start.y
            )
        }

        fun dist(a: Vector, b: Vector): Float {
            val dx: Float = a.x - b.x
            val dy: Float = a.y - b.y
            return Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        }

        fun normal(vectorA: Vector, vectorB: Vector): Vector {
            val delta = vectorA - vectorB
            delta.normalize()
            return Vector(-delta.y, delta.x)
        }

        fun empty(): Vector {
            return Vector(0, 0)
        }
    }


    fun reset(){
        x = 0f
        y = 0f
    }

    fun magnitude(): Float{
        return sqrt(x * x + y * y)
    }

    fun normalise(){
        val magnitude = magnitude()
        if(magnitude > 0){
            this.x = x/magnitude
            this.y = y/magnitude
        }
    }

    fun distance(other: Vector): Float{
        val dx = x - other.x
        val dy = y - other.y
        return sqrt(dx * dx + dy * dy)
    }

    fun direction(other: Vector): Vector {
        val direction = Vector(other.x - x, other.y - y)
        direction.normalise()
        return direction
    }

    fun dot(other: Vector): Float {
        return x * other.x + y * other.y
    }

    fun perpendicular(): Vector {
        val pX = -y
        val pY = x
        return Vector(pX, pY)
    }

    fun normalize() = normalise()

    fun limit(max: Float){
        when {
            magnitudeSquared() > max * max -> {
                normalise()
                x *= max
                y *= max
            }
        }
    }

    fun magnitudeSquared(): Float {
        return x * x + y * y
    }

    fun coord(): Coord =
        Coord(x, y)

    operator fun plus(vector: Vector): Vector {
        return Vector(
            this.x + vector.x,
            this.y + vector.y
        )
    }

    operator fun minus(vector: Vector): Vector {
        return Vector(
            this.x - vector.x,
            this.y - vector.y
        )
    }

    operator fun times(value: Number): Vector {
        return Vector(
            this.x * value.toFloat(),
            this.y * value.toFloat()
        )
    }

    operator fun div(value: Number): Vector {
        return Vector(
            this.x / value.toFloat(),
            this.y / value.toFloat()
        )
    }

    fun clone(): Vector {
        return Vector(x, y)
    }
}