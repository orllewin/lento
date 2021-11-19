package orllewin.tirwedd

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.View
import oppen.stracka.AnimationEndListener
import orllewin.extensions.hide
import orllewin.extensions.show

class ShutterView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    init {
        background = ColorDrawable(Color.BLACK)
        hide()
    }

    fun activate(){
        alpha = 0f
        show()

        animate().alpha(1f).setDuration(50L).setListener(AnimationEndListener{
            animate().alpha(1f).setDuration(100L).setListener(AnimationEndListener{
                hide()
            }).start()
        }).start()
    }
}