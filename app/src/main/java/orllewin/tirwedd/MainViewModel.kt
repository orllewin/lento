package orllewin.tirwedd

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class MainViewModel: ViewModel() {

    private var imageProcessor: AnamorphicPhotoProcessor? = null

    val takePhotoStateFlow = MutableStateFlow<Boolean>(false)

    fun putImageProcessor(imageProcessor: AnamorphicPhotoProcessor) {
        this.imageProcessor = imageProcessor
    }

    fun capturePhoto(){
        takePhotoStateFlow.value = true
        takePhotoStateFlow.value = false//Subsequent event not emitted if we don't reset here
        imageProcessor?.capturePhoto()
    }
}