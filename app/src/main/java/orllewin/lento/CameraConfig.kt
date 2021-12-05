package orllewin.lento

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import orllewin.extensions.getBooleanPref
import orllewin.extensions.putBooleanPref

class CameraConfig(
    var isAnamorphic: Boolean = true
){
    private constructor(builder: Builder) : this(
        isAnamorphic = builder.isAnamorphic ?: true
    )

    companion object {
        inline fun build(block: Builder.() -> Unit) = Builder().apply(block).build()

        fun put(context: Context, config: CameraConfig){
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val editor = prefs.edit()
            editor.putBoolean("isAnamorphic", config.isAnamorphic)

            editor.apply()
        }

        fun get(context: Context): CameraConfig{
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val builder = Builder()
            builder.isAnamorphic = prefs.getBoolean("isAnamorphic",true)

            return builder.build()
        }
    }

    class Builder {
        var isAnamorphic: Boolean? = null
        fun build() = CameraConfig(this)
    }
}