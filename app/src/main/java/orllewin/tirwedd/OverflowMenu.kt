package orllewin.tirwedd

import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu

class OverflowMenu(private val context: Context, view: View) {

    private val popup = PopupMenu(context, view)

    init {
        val cameraItem = popup.menu.add("Switch camera")
        cameraItem.setOnMenuItemClickListener {
            val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            context.startActivity(intent)
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