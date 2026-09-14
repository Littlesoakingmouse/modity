package com.example.modity.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.modity.databinding.ActivitySettingsBinding
import com.example.modity.utils.applySystemBarInsets

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applySystemBarInsets()
        binding.toolbarSettings.setNavigationOnClickListener { finish() }
    }
}
