package orllewin.lento.lut


import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import orllewin.extensions.show
import orllewin.lento.R
import orllewin.lento.databinding.LutCellBinding

@SuppressLint("NotifyDataSetChanged")
class LutAdapter(val onLutSelected: (lut: Lut) -> Unit): RecyclerView.Adapter<LutAdapter.ViewHolder>(){

    companion object{
        val viewTypeDivider = 0
        val viewTypeCell = 1
    }

    private val luts = mutableListOf<Lut>()
    private val toolkit = UnrealLutToolkit()

    private var referenceBitmap: Bitmap? = null

    override fun getItemViewType(position: Int): Int {
        val lut = luts[position]
        return when {
            lut.isDivider -> viewTypeDivider
            else -> viewTypeCell
        }
    }

    fun setReferenceImage(referenceBitmap: Bitmap?){
        this@LutAdapter.referenceBitmap = referenceBitmap

        when {
            luts.isNotEmpty() -> notifyDataSetChanged()
        }
    }

    fun updateLuts(_luts: List<Lut>){
        luts.clear()
        luts.addAll(_luts)
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: LutCellBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = LutCellBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val lut = luts[position]

        val layoutParams = holder.binding.root.layoutParams as StaggeredGridLayoutManager.LayoutParams

        if(lut.isDivider || referenceBitmap == null){
            holder.binding.label.show()
            holder.binding.label.text = lut.label
            layoutParams.isFullSpan = true

            holder.binding.label.background = null

            when (lut.divSize) {
                Lut.divLarge -> holder.binding.label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                Lut.divMedium -> holder.binding.label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                Lut.divSmall -> holder.binding.label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            }

            if(!lut.isDivider){
                holder.binding.root.setOnClickListener {
                    onLutSelected(luts[holder.adapterPosition])
                }
            }
        }else {
            layoutParams.isFullSpan = false
            holder.binding.label.show()
            holder.binding.label.background = ContextCompat.getDrawable(holder.binding.label.context, R.drawable.vector_drawable_rounded_rect)
            holder.binding.label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            holder.binding.label.text = lut.label
            toolkit.loadLut(holder.binding.root.context, lut)
            holder.binding.lutImage.setImageBitmap(toolkit.process(referenceBitmap))
            holder.binding.root.setOnClickListener {
                onLutSelected(luts[holder.adapterPosition])
            }
        }
    }

    override fun getItemCount(): Int = luts.size
}