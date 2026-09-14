package com.example.modity.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.modity.databinding.ItemDateBinding
import com.example.modity.utils.DateTextFormatter
import java.time.LocalDate

class DateAdapter(
    private val onDateClick: (LocalDate) -> Unit = {},
) : ListAdapter<LocalDate, DateAdapter.DateViewHolder>(DateDiffCallback) {
    private var selectedDate: LocalDate? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateViewHolder {
        val binding = ItemDateBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DateViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DateViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun selectDate(date: LocalDate) {
        val previousPosition = currentList.indexOf(selectedDate)
        val selectedPosition = currentList.indexOf(date)
        selectedDate = date

        if (previousPosition >= 0) notifyItemChanged(previousPosition)
        if (selectedPosition >= 0) notifyItemChanged(selectedPosition)
    }

    inner class DateViewHolder(
        private val binding: ItemDateBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(date: LocalDate) = with(binding) {
            dateWeekday.text = DateTextFormatter.weekday(date)
            dateDay.text = DateTextFormatter.dayOfMonth(date)
            dateCard.isChecked = date == selectedDate
            root.setOnClickListener {
                selectDate(date)
                onDateClick(date)
            }
        }
    }

    private object DateDiffCallback : DiffUtil.ItemCallback<LocalDate>() {
        override fun areItemsTheSame(oldItem: LocalDate, newItem: LocalDate): Boolean =
            oldItem == newItem

        override fun areContentsTheSame(oldItem: LocalDate, newItem: LocalDate): Boolean =
            oldItem == newItem
    }
}
