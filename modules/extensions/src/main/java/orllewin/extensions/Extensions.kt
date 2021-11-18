package orllewin.extensions

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.net.Uri
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import androidx.preference.PreferenceManager
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageView
import com.google.android.material.snackbar.Snackbar


/**
 * View
 */
fun View.show(){
    this.visibility = View.VISIBLE
}

fun View.hide(){
    this.visibility = View.GONE
}

fun View.visible(visible: Boolean) = when {
    visible -> this.show()
    else -> this.hide()
}

fun View.snack(message: String){
    Snackbar.make(this, message, Snackbar.LENGTH_SHORT).show()
}

fun View.actionSnack(message: String, actionLabel: String, onAction: () -> Unit, onDismissed: () -> Unit){
    var interrupted = false
    val snackbar = Snackbar.make(this, message, Snackbar.LENGTH_SHORT)
        .setAction(actionLabel){
            interrupted = true
            onAction()
        }
        .addCallback(object: Snackbar.Callback() {
            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                if(!interrupted) onDismissed()
            }
        })

        snackbar.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_action)?.let { actionView ->
            actionView.isAllCaps = false
        }

        snackbar.show()
}

fun View.hideKeyboard(){
    val imm: InputMethodManager? = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
    imm?.hideSoftInputFromWindow(windowToken, 0)
}

fun View.showKeyboard(){
    val imm: InputMethodManager? = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
    imm?.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
}

fun AppCompatEditText.clear(){
    this.text?.clear()
}

fun AppCompatImageView.monochrome(monochrome: Boolean){
    when {
        monochrome -> this.monochrome()
        else -> this.normal()
    }
}

fun AppCompatImageView.monochrome(){
    val matrix = ColorMatrix()
    matrix.setSaturation(0f)

    val filter = ColorMatrixColorFilter(matrix)
    this.colorFilter = filter
}

fun AppCompatImageView.normal(){
    this.colorFilter = null
}

/**
 * Uri
 */

fun Uri.isWeb(): Boolean = this.toString().startsWith("http")
fun Uri.isImage(): Boolean{
    val lowercase = this.toString().lowercase()
    return lowercase.endsWith(".png") ||
            lowercase.endsWith(".jpg") ||
            lowercase.endsWith(".jpeg") ||
            lowercase.endsWith(".gif")
}

/**
 * String
 */

fun String.filename(): String = when {
    this.contains("/") && this.contains(".") -> this.substring(this.lastIndexOf("/") + 1)
    else -> this
}

fun String.endsWithImage(): Boolean{
    val lowercase = this.lowercase()
    return lowercase.endsWith(".png") ||
            lowercase.endsWith(".jpg") ||
            lowercase.endsWith(".jpeg") ||
            lowercase.endsWith(".gif")
}

fun String.Companion.path(vararg _segment: String): String{
    val path = _segment.joinToString("/") { segment ->
        var mapped = segment
        if (segment.length > 1 && segment.startsWith("/") && segment != "//") {
            mapped = segment.substring(1)
        }
        if (segment.length > 1 && segment.endsWith("/") && segment != "//" && !segment.endsWith("://")) {
            mapped = mapped.substring(0, mapped.length - 1)
        }

        mapped
    }
    return path.replace(":///", "://")

}

/**
 * Colours
 */

fun AppCompatActivity.statusBarColour(colour: String){
    val window: Window = window
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    window.statusBarColor = Color.parseColor(colour)
}

fun AppCompatActivity.statusBarColour(@ColorInt colour: Int){
    val window: Window = window
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    window.statusBarColor = colour
}

fun invertColour(@ColorInt color: Int): Int {
    val invertedRed = 255 - Color.red(color)
    val invertedGreen = 255 - Color.green(color)
    val invertedBlue = 255 - Color.blue(color)
    return Color.rgb(invertedRed, invertedGreen, invertedBlue)
}

fun Int.toHexColour(): String {
    return java.lang.String.format("#%06X", 0xFFFFFF and this)
}

/**
 * Structural
 */
fun mainThread(action:() -> Unit){
    Handler(Looper.getMainLooper()).post{
        action.invoke()
    }
}

fun delayed(ms: Long, action: () -> Unit) {
    object : CountDownTimer(ms, ms / 2) {
        override fun onTick(millisUntilFinished: Long) {}

        override fun onFinish() {
            action.invoke()
        }
    }.start()
}

fun AppCompatActivity.delayedNavigation(ms: Long, intent: Intent){
    object : CountDownTimer(ms, ms/2) {
        override fun onTick(millisUntilFinished: Long) {}

        override fun onFinish() {
            startActivity(intent)
            finish()
        }
    }.start()
}

//Context/Prefs
fun Context.getBooleanPref(key: String, default: Boolean): Boolean{
    return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(key, default)
}

fun Context.putBooleanPref(key: String, value: Boolean) {
    PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(key, value).apply()
}

fun Context.getStringPref(key: String, default: String): String? {
    return PreferenceManager.getDefaultSharedPreferences(this).getString(key, default)
}