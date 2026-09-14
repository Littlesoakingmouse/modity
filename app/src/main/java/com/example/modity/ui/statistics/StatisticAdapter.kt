package com.example.modity.ui.statistics

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.modity.databinding.ItemStatisticBinding
import com.example.modity.model.StatisticItem

class StatisticAdapter :
    ListAdapter<StatisticItem, StatisticAdapter.StatisticViewHolder>(StatisticDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatisticViewHolder {
        val binding = ItemStatisticBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StatisticViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StatisticViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class StatisticViewHolder(
        private val binding: ItemStatisticBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(statistic: StatisticItem) = with(binding) {
            statisticLabel.text = statistic.label
            statisticValue.text = statistic.value
        }
    }

    private object StatisticDiffCallback : DiffUtil.ItemCallback<StatisticItem>() {
        override fun areItemsTheSame(oldItem: StatisticItem, newItem: StatisticItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: StatisticItem, newItem: StatisticItem): Boolean =
            oldItem == newItem
    }
}
