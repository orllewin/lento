package orllewin.lento

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import orllewin.lento.databinding.DialogFilmSimulation2Binding
import orllewin.lento.lut.Lut
import orllewin.lento.lut.LutAdapter
import orllewin.lento.lut.LutUtils

class FilmSelectionDialog2(val context: Context, previewBitmap: Bitmap?, val onLut: (lut: Lut) -> Unit) {

    private var dialog: AlertDialog? = null
    private var layoutManager: StaggeredGridLayoutManager

    private val adapter = LutAdapter { lut ->
        onLut(lut)
        dialog?.dismiss()
    }

    init {
        val builder = MaterialAlertDialogBuilder(context, R.style.modern_material_dialog)

        val view: View = LayoutInflater.from(context).inflate(R.layout.dialog_film_simulation2, null, false)
        val binding = DialogFilmSimulation2Binding.bind(view)

        layoutManager = StaggeredGridLayoutManager(3, RecyclerView.VERTICAL)
        binding.lutRecycler.layoutManager = layoutManager
        binding.lutRecycler.adapter = adapter

        adapter.setReferenceImage(previewBitmap)

        val allLutResources = context.resources.getStringArray(R.array.all_luts)
        val luts = LutUtils().parseLuts(allLutResources, context.resources, context.packageName)
        adapter.updateLuts(luts)

        builder.setView(view)
        dialog = builder.create()

    }

    fun show(){
        dialog?.show()
    }
}