package orllewin.lento

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import orllewin.lento.databinding.DialogFirstRunBinding

class FirstRunDialog(context: Context) {

    private var dialog: AlertDialog? = null

    init {
        val builder = MaterialAlertDialogBuilder(context, R.style.modern_material_dialog)

        val view: View = LayoutInflater.from(context).inflate(R.layout.dialog_first_run, null, false)
        val binding = DialogFirstRunBinding.bind(view)
        builder.setView(view)

        builder.setPositiveButton(R.string.disclaimer_button){ _, _ ->
            dialog?.dismiss()
        }

        dialog = builder.create()

    }

    fun show() = dialog?.show()
}