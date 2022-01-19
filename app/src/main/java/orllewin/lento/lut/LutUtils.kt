package orllewin.lento.lut

import android.content.res.Resources
import orllewin.lento.capitaliseAll

class LutUtils {
    data class Dimension(val sizeX: Int, val sizeY: Int, val sizeZ: Int)

    fun parseLuts(allLutResources: Array<String>, resources: Resources, packageName: String): List<Lut> {
        val monoLutCount = allLutResources.size

        val luts = mutableListOf<Lut>()

        repeat(monoLutCount){ i ->
            val resourceIdName = allLutResources[i]

            if(resourceIdName.startsWith(":")){
                val dividerLut = Lut(resourceIdName.substring(resourceIdName.lastIndexOf(":") + 1), -1, true)
                when {
                    resourceIdName.startsWith(":::") -> dividerLut.divSize = Lut.divSmall
                    resourceIdName.startsWith("::") -> dividerLut.divSize = Lut.divMedium
                    resourceIdName.startsWith(":") -> dividerLut.divSize = Lut.divLarge
                }
                luts.add(dividerLut)
            }else{

                val resourceId = resources.getIdentifier(resourceIdName, "drawable", packageName)
                val label = resourceIdName.replace("_", " ").capitaliseAll()
                val lut = Lut(label, resourceId)

                luts.add(lut)
            }
        }

        return luts
    }
}