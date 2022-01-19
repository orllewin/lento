package orllewin.lento

import android.content.Context
import androidx.camera.core.AspectRatio
import androidx.camera.core.ImageCapture
import androidx.preference.PreferenceManager
import orllewin.lento.lut.Lut

class CameraConfig(
    var isFirstRun: Boolean = true,
    var isAnamorphic: Boolean = true,
    var anamorphicScaleFactor: Float = 1.33f,
    var aspectRatioFlag: Int = AspectRatio.RATIO_16_9,
    var zoomLevel: Int = 1,
    var lutResource: Int = -1,
    var lutLabel: String = "",
    var flashMode: Int = ImageCapture.FLASH_MODE_OFF,
    var borderMode: Int = AnamorphicPhotoProcessor.BorderNone,
    var hideAnamnorphicFeatures: Boolean = false
){
    private constructor(builder: Builder) : this(
        isFirstRun = builder.isFirstRun ?: true,
        isAnamorphic = builder.isAnamorphic ?: true,
        anamorphicScaleFactor = builder.anamorphicScaleFactor ?: 1.33f,
        aspectRatioFlag = builder.aspectRatioFlag ?: AspectRatio.RATIO_16_9,
        zoomLevel = builder.zoomLevel ?: 1,
        lutResource = builder.lutResource ?: -1,
        lutLabel = builder.lutLabel,
        flashMode = builder.flashMode,
        borderMode = builder.borderMode,
        hideAnamnorphicFeatures = builder.hideAnamnorphicFeatures ?: false

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
            editor.putInt("flashMode", config.flashMode)
            editor.putInt("borderMode", config.borderMode)
            editor.putBoolean("hideAnamnorphicFeatures", config.hideAnamnorphicFeatures)
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
            builder.flashMode = prefs.getInt("flashMode", ImageCapture.FLASH_MODE_OFF)
            builder.borderMode = prefs.getInt("borderMode", AnamorphicPhotoProcessor.BorderNone)
            builder.hideAnamnorphicFeatures = prefs.getBoolean("hideAnamnorphicFeatures", false)
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
        var flashMode: Int = ImageCapture.FLASH_MODE_OFF
        var borderMode: Int = AnamorphicPhotoProcessor.BorderNone
        var hideAnamnorphicFeatures: Boolean? = null
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

    fun setUnrealLut(context: Context, lut: Lut){
        this.lutResource = lut.resourceId
        this.lutLabel = lut.label
        put(context, this)
    }

    fun setFlashMode(context: Context, flashMode: Int){
        this.flashMode = flashMode
        put(context, this)
    }

    fun setBorderMode(context: Context, borderMode: Int){
        this.borderMode = borderMode
        put(context, this)
    }

    fun setHideAnamnorphicFeatures(context: Context, hideAnamnorphicFeatures: Boolean){
        this.hideAnamnorphicFeatures = hideAnamnorphicFeatures
        put(context, this)
    }

    fun logAspectRatio() = when (aspectRatioFlag) {
        AspectRatio.RATIO_4_3 -> println("aspectRatioFlag: RATIO_4_3")
        AspectRatio.RATIO_16_9 -> println("aspectRatioFlag: RATIO_16_9")
        else -> println("aspectRatioFlag: UNKNOWN")
    }
}