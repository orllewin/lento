package orllewin.lento

import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu

class OverflowMenu(
    private val context: Context,
    view: View,
    timerActive: Boolean,
    levelActive: Boolean,
    gridActive: Boolean,
    onToggleLevel: () -> Unit,
    onTimerToggle: () -> Unit,
    onToggleGrid: () -> Unit) {

    private val popup = PopupMenu(context, view)

    init {
        val cameraItem = popup.menu.add("Switch camera")
        cameraItem.setOnMenuItemClickListener {
            val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            context.startActivity(intent)
            true
        }


        val timerItem = if(timerActive){
            popup.menu.add("Turn timer off")
        }else{
            popup.menu.add("Turn timer on")
        }

        timerItem.setOnMenuItemClickListener {
            onTimerToggle()
            true
        }

        val levelItem = if(levelActive){
            popup.menu.add("Turn level off")
        }else{
            popup.menu.add("Turn level on")
        }

        levelItem.setOnMenuItemClickListener {
            onToggleLevel()
            true
        }

        val gridItem = if(gridActive){
            popup.menu.add("Turn grid off")
        }else{
            popup.menu.add("Turn grid on")
        }
        gridItem.setOnMenuItemClickListener {
            onToggleGrid()
            true
        }

        val settingsItem = popup.menu.add("Settings")
        settingsItem.setOnMenuItemClickListener {
            context.startActivity(Intent(context, SettingsActivity::class.java))
            true
        }

        val quitItem = popup.menu.add("Quit")
        quitItem.setOnMenuItemClickListener {
            if(context is AppCompatActivity) context.finishAffinity()
            true
        }
    }

    fun show() = popup.show()
}