package orllewin.skiss


import android.graphics.Canvas
import orllewin.skiss.SkissView

abstract class Skiss(private val skissView: SkissView): SkissSyntaxBase() {

    init {

        skissView.setup{ width, height ->
            this.width = width
            this.height = height
            setup(width, height)
        }

        skissView.update { canvas ->
            this.canvas = canvas
            if(canvas != null) update(canvas)
        }

        skissView.onTouch { x, y ->
            onTouch(x, y)
        }

        skissView.setOnLongClickListener {
            onLongClick()
            true
        }
    }

    fun start() = skissView.start()
    fun stop() = skissView.stop()
    fun pause() = skissView.pause()
    fun unpause() = skissView.unpause()

    abstract fun setup(width: Int, height: Int)
    abstract fun update(canvas: Canvas)
    open fun onTouch(x: Int, y: Int) {}
    open fun onLongClick() {}

}