package com.example.pomodoro2

import android.content.Intent
import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pomodoro2.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    onNavigateToTasks: () -> Unit = {},
    onNavigateToMotivation: () -> Unit = {},
    onNavigateToFocus: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToWhitelist: () -> Unit = {}
) {
    val context = LocalContext.current
    val timerViewModel: TimerViewModel = viewModel()
    val dataStore = remember { AuraDataStore(context) }
    val scope = rememberCoroutineScope()
    val database = remember { AuraDatabase.getDatabase(context) }

    val colorScheme = MaterialTheme.colorScheme
    val currentLanguage by dataStore.languageFlow.collectAsState(initial = "vi")
    
    val scrollState = rememberScrollState()

    var currentQuote by remember { mutableStateOf<Quote?>(null) }
    var isAiLoading by remember { mutableStateOf(false) }
    
    var currentSoundRes by remember { mutableIntStateOf(0) } 
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    
    val rainResId = remember { context.resources.getIdentifier("rain", "raw", context.packageName) }
    val forestResId = remember { context.resources.getIdentifier("forest", "raw", context.packageName) }

    fun fetchAiQuote() {
        scope.launch {
            isAiLoading = true
            try {
                val quotes = QuotesApi.retrofitService.getRandomQuote()
                if (quotes.isNotEmpty()) currentQuote = quotes[0]
            } catch (e: Exception) {
                currentQuote = Quote("Hãy tin rằng bạn có thể làm được!", "Aura AI", "")
            } finally {
                isAiLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { fetchAiQuote() }

    LaunchedEffect(timerViewModel.isRunning.value, currentSoundRes) {
        if (timerViewModel.isRunning.value && currentSoundRes != 0) {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                val player = MediaPlayer.create(context, currentSoundRes).apply {
                    isLooping = true
                    start()
                }
                mediaPlayer = player
            } catch (e: Exception) {
                currentSoundRes = 0
            }
        } else {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    val unitMinutes = if (currentLanguage == "vi") "Phút" else "Mins"
    val strictLabel = if (currentLanguage == "vi") "Nghiêm ngặt" else "Strict"
    val whitelistLabel = if (currentLanguage == "vi") "Danh sách trắng" else "Whitelist"
    val startLabel = if (currentLanguage == "vi") "Bắt đầu" else "Start"
    val stopLabel = if (currentLanguage == "vi") "Dừng lại" else "Stop"
    val goalTitle = if (currentLanguage == "vi") "Mục tiêu của bạn" else "Your Goal"
    val goalHint = if (currentLanguage == "vi") "Nhập mục tiêu tập trung..." else "Enter focus goal..."
    val saveLabel = if (currentLanguage == "vi") "Lưu" else "Save"
    val warningTitle = if (currentLanguage == "vi") "Cảnh báo!" else "Warning!"
    val warningText = if (currentLanguage == "vi") "Dừng lại bây giờ bạn sẽ bị trừ 50 🪙 và cây trong vườn sẽ CHẾT! Bạn có chắc không?" else "Stopping now will cost 50 🪙 and your plants will DIE! Are you sure?"
    val backLabel = if (currentLanguage == "vi") "Quay lại" else "Go back"

    var currentTask by remember { mutableStateOf("") }
    LaunchedEffect(currentLanguage) {
        if (currentTask == "" || currentTask == "Goal" || currentTask == "Mục tiêu") {
            currentTask = goalTitle
        }
    }

    var showTaskDialog by remember { mutableStateOf(false) }
    var tempTaskInput by remember { mutableStateOf("") }
    var isAnalyzingTask by remember { mutableStateOf(false) }
    var showWarningDialog by remember { mutableStateOf(false) }
    var isPlantDead by remember { mutableStateOf(false) }
    val selectedTimerMode by timerViewModel.selectedTimerMode
    val isStrictMode by timerViewModel.isStrictMode

    var countdownTime by remember { mutableFloatStateOf(0.5f) }
    var pomoFocusTime by remember { mutableFloatStateOf(25f) }
    var pomoBreakTime by remember { mutableFloatStateOf(5f) }

    var showSoundPicker by remember { mutableStateOf(false) }

    fun analyzeTaskWithAi() {
        if (tempTaskInput.isBlank()) return
        scope.launch {
            isAnalyzingTask = true
            delay(1500)
            val original = tempTaskInput.lowercase()
            tempTaskInput = when {
                original.contains("code") || original.contains("lập trình") -> 
                    "1. Chuẩn bị tài liệu\n2. Đọc lướt lý thuyết\n3. Thực hành gõ code\n4. Fix lỗi & Ghi chú"
                original.contains("học") || original.contains("đọc") -> 
                    "1. Chuẩn bị tài liệu\n2. Đọc lướt nội dung\n3. Ghi chú ý chính\n4. Ôn tập nhanh"
                else -> "1. Xác định mục tiêu\n2. Chuẩn bị công cụ\n3. Tập trung thực hiện\n4. Tổng kết kết quả"
            }
            isAnalyzingTask = false
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colorScheme.background,
        bottomBar = {
            AppBottomNavigation(
                currentRoute = "focus",
                onNavigateToTasks = onNavigateToTasks,
                onNavigateToMotivation = onNavigateToMotivation,
                onNavigateToFocus = {},
                onNavigateToHistory = onNavigateToHistory,
                onNavigateToSettings = onNavigateToSettings,
                language = currentLanguage
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // PHẦN NỘI DUNG CUỘN
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // CHỌN CHẾ ĐỘ
                AnimatedVisibility(visible = !timerViewModel.isRunning.value) {
                    Surface(
                        color = colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(30.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                            TabModeItem(icon = "⏳", isSelected = selectedTimerMode == 0, modifier = Modifier.weight(1f)) {
                                timerViewModel.selectedTimerMode.value = 0
                                timerViewModel.setTime(countdownTime, false)
                            }
                            TabModeItem(icon = "🍅", isSelected = selectedTimerMode == 1, modifier = Modifier.weight(1f)) {
                                timerViewModel.selectedTimerMode.value = 1
                                timerViewModel.setTime(pomoFocusTime, false)
                            }
                            TabModeItem(icon = "⏱️", isSelected = selectedTimerMode == 2, modifier = Modifier.weight(1f)) {
                                timerViewModel.selectedTimerMode.value = 2
                                timerViewModel.setTime(0f, true)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // TIÊU ĐỀ & NGHIÊM NGẶT
                AnimatedVisibility(visible = !timerViewModel.isRunning.value) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val timerTitle = when(selectedTimerMode) {
                                0 -> if(currentLanguage == "vi") "Hẹn giờ" else "Countdown"
                                1 -> "Pomodoro"
                                else -> if(currentLanguage == "vi") "Bấm giờ" else "Stopwatch"
                            }
                            Text(text = timerTitle, fontWeight = FontWeight.Bold, color = colorScheme.onBackground, fontSize = 28.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = { showSoundPicker = true }) {
                                Icon(imageVector = if (currentSoundRes != 0) Icons.Default.MusicNote else Icons.Default.MusicOff, contentDescription = null, tint = if (currentSoundRes != 0) colorScheme.primary else colorScheme.onBackground.copy(alpha = 0.4f))
                            }
                        }
                        Surface(color = colorScheme.primary.copy(alpha = 0.1f), shape = RoundedCornerShape(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                Text(strictLabel, fontSize = 14.sp, color = colorScheme.primary, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(checked = isStrictMode, onCheckedChange = { timerViewModel.toggleStrictMode(it) }, modifier = Modifier.scale(0.8f), colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = colorScheme.primary))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // KHU VỰC TRUNG TÂM
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (!timerViewModel.isRunning.value) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(color = colorScheme.primaryContainer, shape = RoundedCornerShape(24.dp)) {
                                Row(modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("💧", fontSize = 22.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    val rewardDrops = when (selectedTimerMode) {
                                        0 -> if (countdownTime < 1f) 1 else countdownTime.toInt()
                                        1 -> if (pomoFocusTime < 1f) 4 else (pomoFocusTime.toInt() * 4)
                                        else -> 0
                                    }
                                    val rewardText = if (selectedTimerMode == 2) (if (currentLanguage == "vi") "10💧 / 5 phút" else "10💧 / 5 mins") else "+ $rewardDrops"
                                    Text(rewardText, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colorScheme.onPrimaryContainer)
                                }
                            }
                            Spacer(modifier = Modifier.height(30.dp))
                            when (selectedTimerMode) {
                                0 -> {
                                    val mins = countdownTime.toInt()
                                    val secs = ((countdownTime - mins) * 60).roundToInt()
                                    Text(String.format(Locale.getDefault(), "%02d:%02d", mins, secs), fontSize = 64.sp, fontWeight = FontWeight.Bold, color = colorScheme.onBackground)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    CustomDotSlider(value = countdownTime, onValueChange = { val snapped = (it * 2).roundToInt() / 2f; countdownTime = snapped; timerViewModel.setTime(snapped, false) }, valueRange = 0.5f..120f, activeColor = colorScheme.primary)
                                }
                                1 -> {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(if (currentLanguage == "vi") "Tập trung: ${pomoFocusTime.toInt()} Phút" else "Focus: ${pomoFocusTime.toInt()} Mins", color = colorScheme.onBackground, fontWeight = FontWeight.Bold)
                                        CustomDotSlider(value = pomoFocusTime, onValueChange = { pomoFocusTime = it.roundToInt().toFloat(); timerViewModel.setTime(pomoFocusTime, false) }, valueRange = 5f..60f, activeColor = colorScheme.primary)
                                        Spacer(modifier = Modifier.height(20.dp))
                                        Text(if (currentLanguage == "vi") "Giải lao: ${pomoBreakTime.toInt()} Phút" else "Break: ${pomoBreakTime.toInt()} Mins", color = colorScheme.onBackground, fontWeight = FontWeight.Bold)
                                        CustomDotSlider(value = pomoBreakTime, onValueChange = { pomoBreakTime = it.roundToInt().toFloat() }, valueRange = 1f..15f, activeColor = Color(0xFF66BB6A))
                                    }
                                }
                                2 -> { Text(if(currentLanguage == "vi") "Bấm giờ để theo dõi việc học." else "Stopwatch counts up.", color = colorScheme.onBackground.copy(alpha = 0.6f)) }
                            }
                        }
                    } else {
                        // VIEW ĐANG CHẠY - ĐÃ FIX LỖI ĐÈ CHỮ
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(300.dp)) {
                                Canvas(modifier = Modifier.size(260.dp)) {
                                    drawArc(color = colorScheme.surfaceVariant.copy(alpha = 0.3f), startAngle = -90f, sweepAngle = 360f, useCenter = false, style = Stroke(width = 12f, cap = StrokeCap.Round))
                                    val progressAngle = if (timerViewModel.isCountUp.value) 360f else (timerViewModel.timeLeft.value.toFloat() / (timerViewModel.totalTime.value.toFloat().takeIf { it > 0f } ?: 1f)) * 360f
                                    drawArc(brush = Brush.linearGradient(colors = listOf(colorScheme.primary, colorScheme.primary.copy(alpha = 0.6f))), startAngle = -90f, sweepAngle = progressAngle, useCenter = false, style = Stroke(width = 16f, cap = StrokeCap.Round))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(if(isPlantDead) "🥀" else "🌿", fontSize = 56.sp)
                                    Text(timerViewModel.formatTime(), fontSize = 68.sp, fontWeight = FontWeight.ExtraBold, color = colorScheme.onBackground)
                                }
                            }

                            Spacer(modifier = Modifier.height(30.dp))

                            if (currentTask.isNotBlank()) {
                                Surface(
                                    color = colorScheme.primaryContainer.copy(alpha = 0.9f),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth(0.85f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.Flag, null, tint = colorScheme.primary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(currentTask, color = colorScheme.onPrimaryContainer, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            val progressValue = if (timerViewModel.isCountUp.value) 1f else timerViewModel.timeLeft.value.toFloat() / (timerViewModel.totalTime.value.toFloat().takeIf { it > 0f } ?: 1f)
                            LinearProgressIndicator(progress = { progressValue }, modifier = Modifier.fillMaxWidth(0.7f).height(8.dp).clip(RoundedCornerShape(4.dp)), color = colorScheme.primary, trackColor = colorScheme.surfaceVariant)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // CÂU ĐỘNG LỰC
                if (!timerViewModel.isRunning.value && currentQuote != null) {
                    Surface(color = colorScheme.primary.copy(alpha = 0.05f), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "\"${currentQuote?.q ?: ""}\"", fontSize = 15.sp, fontStyle = FontStyle.Italic, color = colorScheme.onBackground, textAlign = TextAlign.Center)
                            Text(text = "- ${currentQuote?.a}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // CÁC NÚT PHỤ
                if (!timerViewModel.isRunning.value) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(color = colorScheme.primary.copy(alpha = 0.15f), shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f).clickable { onNavigateToWhitelist() }) {
                            Row(modifier = Modifier.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Icon(Icons.Default.Star, null, tint = colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(whitelistLabel, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
                            }
                        }
                        Surface(color = colorScheme.primary.copy(alpha = 0.15f), shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f).clickable { showTaskDialog = true }) {
                            Row(modifier = Modifier.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Icon(Icons.Default.Edit, null, tint = colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (currentTask == goalTitle) "Mục tiêu" else currentTask, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Surface(color = colorScheme.primary, shape = RoundedCornerShape(20.dp), modifier = Modifier.size(54.dp).clickable { fetchAiQuote() }) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isAiLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                else Icon(Icons.Default.AutoAwesome, null, tint = Color.White)
                            }
                        }
                    }
                }
            }

            // NÚT BẮT ĐẦU / DỪNG CỐ ĐỊNH Ở DƯỚI
            Box(modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Button(
                    onClick = {
                        if (timerViewModel.isRunning.value) {
                            if (selectedTimerMode == 2) timerViewModel.stopTimer() else showWarningDialog = true
                        } else {
                            val intent = Intent(context, TimerService::class.java).apply {
                                action = "START"
                                putExtra("DURATION", timerViewModel.timeLeft.value)
                                putExtra("TASK_NAME", currentTask)
                                putExtra("IS_COUNT_UP", selectedTimerMode == 2)
                            }
                            context.startForegroundService(intent)
                            isPlantDead = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (timerViewModel.isRunning.value) Color(0xFFFF6D7F) else colorScheme.primary),
                    shape = RoundedCornerShape(32.dp),
                    modifier = Modifier.height(64.dp).fillMaxWidth()
                ) {
                    Text(if (timerViewModel.isRunning.value) stopLabel else startLabel, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            }
        }

        // DIALOGS...
        if (showSoundPicker) {
            AlertDialog(onDismissRequest = { showSoundPicker = false }, title = { Text(if(currentLanguage=="vi") "Âm thanh tập trung" else "Focus Sound") }, text = { Column {
                SoundItem(name = if(currentLanguage=="vi") "Không có" else "None", isSelected = currentSoundRes == 0) { currentSoundRes = 0; showSoundPicker = false }
                SoundItem(name = "🌧️ Rainy Night", isSelected = currentSoundRes == rainResId && rainResId != 0) { if (rainResId != 0) { currentSoundRes = rainResId; showSoundPicker = false } }
                SoundItem(name = "🌲 Forest Birds", isSelected = currentSoundRes == forestResId && forestResId != 0) { if (forestResId != 0) { currentSoundRes = forestResId; showSoundPicker = false } }
            } }, confirmButton = { TextButton(onClick = { showSoundPicker = false }) { Text(backLabel) } })
        }

        if (showTaskDialog) {
            AlertDialog(
                onDismissRequest = { showTaskDialog = false }, containerColor = colorScheme.surface, modifier = Modifier.border(1.dp, colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(28.dp)),
                title = { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(goalTitle, color = colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    IconButton(onClick = { analyzeTaskWithAi() }) { if (isAnalyzingTask) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = colorScheme.primary, strokeWidth = 2.dp) else Icon(Icons.Default.AutoAwesome, null, tint = colorScheme.primary) }
                }},
                text = { OutlinedTextField(value = tempTaskInput, onValueChange = { tempTaskInput = it }, placeholder = { Text(goalHint) }, modifier = Modifier.fillMaxWidth(), minLines = 3, colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = colorScheme.primary.copy(alpha = 0.5f), focusedBorderColor = colorScheme.primary), shape = RoundedCornerShape(12.dp)) },
                confirmButton = { Button(onClick = { if(tempTaskInput.isNotBlank()) currentTask = tempTaskInput.replace("\n", " | "); showTaskDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6D7F)), shape = RoundedCornerShape(20.dp)) { Text(saveLabel, color = Color.White) } }
            )
        }

        if (showWarningDialog) {
            AlertDialog(
                onDismissRequest = { showWarningDialog = false }, containerColor = colorScheme.surface, title = { Text(warningTitle) }, text = { Text(warningText) },
                confirmButton = { Button(onClick = { showWarningDialog = false; timerViewModel.stopTimer(); isPlantDead = true; scope.launch { val currentCoins = dataStore.userCoinsFlow.first(); dataStore.saveUserCoins(maxOf(0, currentCoins - 50)); database.auraDao().deleteAllPlants(); Toast.makeText(context, if(currentLanguage=="vi") "Cây đã chết! Bạn bị trừ 50 🪙" else "Plants died! -50 🪙 penalty", Toast.LENGTH_LONG).show() } }, colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error)) { Text(stopLabel) } },
                dismissButton = { TextButton(onClick = { showWarningDialog = false }) { Text(backLabel) } }
            )
        }
    }
}

@Composable
fun CustomDotSlider(value: Float, onValueChange: (Float) -> Unit, valueRange: ClosedFloatingPointRange<Float>, activeColor: Color) {
    val trackHeight = 18.dp
    Box(modifier = Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.fillMaxWidth().height(trackHeight).clip(RoundedCornerShape(9.dp)).background(activeColor.copy(alpha = 0.15f)))
        Canvas(modifier = Modifier.fillMaxWidth().height(trackHeight)) {
            val width = size.width
            val height = size.height
            val centerY = height / 2
            val fraction = (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
            drawRoundRect(color = activeColor, size = Size(width * fraction, height), cornerRadius = androidx.compose.ui.geometry.CornerRadius(height / 2))
            val dotCount = 12
            val dotSpacing = width / (dotCount - 1)
            for (i in 0 until dotCount) { drawCircle(color = Color.White.copy(alpha = 0.6f), radius = 2.5.dp.toPx(), center = Offset(i * dotSpacing, centerY)) }
            drawRoundRect(color = activeColor, topLeft = Offset(width * fraction - 2.dp.toPx(), -6.dp.toPx()), size = Size(4.dp.toPx(), height + 12.dp.toPx()), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()))
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange, modifier = Modifier.fillMaxWidth(), colors = SliderDefaults.colors(thumbColor = Color.Transparent, activeTrackColor = Color.Transparent, inactiveTrackColor = Color.Transparent))
    }
}

@Composable
fun TabModeItem(icon: String, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Box(modifier = modifier.fillMaxHeight().padding(4.dp).clip(RoundedCornerShape(26.dp)).background(if (isSelected) colorScheme.primaryContainer else Color.Transparent).clickable { onClick() }, contentAlignment = Alignment.Center) { Text(text = icon, fontSize = 24.sp) }
}

@Composable
fun AppBottomNavigation(currentRoute: String, onNavigateToTasks: () -> Unit, onNavigateToMotivation: () -> Unit, onNavigateToFocus: () -> Unit, onNavigateToHistory: () -> Unit, onNavigateToSettings: () -> Unit, language: String = "vi") {
    val colorScheme = MaterialTheme.colorScheme
    NavigationBar(containerColor = colorScheme.background, tonalElevation = 0.dp) {
        NavigationBarItem(icon = { Icon(Icons.AutoMirrored.Filled.List, null) }, selected = currentRoute == "tasks", onClick = onNavigateToTasks, colors = NavigationBarItemDefaults.colors(indicatorColor = colorScheme.primaryContainer, selectedIconColor = colorScheme.primary, unselectedIconColor = colorScheme.onBackground.copy(alpha = 0.6f)))
        NavigationBarItem(icon = { Icon(Icons.Default.FavoriteBorder, null) }, selected = currentRoute == "motivation", onClick = onNavigateToMotivation, colors = NavigationBarItemDefaults.colors(indicatorColor = colorScheme.primaryContainer, selectedIconColor = colorScheme.primary, unselectedIconColor = colorScheme.onBackground.copy(alpha = 0.6f)))
        NavigationBarItem(icon = { Surface(shape = CircleShape, color = colorScheme.primary, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.padding(10.dp)) } }, selected = currentRoute == "focus", onClick = onNavigateToFocus, colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent))
        NavigationBarItem(icon = { Icon(Icons.Default.DateRange, null) }, selected = currentRoute == "history", onClick = onNavigateToHistory, colors = NavigationBarItemDefaults.colors(indicatorColor = colorScheme.primaryContainer, selectedIconColor = colorScheme.primary, unselectedIconColor = colorScheme.onBackground.copy(alpha = 0.6f)))
        NavigationBarItem(icon = { Icon(Icons.Default.Person, null) }, selected = currentRoute == "settings", onClick = onNavigateToSettings, colors = NavigationBarItemDefaults.colors(indicatorColor = colorScheme.primaryContainer, selectedIconColor = colorScheme.primary, unselectedIconColor = colorScheme.onBackground.copy(alpha = 0.6f)))
    }
}

@Composable
fun SoundItem(name: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = isSelected, onClick = onClick)
        Text(name, modifier = Modifier.padding(start = 8.dp))
    }
}
