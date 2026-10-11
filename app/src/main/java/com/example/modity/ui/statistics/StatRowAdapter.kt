package com.example.modity.ui.statistics

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.modity.databinding.ItemStatRowBinding
import com.example.modity.ui.common.bindBadge
import com.example.modity.utils.themeColor
import com.google.android.material.R as MaterialR

/** One row of a statistics list (group rate, habit streak, day history). */
data class StatRow(
    val key: String,
    val title: String,
    val value: String,
    val subtitle: String? = null,
    val progress: Int? = null,
    val color: Int? = null,
    val icon: String? = null,
)

class StatRowAdapter : ListAdapter<StatRow, StatRowAdapter.Holder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemStatRowBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int): Unit = with(holder.binding) {
        val row = getItem(position)
        tvRowTitle.text = row.title
        tvRowValue.text = row.value
        tvRowSubtitle.text = row.subtitle
        tvRowSubtitle.isVisible = !row.subtitle.isNullOrBlank()
        ivRowBadge.isVisible = row.icon != null && row.color != null
        if (row.icon != null && row.color != null) ivRowBadge.bindBadge(row.icon, row.color)
        progressRow.isVisible = row.progress != null
        row.progress?.let {
            progressRow.setIndicatorColor(row.color ?: root.context.themeColor(MaterialR.attr.colorOnSurface))
            progressRow.setProgressCompat(it, false)
        }
    }

    class Holder(val binding: ItemStatRowBinding) : RecyclerView.ViewHolder(binding.root)

    private object Diff : DiffUtil.ItemCallback<StatRow>() {
        override fun areItemsTheSame(oldItem: StatRow, newItem: StatRow) = oldItem.key == newItem.key
        override fun areContentsTheSame(oldItem: StatRow, newItem: StatRow) = oldItem == newItem
    }
}
