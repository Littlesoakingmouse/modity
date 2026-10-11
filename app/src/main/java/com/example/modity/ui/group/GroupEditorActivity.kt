package com.example.modity.ui.group

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.modity.R
import com.example.modity.data.ModityRepository
import com.example.modity.databinding.ActivityGroupEditorBinding
import com.example.modity.model.TaskColors
import com.example.modity.model.TaskGroup
import com.example.modity.model.TaskIcon
import com.example.modity.ui.common.PickerAdapter
import com.example.modity.ui.common.bindBadge
import com.example.modity.utils.applySystemBarInsets
import kotlinx.coroutines.launch

/** Creates or edits a group. Returns the group id in [EXTRA_GROUP_ID]. */
class GroupEditorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGroupEditorBinding
    private lateinit var repository: ModityRepository
    private lateinit var colorAdapter: PickerAdapter
    private lateinit var iconAdapter: PickerAdapter

    private var original: TaskGroup? = null
    private lateinit var palette: List<Int>
    private var color = 0
    private var icon = TaskIcon.STAR

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        repository = ModityRepository.get(this)
        palette = TaskColors.palette(this)
        color = palette.first()

        colorAdapter = PickerAdapter.colors(palette) {
            color = it
            iconAdapter.notifyItemChanged(iconAdapter.selected)
            renderPreview()
        }
        iconAdapter = PickerAdapter.icons({ color }) {
            icon = it
            renderPreview()
        }
        binding.rvColors.adapter = colorAdapter
        binding.rvIcons.adapter = iconAdapter
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnSave.setOnClickListener { save() }

        lifecycleScope.launch {
            val groupId = intent.getLongExtra(EXTRA_GROUP_ID, -1L)
            repository.group(groupId)?.let { group ->
                original = group
                binding.toolbar.setTitle(R.string.edit_group)
                binding.etName.setText(group.name)
                binding.etDescription.setText(group.description)
                color = group.color
                icon = TaskIcon.from(group.icon)
            }
            colorAdapter.selected = palette.indexOf(color).coerceAtLeast(0)
            iconAdapter.selected = icon.ordinal
            renderPreview()
        }
    }

    private fun renderPreview() = binding.ivPreview.bindBadge(icon.name, color)

    private fun save() {
        val name = binding.etName.text?.toString()?.trim().orEmpty()
        binding.tilName.error = null
        if (name.isEmpty()) {
            binding.tilName.error = getString(R.string.error_name_required)
            return
        }
        val description = binding.etDescription.text?.toString()?.trim().orEmpty()
        val group = original?.copy(name = name, description = description, color = color, icon = icon.name)
            ?: TaskGroup(name = name, description = description, color = color, icon = icon.name)
        lifecycleScope.launch {
            val id = repository.saveGroup(group)
            setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_GROUP_ID, id))
            finish()
        }
    }

    companion object {
        const val EXTRA_GROUP_ID = "group_id"

        fun editIntent(context: Context, groupId: Long): Intent =
            Intent(context, GroupEditorActivity::class.java).putExtra(EXTRA_GROUP_ID, groupId)
    }
}
