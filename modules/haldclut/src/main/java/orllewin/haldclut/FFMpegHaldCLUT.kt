package orllewin.haldclut

import android.content.Context
import com.arthenica.ffmpegkit.FFmpegKit
import java.io.File

class FFMpegHaldCLUT(val context: Context) {

    var startTime = 0L
    var endTime = 0L

    fun process(haldRawResourceId: Int, haldLabel: String, photoFile: File, onProcessed: (file: File?, error: String?) -> Unit){
        startTime = System.currentTimeMillis()
        val haldLabelPathSegment = haldLabel.lowercase().replace(" ", "_").replace("\n", "_")

        //first see if we already have a file for this hald clut
        val existingHaldClut = context.cacheDir.listFiles()?.find { file ->
            file.name.contains(haldLabelPathSegment)
        }

        val haldClutExists = existingHaldClut != null
        val haldClutCacheFile = when {
            haldClutExists -> existingHaldClut
            else -> File.createTempFile("haldclut_temp_$haldLabelPathSegment", ".png", context.cacheDir)
        }

        when {
            haldClutExists -> println("xxhaldclut Lento cache haldclut already exists: ${haldClutCacheFile!!.name}")
            else -> {
                println("xxhaldclut Lento cache haldclut not in cache: fetching resource")
                val inputStream = context.resources.openRawResource(haldRawResourceId)
                haldClutCacheFile!!.outputStream().use { fileOutputStream ->
                    val buffer = ByteArray(1024)
                    var readSize: Int
                    while (inputStream.read(buffer).also { readSize = it } > 0) {
                        fileOutputStream.write(buffer, 0, readSize)
                    }
                }
            }
        }

        val tempFilteredFile = File.createTempFile("filtered_temp", ".jpg", context.cacheDir)

        val haldPath = haldClutCacheFile!!.absolutePath
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

            when (returnCode.value) {
                0 -> {
                    //Success
                    endTime = System.currentTimeMillis()
                    val processTime = endTime - startTime
                    println("xxhaldclut process time: $processTime milliseconds")
                    onProcessed(tempFilteredFile, null)
                }
                else -> {
                    //Error
                    onProcessed(null, stackTrace)
                }
            }
        }, { log ->
            println("FFMpeg half clut log: $log")
        }){ stats ->
            //NOOP
        }
    }
}