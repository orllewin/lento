package orllewin.lento

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.children
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import orllewin.extensions.toPixels
import orllewin.lento.databinding.DialogFilmSelectionBinding

class FilmSelectionDialog(val context: Context, val onFilmSelect: (resId: Int?, label: String?) -> Unit): View.OnClickListener {

    private var dialog: AlertDialog? = null

    init {
        val builder = MaterialAlertDialogBuilder(context, R.style.modern_material_dialog)

        val view: View = LayoutInflater.from(context).inflate(R.layout.dialog_film_selection, null, false)
        val binding = DialogFilmSelectionBinding.bind(view)

        //Lento
        val lentoLabels = context.resources.getStringArray(R.array.lento_film_labels)
        val lentoResources = context.resources.getStringArray(R.array.lento_film_raw_resources)

        lentoLabels.forEachIndexed { index, label ->
            val chip = Chip(context)

            chip.text = label

            val rawId = lentoResources[index]
            chip.tag = rawId
            chip.isCheckable = true
            chip.id = ViewCompat.generateViewId()
            chip.setOnClickListener(this)
            binding.flexBoxLento.addView(chip)
        }

        binding.flexBoxLento.children.iterator().forEach { chip ->
            val lp = chip.layoutParams as FlexboxLayout.LayoutParams
            lp.setMargins(0, 0, 8.toPixels(), 0)
            chip.layoutParams = lp
        }

        //End of Lento

        //Colour
        val fileLabelsColour = context.resources.getStringArray(R.array.film_labels_colour)
        val fileResourcesColour = context.resources.getStringArray(R.array.film_raw_resources_colour)

        fileLabelsColour.forEachIndexed { index, label ->
            val chip = Chip(context)

            chip.text = label

            val rawId = fileResourcesColour[index]
            chip.tag = rawId
            chip.isCheckable = true
            chip.id = ViewCompat.generateViewId()
            chip.setOnClickListener(this)
            binding.flexBoxColour.addView(chip)
        }

        binding.flexBoxColour.children.iterator().forEach { chip ->
            val lp = chip.layoutParams as FlexboxLayout.LayoutParams
            lp.setMargins(0, 0, 8.toPixels(), 0)
            chip.layoutParams = lp
        }

        //End of colour

        //Monochrome
        val fileLabelsBW = context.resources.getStringArray(R.array.film_labels_bw)
        val fileResourcesBW = context.resources.getStringArray(R.array.film_raw_resources_bw)

        fileLabelsBW.forEachIndexed { index, label ->
            val chip = Chip(context)

            chip.text = label

            val rawId = fileResourcesBW[index]
            chip.tag = rawId
            chip.isCheckable = true
            chip.id = ViewCompat.generateViewId()
            chip.setOnClickListener(this)
            binding.flexBoxBw.addView(chip)
        }

        binding.flexBoxBw.children.iterator().forEach { chip ->
            val lp = chip.layoutParams as FlexboxLayout.LayoutParams
            lp.setMargins(0, 0, 8.toPixels(), 0)
            chip.layoutParams = lp
        }

        //End of monochrome

        //Start of misc
        val fileLabelsMisc = context.resources.getStringArray(R.array.film_labels_misc)
        val fileResourcesMisc = context.resources.getStringArray(R.array.film_raw_resources_misc)

        fileLabelsMisc.forEachIndexed { index, label ->
            val chip = Chip(context)

            chip.text = label

            val rawId = fileResourcesMisc[index]
            chip.tag = rawId
            chip.isCheckable = true
            chip.id = ViewCompat.generateViewId()
            chip.setOnClickListener(this)
            binding.flexBoxMisc.addView(chip)
        }

        binding.flexBoxMisc.children.iterator().forEach { chip ->
            val lp = chip.layoutParams as FlexboxLayout.LayoutParams
            lp.setMargins(0, 0, 8.toPixels(), 0)
            chip.layoutParams = lp
        }

        //End of misc

        builder.setView(view)
        dialog = builder.create()

    }

    fun show(){
        dialog?.show()
    }

    override fun onClick(view: View?) {
        when (val tag = view?.tag.toString()) {
            "none" -> onFilmSelect(null, null)
            else -> {
                val resId = context.resources.getIdentifier(tag, "raw", context.packageName)
                onFilmSelect(resId, (view as Chip).text.toString())
            }
        }

        dialog?.dismiss()
    }
}