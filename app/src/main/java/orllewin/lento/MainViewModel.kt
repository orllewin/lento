package orllewin.lento

import android.os.CountDownTimer
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class MainViewModel: ViewModel() {

    var isTimerActive = false

    private var imageProcessor: AnamorphicPhotoProcessor? = null

    val takePhotoStateFlow = MutableStateFlow<Boolean>(false)
    val hudMessageFlow = MutableStateFlow<String?>(null)

    fun timerActive(isTimerActive: Boolean){
        this.isTimerActive = isTimerActive
    }

    fun putImageProcessor(imageProcessor: AnamorphicPhotoProcessor) {
        this.imageProcessor = imageProcessor
    }

    fun capturePhoto(){
        if(isTimerActive){
            val timer = object: CountDownTimer(5000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    val seconds = ((millisUntilFinished + 500) / 1000).toInt()
                    hudMessageFlow.value = "Capture in ${seconds}"
                }

                override fun onFinish() {
                    hudMessageFlow.value = null
                    doCapture()
                }
            }
            timer.start()
        }else{
            doCapture()
        }
    }

    fun doCapture(){
        takePhotoStateFlow.value = true
        takePhotoStateFlow.value = false//Subsequent event not emitted if we don't reset here
        imageProcessor?.capturePhoto()
    }
}