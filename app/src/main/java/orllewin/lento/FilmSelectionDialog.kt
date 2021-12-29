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

        val lentoLabels = context.resources.getStringArray(R.array.lento_film_labels)
        val lentoResources = context.resources.getStringArray(R.array.lento_film_raw_resources)

        //None chip to cancel LUT selection
        val noneChip = Chip(context)
        noneChip.text = "None"
        noneChip.tag = "none"
        noneChip.isCheckable = true
        noneChip.id = ViewCompat.generateViewId()
        noneChip.setOnClickListener(this)
        binding.flexBoxLento.addView(noneChip)

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