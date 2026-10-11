package com.example.modity.ui.focus

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.modity.R
import com.example.modity.data.ModityRepository
import com.example.modity.databinding.ActivityFocusTimerBinding
import com.example.modity.model.FocusResult
import com.example.modity.model.FocusSession
import com.example.modity.model.OccurrenceStatus
import com.example.modity.utils.Formatters
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

/**
 * Minimal full-screen countdown. Records start/end time, planned and actual duration,
 * pause count and result as a [FocusSession]. It cannot lock the phone; it only keeps the
 * screen on, hides system bars and optionally turns on Do Not Disturb when access was granted.
 */
class FocusTimerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFocusTimerBinding
    private lateinit var repository: ModityRepository

    private var taskId: Long? = null
    private var taskTitle = ""
    private var plannedMs = 0L
    private var remainingMs = 0L
    private var endAtElapsed = 0L
    private var running = false
    private var pauseCount = 0
    private var startedAt = 0L
    private var finished = false
    private var date: LocalDate = LocalDate.now()

    private var doNotDisturb = false
    private var previousInterruptionFilter: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFocusTimerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repository = ModityRepository.get(this)

        taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L).takeIf { it > 0 }
        taskTitle = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        plannedMs = intent.getIntExtra(EXTRA_MINUTES, 25) * 60_000L
        date = LocalDate.ofEpochDay(intent.getLongExtra(EXTRA_DATE, LocalDate.now().toEpochDay()))
        doNotDisturb = intent.getBooleanExtra(EXTRA_DND, false)
        remainingMs = plannedMs
        binding.tvTaskTitle.text = taskTitle

        enterImmersiveMode()
        setupListeners()
        startSession()
        tickLoop()
    }

    private fun enterImmersiveMode() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun setupListeners() = with(binding) {
        btnPause.setOnClickListener { if (running) pause() else resume() }
        btnEnd.setOnClickListener { confirmEnd() }
        btnClose.setOnClickListener { finish() }
        btnMarkDone.setOnClickListener {
            val id = taskId ?: return@setOnClickListener
            lifecycleScope.launch {
                repository.setStatus(id, date, OccurrenceStatus.COMPLETED)
                finish()
            }
        }
        onBackPressedDispatcher.addCallback(this@FocusTimerActivity, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (finished) finish() else confirmEnd()
            }
        })
    }

    private fun startSession() {
        startedAt = System.currentTimeMillis()
        if (doNotDisturb) {
            val manager = getSystemService(NotificationManager::class.java)
            if (manager.isNotificationPolicyAccessGranted) {
                previousInterruptionFilter = manager.currentInterruptionFilter
                manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            }
        }
        resume()
    }

    private fun resume() {
        endAtElapsed = SystemClock.elapsedRealtime() + remainingMs
        running = true
        render()
    }

    private fun pause() {
        remainingMs = (endAtElapsed - SystemClock.elapsedRealtime()).coerceAtLeast(0)
        running = false
        pauseCount++
        render()
    }

    private fun tickLoop() {
        lifecycleScope.launch {
            while (isActive && !finished) {
                if (running) {
                    remainingMs = (endAtElapsed - SystemClock.elapsedRealtime()).coerceAtLeast(0)
                    if (remainingMs == 0L) {
                        complete(FocusResult.COMPLETED)
                        playChime()
                    }
                }
                render()
                delay(TICK_MS)
            }
        }
    }

    private fun confirmEnd() {
        if (finished) return
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.focus_end_title)
            .setMessage(R.string.focus_end_message)
            .setPositiveButton(R.string.focus_end) { _, _ ->
                if (running) remainingMs = (endAtElapsed - SystemClock.elapsedRealtime()).coerceAtLeast(0)
                complete(FocusResult.STOPPED_EARLY)
            }
            .setNegativeButton(R.string.focus_keep_going, null)
            .show()
    }

    private fun complete(result: FocusResult) {
        if (finished) return
        finished = true
        running = false
        restoreDoNotDisturb()
        val session = buildSession(result)
        lifecycleScope.launch { repository.saveFocusSession(session) }

        with(binding) {
            tvFocusLabel.setText(if (result == FocusResult.COMPLETED) R.string.focus_complete else R.string.focus_stopped)
            tvDoneSummary.text = resources.getQuantityString(
                R.plurals.focus_done_summary,
                pauseCount,
                pauseCount,
                Formatters.duration(this@FocusTimerActivity, session.actualSeconds),
            )
            btnPause.isVisible = false
            btnEnd.isVisible = false
            panelDone.isVisible = true
            btnMarkDone.isVisible = taskId != null
        }
        render()
    }

    private fun buildSession(result: FocusResult) = FocusSession(
        taskId = taskId,
        taskTitle = taskTitle,
        startedAt = startedAt,
        endedAt = System.currentTimeMillis(),
        plannedMinutes = (plannedMs / 60_000).toInt(),
        actualSeconds = (plannedMs - remainingMs) / 1000,
        pauseCount = pauseCount,
        result = result,
    )

    private fun render() = with(binding) {
        val totalSeconds = (remainingMs + 999) / 1000
        tvTime.text = String.format(Locale.US, "%02d:%02d", totalSeconds / 60, totalSeconds % 60)
        progressRing.setProgressCompat(if (plannedMs == 0L) 0 else (remainingMs * 1000 / plannedMs).toInt(), false)
        tvState.setText(
            when {
                finished -> R.string.focus_state_done
                running -> R.string.focus_state_running
                else -> R.string.focus_state_paused
            },
        )
        btnPause.setText(if (running) R.string.focus_pause else R.string.focus_resume)
        btnPause.setIconResource(if (running) R.drawable.ic_pause else R.drawable.ic_play)
    }

    private fun playChime() {
        runCatching {
            RingtoneManager.getRingtone(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))?.play()
        }
    }

    private fun restoreDoNotDisturb() {
        val previous = previousInterruptionFilter ?: return
        previousInterruptionFilter = null
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.isNotificationPolicyAccessGranted) manager.setInterruptionFilter(previous)
    }

    override fun onDestroy() {
        // Leaving without ending (e.g. the app was swiped away) still records the session.
        if (!finished && isFinishing) {
            if (running) remainingMs = (endAtElapsed - SystemClock.elapsedRealtime()).coerceAtLeast(0)
            finished = true
            val session = buildSession(FocusResult.STOPPED_EARLY)
            CoroutineScope(Dispatchers.IO).launch { repository.saveFocusSession(session) }
        }
        restoreDoNotDisturb()
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_TASK_ID = "task_id"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_MINUTES = "minutes"
        private const val EXTRA_DATE = "date"
        private const val EXTRA_DND = "dnd"
        private const val TICK_MS = 250L

        fun intent(
            context: Context,
            taskId: Long?,
            title: String,
            minutes: Int,
            date: LocalDate,
            doNotDisturb: Boolean,
        ): Intent = Intent(context, FocusTimerActivity::class.java)
            .putExtra(EXTRA_TASK_ID, taskId ?: -1L)
            .putExtra(EXTRA_TITLE, title)
            .putExtra(EXTRA_MINUTES, minutes)
            .putExtra(EXTRA_DATE, date.toEpochDay())
            .putExtra(EXTRA_DND, doNotDisturb)
    }
}
