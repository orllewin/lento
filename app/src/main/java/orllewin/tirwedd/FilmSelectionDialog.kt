package orllewin.tirwedd

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.children
import androidx.core.view.marginEnd
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import orllewin.extensions.toPixels
import orllewin.tirwedd.databinding.DialogFilmSelectionBinding

class FilmSelectionDialog(val context: Context, val onFilmSelect: (resId: Int?) -> Unit): View.OnClickListener {

    private var dialog: AlertDialog? = null

    init {
        val builder = MaterialAlertDialogBuilder(context, R.style.modern_material_dialog)

        val view: View = LayoutInflater.from(context).inflate(R.layout.dialog_film_selection, null, false)
        val binding = DialogFilmSelectionBinding.bind(view)

        val fileLabels = context.resources.getStringArray(R.array.film_labels)
        val fileResources = context.resources.getStringArray(R.array.film_raw_resources)

        fileLabels.forEachIndexed { index, label ->
            val chip = Chip(context)

            chip.text = label

            val rawId = fileResources[index]
            chip.tag = rawId
            chip.isCheckable = true
            chip.id = ViewCompat.generateViewId()
            chip.setOnClickListener(this)
            binding.flexBox.addView(chip)
        }

        binding.flexBox.children.iterator().forEach { view ->
            val lp = view.layoutParams as FlexboxLayout.LayoutParams
            lp.setMargins(0, 0, 16.toPixels(), 0)
            view.layoutParams = lp
        }

        builder.setView(view)
        dialog = builder.create()

        //dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#22ff00cc")))
    }

    fun show(){
        dialog?.show()
    }

    override fun onClick(view: View?) {
        when (val tag = view?.tag.toString()) {
            "none" -> onFilmSelect(null)
            else -> {
                val resId = context.resources.getIdentifier(tag, "raw", context.packageName)
                onFilmSelect(resId)
            }
        }

        dialog?.dismiss()
    }
}