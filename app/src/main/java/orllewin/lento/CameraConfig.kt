package orllewin.lento

import android.content.Context
import androidx.camera.core.AspectRatio
import androidx.preference.PreferenceManager

class CameraConfig(
    var isFirstRun: Boolean = true,
    var isAnamorphic: Boolean = true,
    var anamorphicScaleFactor: Float = 1.33f,
    var aspectRatioFlag: Int = AspectRatio.RATIO_16_9,
    var zoomLevel: Int = 1,
    var lutResource: Int = -1,
    var lutLabel: String = ""
){
    private constructor(builder: Builder) : this(
        isFirstRun = builder.isFirstRun ?: true,
        isAnamorphic = builder.isAnamorphic ?: true,
        anamorphicScaleFactor = builder.anamorphicScaleFactor ?: 1.33f,
        aspectRatioFlag = builder.aspectRatioFlag ?: AspectRatio.RATIO_16_9,
        zoomLevel = builder.zoomLevel ?: 1,
        lutResource = builder.lutResource ?: -1,
        lutLabel = builder.lutLabel ?: ""
    )

    companion object {
        //inline fun build(block: Builder.() -> Unit) = Builder().apply(block).build()

        fun put(context: Context, config: CameraConfig){
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val editor = prefs.edit()
            editor.putBoolean("isFirstRun", config.isFirstRun)
            editor.putBoolean("isAnamorphic", config.isAnamorphic)
            editor.putFloat("anamorphicScaleFactor", config.anamorphicScaleFactor)
            editor.putInt("aspectRatioFlag", config.aspectRatioFlag)
            editor.putInt("zoomLevel", config.zoomLevel)
            editor.putInt("lutResource", config.lutResource)
            editor.putString("lutLabel", config.lutLabel)
            editor.apply()
        }

        fun get(context: Context): CameraConfig{
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val builder = Builder()
            builder.isFirstRun = prefs.getBoolean("isFirstRun", true)
            builder.isAnamorphic = prefs.getBoolean("isAnamorphic", false)
            builder.anamorphicScaleFactor = prefs.getFloat("anamorphicScaleFactor", 1.33f)
            builder.aspectRatioFlag = prefs.getInt("aspectRatioFlag", AspectRatio.RATIO_16_9)
            builder.zoomLevel = prefs.getInt("zoomLevel", 1)
            builder.lutResource = prefs.getInt("lutResource", -1)
            builder.lutLabel = prefs.getString("lutLabel", "") ?: ""
            return builder.build()
        }
    }

    class Builder {
        var isFirstRun: Boolean? = null
        var isAnamorphic: Boolean? = null
        var anamorphicScaleFactor: Float? = 1.33f
        var aspectRatioFlag: Int? = AspectRatio.RATIO_16_9
        var zoomLevel: Int? = 1
        var lutResource: Int? = -1
        var lutLabel: String = ""
        fun build() = CameraConfig(this)
    }

    fun hasRan(context: Context){
        this.isFirstRun = false
        put(context, this)
    }

    fun setAnamorphic(context: Context, isAnamorphic: Boolean){
        this.isAnamorphic = isAnamorphic
        put(context, this)
    }

    fun setAnamorphicScaleFactor(context: Context, anamorphicScaleFactor: Float){
        this.anamorphicScaleFactor = anamorphicScaleFactor
        put(context, this)
    }

    fun setAspectRatio(context: Context, aspectRatioFlag: Int){
        this.aspectRatioFlag = aspectRatioFlag
        put(context, this)
    }

    fun setZoomLevel(context: Context, zoomLevel: Int){
        this.zoomLevel = zoomLevel
        put(context, this)
    }

    fun setLut(context: Context, lutResource: Int?, lutLabel: String){
        this.lutResource = lutResource ?: -1
        this.lutLabel = lutLabel
        put(context, this)
    }

    fun logAspectRatio() = when (aspectRatioFlag) {
        AspectRatio.RATIO_4_3 -> println("aspectRatioFlag: RATIO_4_3")
        AspectRatio.RATIO_16_9 -> println("aspectRatioFlag: RATIO_16_9")
        else -> println("aspectRatioFlag: UNKNOWN")
    }
}