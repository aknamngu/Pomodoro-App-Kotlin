package com.example.pomodoro2

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val dataStore = AuraDataStore(application)
    
    // Biến này sẽ giữ trạng thái Sáng/Tối cho cả App
    var isDarkMode = mutableStateOf(false)

    init {
        // Tải trạng thái từ DataStore khi khởi tạo
        viewModelScope.launch {
            dataStore.isDarkModeFlow.collectLatest { dark ->
                isDarkMode.value = dark
            }
        }
    }

    fun toggleTheme() {
        val newValue = !isDarkMode.value
        isDarkMode.value = newValue
        viewModelScope.launch {
            dataStore.saveDarkMode(newValue)
        }
    }
}
