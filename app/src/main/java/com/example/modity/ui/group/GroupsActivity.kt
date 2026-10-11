package com.example.modity.ui.group

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.modity.R
import com.example.modity.data.ModityRepository
import com.example.modity.databinding.ActivityGroupsBinding
import com.example.modity.databinding.ItemGroupManageBinding
import com.example.modity.model.TaskGroup
import com.example.modity.ui.common.bindBadge
import com.example.modity.utils.applySystemBarInsets
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Collections

/** Lists all groups; drag to change their order on the home screen. */
class GroupsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGroupsBinding
    private lateinit var repository: ModityRepository
    private val adapter = GroupsAdapter()
    private lateinit var touchHelper: ItemTouchHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        repository = ModityRepository.get(this)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnAddGroup.setOnClickListener { startActivity(Intent(this, GroupEditorActivity::class.java)) }
        binding.rvGroups.adapter = adapter

        touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun onMove(recyclerView: RecyclerView, from: RecyclerView.ViewHolder, to: RecyclerView.ViewHolder): Boolean {
                adapter.move(from.bindingAdapterPosition, to.bindingAdapterPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

            override fun isLongPressDragEnabled() = false

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                lifecycleScope.launch { repository.reorderGroups(adapter.groups) }
            }
        })
        touchHelper.attachToRecyclerView(binding.rvGroups)
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val tasks = repository.tasks()
            adapter.taskCounts = tasks.groupingBy { it.groupId }.eachCount()
            adapter.groups = repository.groups().toMutableList()
            @SuppressLint("NotifyDataSetChanged")
            adapter.notifyDataSetChanged()
        }
    }

    private inner class GroupsAdapter : RecyclerView.Adapter<GroupsAdapter.Holder>() {
        var groups: MutableList<TaskGroup> = mutableListOf()
        var taskCounts: Map<Long?, Int> = emptyMap()

        fun move(from: Int, to: Int) {
            if (from < 0 || to < 0) return
            Collections.swap(groups, from, to)
            notifyItemMoved(from, to)
        }

        override fun getItemCount() = groups.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(ItemGroupManageBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        @SuppressLint("ClickableViewAccessibility")
        override fun onBindViewHolder(holder: Holder, position: Int): Unit = with(holder.binding) {
            val group = groups[position]
            ivGroupIcon.bindBadge(group.icon, group.color)
            tvGroupName.text = group.name
            val count = taskCounts[group.id] ?: 0
            tvGroupInfo.text = resources.getQuantityString(R.plurals.group_task_count, count, count)
            groupManageCard.setOnClickListener {
                startActivity(GroupDetailActivity.intent(this@GroupsActivity, group.id, LocalDate.now()))
            }
            ivDrag.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) touchHelper.startDrag(holder)
                false
            }
        }

        inner class Holder(val binding: ItemGroupManageBinding) : RecyclerView.ViewHolder(binding.root)
    }
}
