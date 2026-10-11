package com.example.modity.ui.common

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.modity.R
import com.example.modity.databinding.ItemPickerOptionBinding
import com.example.modity.model.TaskIcon
import com.example.modity.utils.themeColor

/** Horizontal single-choice picker for task/group colors or icons. */
class PickerAdapter private constructor(
    private val size: Int,
    private val bindOption: (ItemPickerOptionBinding, Int, Boolean) -> Unit,
    private val onSelected: (Int) -> Unit,
) : RecyclerView.Adapter<PickerAdapter.Holder>() {

    var selected: Int = 0
        set(value) {
            val previous = field
            field = value
            notifyItemChanged(previous)
            notifyItemChanged(value)
        }

    override fun getItemCount(): Int = size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(ItemPickerOptionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        bindOption(holder.binding, position, position == selected)
        holder.binding.root.setOnClickListener {
            val index = holder.bindingAdapterPosition
            if (index == RecyclerView.NO_POSITION) return@setOnClickListener
            selected = index
            onSelected(index)
        }
    }

    class Holder(val binding: ItemPickerOptionBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {
        fun colors(palette: List<Int>, onSelected: (Int) -> Unit) = PickerAdapter(palette.size, { binding, position, isSelected ->
            val white = ContextCompat.getColor(binding.root.context, android.R.color.white)
            binding.ivOption.backgroundTintList = ColorStateList.valueOf(palette[position])
            binding.ivOption.setImageResource(if (isSelected) R.drawable.ic_check else 0)
            binding.ivOption.imageTintList = ColorStateList.valueOf(white)
            binding.root.contentDescription = binding.root.context.getString(R.string.desc_color_option, position + 1)
        }, { onSelected(palette[it]) })

        /** [accent] returns the currently chosen color so the selected icon previews it. */
        fun icons(accent: () -> Int, onSelected: (TaskIcon) -> Unit) = PickerAdapter(TaskIcon.entries.size, { binding, position, isSelected ->
            val context = binding.root.context
            val background = if (isSelected) accent() else context.themeColor(com.google.android.material.R.attr.colorSurfaceVariant)
            val tint = if (isSelected) ContextCompat.getColor(context, android.R.color.white)
            else context.themeColor(com.google.android.material.R.attr.colorOnSurfaceVariant)
            binding.ivOption.backgroundTintList = ColorStateList.valueOf(background)
            binding.ivOption.setImageResource(TaskIcon.entries[position].drawable)
            binding.ivOption.imageTintList = ColorStateList.valueOf(tint)
            binding.root.contentDescription = TaskIcon.entries[position].name.lowercase()
        }, { onSelected(TaskIcon.entries[it]) })
    }
}
