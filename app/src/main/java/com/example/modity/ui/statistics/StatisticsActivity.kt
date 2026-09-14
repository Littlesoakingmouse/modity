package com.example.modity.ui.statistics

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.modity.databinding.ActivityStatisticsBinding
import com.example.modity.utils.applySystemBarInsets

class StatisticsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStatisticsBinding
    private val statisticAdapter = StatisticAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applySystemBarInsets()
        binding.toolbarStatistics.setNavigationOnClickListener { finish() }
        binding.rvStatistics.layoutManager = LinearLayoutManager(this)
        binding.rvStatistics.adapter = statisticAdapter
        binding.emptyStatistics.isVisible = statisticAdapter.currentList.isEmpty()
    }
}
