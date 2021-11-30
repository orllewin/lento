package orllewin.haldclut

import android.content.Context
import com.arthenica.ffmpegkit.FFmpegKit
import java.io.File

class FFMpegHaldCLUT(val context: Context) {

    fun process(haldRawResourceId: Int, photoFile: File, onProcessed: (file: File?, error: String?) -> Unit){
        val inputStream = context.resources.openRawResource(haldRawResourceId)
        val tempHaldClutFile = File.createTempFile("haldclut_temp", ".png", context.cacheDir)


        if(tempHaldClutFile.exists()) tempHaldClutFile.delete()

        tempHaldClutFile.outputStream().use { fileOutputStream ->
            val buffer = ByteArray(1024)
            var readSize: Int

            while (inputStream.read(buffer).also { readSize = it } > 0) {
                fileOutputStream.write(buffer, 0, readSize)
            }
        }

        val tempFilteredFile = File.createTempFile("filtered_temp", ".jpg", context.cacheDir)

        val haldPath = tempHaldClutFile.absolutePath
        val photoPath = photoFile.absolutePath

        val filteredPath = tempFilteredFile.absolutePath

        val command = "-y -i \"$photoPath\" -i \"$haldPath\" -filter_complex \"haldclut\" \"$filteredPath\""

        println("FFMpeg: starting async...")
        FFmpegKit.executeAsync(command, { session ->
            println("FFMpeg: finished async...")
            val state = session.state
            val stackTrace = session.failStackTrace
            val returnCode = session.returnCode
            println("Process exit return code: $returnCode")

            if(returnCode.value == 0){
                //Success
                onProcessed(tempFilteredFile, null)
            }else{
                //Error
                throw Exception("FFMpeg: Failed to procdess using FFMpeg: $stackTrace")
                onProcessed(null, stackTrace)
            }
        }, { log ->
            println("FFMpeg log: $log")
        }){ stats ->
            //NOOP
        }
    }
}