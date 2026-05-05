package com.example.pomodoro2

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
// --- THÊM 2 DÒNG IMPORT NÀY CHO WIDGET ---
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
// -----------------------------------------
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class TimerService : LifecycleService() {

    private val CHANNEL_ID = "TimerServiceChannel"
    private val NOTIFICATION_ID = 1

    private var timerJob: Job? = null
    private val _timeLeft = MutableStateFlow(25 * 60 * 1000L)
    val timeLeft = _timeLeft.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning = _isRunning.asStateFlow()

    private var isStrictMode = false
    private var whitelistedPackages = mutableSetOf<String>()
    private var checkAppHandler = Handler(Looper.getMainLooper())

    private var currentTaskName = "Tập trung"
    private var activeTaskId: Int? = null
    private var initialDurationMinutes = 25
    private var currentLanguage = "vi"
    private var isCountUp = false

    private val binder = TimerBinder()

    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        addSystemAppsToWhitelist()

        val dataStore = AuraDataStore(this)

        lifecycleScope.launch {
            dataStore.languageFlow.collect {
                currentLanguage = it
            }
        }

        lifecycleScope.launch {
            combine(dataStore.isStrictModeFlow, _isRunning) { strict, running ->
                strict to running
            }.collect { (strict, running) ->
                isStrictMode = strict
                if (strict && running) {
                    startAppBlocking()
                } else {
                    stopAppBlocking()
                }
            }
        }

        lifecycleScope.launch {
            dataStore.whitelistedPackagesFlow.collect {
                whitelistedPackages.clear()
                addSystemAppsToWhitelist()
                whitelistedPackages.addAll(it)
            }
        }
    }

    private fun addSystemAppsToWhitelist() {
        whitelistedPackages.add(packageName)
        whitelistedPackages.add("com.android.settings")
        whitelistedPackages.add("com.android.systemui")

        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.resolveActivity(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
        } else {
            @Suppress("DEPRECATION")
            packageManager.resolveActivity(intent, PackageManager.GET_META_DATA)
        }

        resolveInfo?.activityInfo?.packageName?.let {
            whitelistedPackages.add(it)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            "START" -> {
                val duration = intent.getLongExtra("DURATION", 25 * 60 * 1000L)
                currentTaskName = intent.getStringExtra("TASK_NAME") ?: (if(currentLanguage=="vi") "Tập trung" else "Focusing")
                isCountUp = intent.getBooleanExtra("IS_COUNT_UP", false)
                // Fix lỗi initialDurationMinutes nếu nhảy từ Widget (lấy mặc định 25 nếu duration bị sai)
                initialDurationMinutes = if (duration > 0) (duration / (60 * 1000)).toInt() else 25
                if (initialDurationMinutes == 0) initialDurationMinutes = 25

                startTimer(duration)
            }
            "PAUSE" -> pauseTimer()
            "STOP" -> {
                if (isCountUp && _isRunning.value) {
                    onTimerFinished()
                } else {
                    stopTimer()
                }
            }
            "SET_ACTIVE_TASK" -> {
                val id = intent.getIntExtra("TASK_ID", -1)
                val title = intent.getStringExtra("TASK_TITLE")
                if (id != -1) {
                    activeTaskId = id
                    currentTaskName = title ?: (if(currentLanguage=="vi") "Nhiệm vụ" else "Task")
                }
            }
        }

        return START_STICKY
    }

    private fun startTimer(duration: Long) {
        if (duration > 0) {
            _timeLeft.value = duration
        }
        _isRunning.value = true

        timerJob?.cancel()
        timerJob = lifecycleScope.launch {
            while (_isRunning.value) {
                updateNotification(formatTime(_timeLeft.value))
                updateWidgetState() // <-- BÁO CHO WIDGET CẬP NHẬT MỖI GIÂY

                delay(1000)
                if (isCountUp) {
                    _timeLeft.value += 1000
                    if (_timeLeft.value >= 3 * 60 * 60 * 1000L) {
                        onTimerFinished()
                        break
                    }
                } else {
                    _timeLeft.value -= 1000
                    if (_timeLeft.value <= 0) {
                        onTimerFinished()
                        break
                    }
                }
            }
        }

        startForeground(NOTIFICATION_ID, createNotification(formatTime(_timeLeft.value)))
    }

    private fun pauseTimer() {
        _isRunning.value = false
        timerJob?.cancel()
        updateNotification((if(currentLanguage=="vi") "Đã tạm dừng - " else "Paused - ") + formatTime(_timeLeft.value))

        // Cập nhật Widget để đổi nút thành START
        lifecycleScope.launch { updateWidgetState() }
    }

    private fun stopTimer() {
        _isRunning.value = false
        timerJob?.cancel()
        stopForeground(true)

        // Reset Widget về 00:00
        lifecycleScope.launch { updateWidgetState() }
        stopSelf()
    }

    private fun onTimerFinished() {
        _isRunning.value = false
        timerJob?.cancel()

        lifecycleScope.launch {
            val database = AuraDatabase.getDatabase(this@TimerService)
            val dao = database.auraDao()
            val dataStore = AuraDataStore(this@TimerService)

            val finalMinutes = if (isCountUp) (_timeLeft.value / (60 * 1000)).toInt() else initialDurationMinutes

            dao.insertHistory(HistoryEntity(taskName = currentTaskName, durationMinutes = finalMinutes))

            val timeReward = (finalMinutes / 5) * 10
            val taskBonus = 20
            val totalDropsReward = timeReward + taskBonus

            val currentDrops = dataStore.auraDropsFlow.first()
            dataStore.saveAuraDrops(currentDrops + totalDropsReward)

            val currentXP = dataStore.userXPFlow.first()
            dataStore.saveUserXP(currentXP + 50)

            activeTaskId?.let { taskId ->
                val allTasks = dao.getAllTasks().first()
                allTasks.find { it.id == taskId }?.let { task ->
                    if (task.progress < task.maxProgress) {
                        dao.updateTask(task.copy(progress = task.progress + 1))
                    }
                }
                activeTaskId = null
            }

            val allTasks = dao.getAllTasks().first()
            val sessionTask = allTasks.find { it.isSystem && it.title.contains(if(currentLanguage=="vi") "phiên" else "session") }
            if (sessionTask != null && sessionTask.progress < sessionTask.maxProgress) {
                dao.updateTask(sessionTask.copy(progress = sessionTask.progress + 1))
            }

            val minutesTask = allTasks.find { it.isSystem && it.title.contains(if(currentLanguage=="vi") "tổng cộng" else "total") }
            if (minutesTask != null && minutesTask.progress < minutesTask.maxProgress) {
                dao.updateTask(minutesTask.copy(progress = minOf(minutesTask.maxProgress, minutesTask.progress + finalMinutes)))
            }

            updateNotification(if(currentLanguage=="vi") "Hoàn thành! +$totalDropsReward 💧 và +50 XP" else "Completed! +$totalDropsReward 💧 and +50 XP")

            updateWidgetState() // Báo Widget là đã xong
            stopForeground(true)
            stopSelf()
        }
    }

    private val appCheckRunnable = object : Runnable {
        override fun run() {
            if (_isRunning.value && isStrictMode) {
                checkForegroundApp()
                checkAppHandler.postDelayed(this, 500)
            }
        }
    }

    private fun startAppBlocking() {
        checkAppHandler.removeCallbacks(appCheckRunnable)
        checkAppHandler.post(appCheckRunnable)
    }

    private fun stopAppBlocking() {
        checkAppHandler.removeCallbacks(appCheckRunnable)
    }

    private fun checkForegroundApp() {
        val currentApp = getForegroundPackage()

        if (currentApp != null && currentApp != packageName && !whitelistedPackages.contains(currentApp)) {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(intent)

            checkAppHandler.post {
                Toast.makeText(this@TimerService, if(currentLanguage=="vi") "Đang trong chế độ Nghiêm ngặt! Chỉ dùng được app đã cho phép 🌿" else "Strict mode is ON! Only allowed apps can be used 🌿", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getForegroundPackage(): String? {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()

        val usageEvents = usageStatsManager.queryEvents(time - 5000, time)
        val event = UsageEvents.Event()
        var lastPackage: String? = null
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                lastPackage = event.packageName
            }
        }
        if (lastPackage != null) return lastPackage

        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 10000, time)
        if (stats != null && stats.isNotEmpty()) {
            return stats.maxByOrNull { it.lastTimeUsed }?.packageName
        }

        return null
    }

    private fun createNotification(content: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Pomodoro: $currentTaskName")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification(content: String) {
        val notification = createNotification(content)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID, "Timer Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun formatTime(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    // =====================================================================
    // THÊM HÀM NÀY ĐỂ TRUYỀN DỮ LIỆU SANG WIDGET
    // =====================================================================
    private suspend fun updateWidgetState() {
        try {
            val glanceId = GlanceAppWidgetManager(this)
                .getGlanceIds(PomodoroWidget::class.java).firstOrNull()

            glanceId?.let { id ->
                updateAppWidgetState(this, id) { prefs ->
                    prefs[PomodoroWidget.PrefTimeLeft] = formatTime(_timeLeft.value)
                    prefs[PomodoroWidget.PrefIsRunning] = _isRunning.value

                    // Tính toán % cho thanh Progress Bar Cyberpunk
                    val progress = if (isCountUp) {
                        0f
                    } else {
                        val totalMillis = initialDurationMinutes * 60 * 1000f
                        if (totalMillis > 0) (_timeLeft.value.toFloat() / totalMillis) else 0f
                    }
                    prefs[PomodoroWidget.PrefProgress] = progress.toString()
                }
                PomodoroWidget().update(this, id)
            }
        } catch (e: Exception) {
            Log.e("TimerService", "Lỗi cập nhật Widget: ${e.message}")
        }
    }
}