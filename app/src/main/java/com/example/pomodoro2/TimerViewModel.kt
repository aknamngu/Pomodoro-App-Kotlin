package com.example.pomodoro2

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

class TimerViewModel(application: Application) : AndroidViewModel(application) {
    
    private val context = application.applicationContext
    private val dataStore = AuraDataStore(context)
    private var timerService: TimerService? = null
    private var isBound = false

    var timeLeft = mutableStateOf(25 * 60 * 1000L)
    var isRunning = mutableStateOf(false)
    var totalTime = mutableStateOf(25 * 60 * 1000L)
    var isCountUp = mutableStateOf(false)
    
    // Thêm trạng thái Strict Mode để UI quan sát
    var isStrictMode = mutableStateOf(false)

    // Lưu tab hiện tại: 0: Hẹn giờ, 1: Pomodoro, 2: Bấm giờ
    var selectedTimerMode = mutableStateOf(1)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as TimerService.TimerBinder
            timerService = binder.getService()
            isBound = true
            
            viewModelScope.launch {
                timerService?.timeLeft?.collectLatest { time ->
                    timeLeft.value = time
                }
            }
            viewModelScope.launch {
                timerService?.isRunning?.collectLatest { running ->
                    isRunning.value = running
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            timerService = null
            isBound = false
        }
    }

    init {
        val intent = Intent(context, TimerService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        
        // Tải trạng thái strict mode từ DataStore
        viewModelScope.launch {
            dataStore.isStrictModeFlow.collectLatest { strict ->
                isStrictMode.value = strict
            }
        }
    }

    fun toggleStrictMode(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.saveStrictMode(enabled)
        }
    }

    fun setTime(minutes: Float, countUp: Boolean = false) {
        isCountUp.value = countUp
        val millis = (minutes * 60 * 1000).toLong()
        timeLeft.value = millis
        totalTime.value = if (countUp) 3 * 60 * 60 * 1000L else millis
    }

    fun startTimer() {
        val intent = Intent(context, TimerService::class.java).apply {
            action = "START"
            putExtra("DURATION", timeLeft.value)
        }
        context.startForegroundService(intent)
    }

    fun pauseTimer() {
        val intent = Intent(context, TimerService::class.java).apply {
            action = "PAUSE"
        }
        context.startService(intent)
    }

    fun stopTimer() {
        val intent = Intent(context, TimerService::class.java).apply {
            action = "STOP"
        }
        context.startService(intent)
    }

    fun resetTimer() {
        stopTimer()
        timeLeft.value = totalTime.value
    }

    fun formatTime(): String {
        val totalSeconds = timeLeft.value / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            context.unbindService(serviceConnection)
            isBound = false
        }
    }
}
