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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    val currentLanguage by dataStore.languageFlow.collectAsState(initial = "vi")
    val scrollState = rememberScrollState()

    // Sử dụng bảng màu chuẩn của App để đồng bộ
    val colorScheme = MaterialTheme.colorScheme
    val primaryColor = MauNhanTym
    val accentColor = MauNutCam
    val surfaceColor = colorScheme.surface
    val onSurfaceColor = colorScheme.onSurface
    val bgColor = colorScheme.background

    var currentQuote by remember { mutableStateOf<Quote?>(null) }
    var isAiLoading by remember { mutableStateOf(false) }
    var currentSoundRes by remember { mutableIntStateOf(0) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    
    val rainResId = remember { context.resources.getIdentifier("rain", "raw", context.packageName) }
    val forestResId = remember { context.resources.getIdentifier("forest", "raw", context.packageName) }

    val strings = remember(currentLanguage) {
        if (currentLanguage == "vi") {
            mapOf(
                "strict" to "Nghiêm ngặt",
                "whitelist" to "Cho phép",
                "goal" to "Mục tiêu",
                "start" to "BẮT ĐẦU",
                "stop" to "DỪNG LẠI",
                "warning" to "Cảnh báo!",
                "warning_text" to "Dừng lại sẽ bị trừ 50 🪙 và cây sẽ CHẾT! Bạn chắc chứ?",
                "back" to "Quay lại",
                "save" to "Lưu",
                "hint" to "Nhập mục tiêu của bạn..."
            )
        } else {
            mapOf(
                "strict" to "Strict Mode",
                "whitelist" to "Whitelist",
                "goal" to "Goal",
                "start" to "START",
                "stop" to "STOP",
                "warning" to "Warning!",
                "warning_text" to "Stopping will cost 50 🪙 and plants will DIE! Sure?",
                "back" to "Go back",
                "save" to "Save",
                "hint" to "Enter your focus goal..."
            )
        }
    }

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
                mediaPlayer = MediaPlayer.create(context, currentSoundRes).apply {
                    isLooping = true
                    start()
                }
            } catch (e: Exception) { currentSoundRes = 0 }
        } else {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    var currentTask by remember { mutableStateOf("") }
    var showTaskDialog by remember { mutableStateOf(false) }
    var tempTaskInput by remember { mutableStateOf("") }
    var isAnalyzingTask by remember { mutableStateOf(false) }
    var showWarningDialog by remember { mutableStateOf(false) }
    var isPlantDead by remember { mutableStateOf(false) }
    val selectedTimerMode by timerViewModel.selectedTimerMode
    val isStrictMode by timerViewModel.isStrictMode

    var countdownTime by remember { mutableFloatStateOf(25f) }
    var pomoFocusTime by remember { mutableFloatStateOf(25f) }
    var pomoBreakTime by remember { mutableFloatStateOf(5f) }
    var showSoundPicker by remember { mutableStateOf(false) }

    fun analyzeTaskWithAi() {
        if (tempTaskInput.isBlank()) return
        scope.launch {
            isAnalyzingTask = true
            tempTaskInput = GeminiService.analyzeTask(tempTaskInput, currentLanguage)
            isAnalyzingTask = false
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = bgColor,
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
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // HEADER: MODE SWITCHER
            AnimatedVisibility(visible = !timerViewModel.isRunning.value) {
                Surface(
                    color = surfaceColor,
                    shape = RoundedCornerShape(32.dp),
                    modifier = Modifier.fillMaxWidth().height(64.dp).border(1.dp, primaryColor.copy(alpha = 0.2f), RoundedCornerShape(32.dp)),
                    shadowElevation = 2.dp
                ) {
                    Row(modifier = Modifier.fillMaxSize().padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        TabModeItem(icon = "⏳", label = "Count", isSelected = selectedTimerMode == 0, primaryColor = primaryColor, modifier = Modifier.weight(1f)) {
                            timerViewModel.selectedTimerMode.value = 0
                            timerViewModel.setTime(countdownTime, false)
                        }
                        TabModeItem(icon = "🍅", label = "Pomo", isSelected = selectedTimerMode == 1, primaryColor = primaryColor, modifier = Modifier.weight(1f)) {
                            timerViewModel.selectedTimerMode.value = 1
                            timerViewModel.setTime(pomoFocusTime, false)
                        }
                        TabModeItem(icon = "⏱️", label = "Stop", isSelected = selectedTimerMode == 2, primaryColor = primaryColor, modifier = Modifier.weight(1f)) {
                            timerViewModel.selectedTimerMode.value = 2
                            timerViewModel.setTime(0f, true)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // MAIN TIMER CARD
            Surface(
                color = surfaceColor,
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier.fillMaxWidth().aspectRatio(1f).border(1.dp, primaryColor.copy(alpha = 0.1f), RoundedCornerShape(32.dp)),
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // Vòng tròn tiến trình Aura
                    Canvas(modifier = Modifier.size(280.dp)) {
                        val strokeWidth = 12.dp.toPx()
                        drawArc(color = onSurfaceColor.copy(alpha = 0.05f), startAngle = 0f, sweepAngle = 360f, useCenter = false, style = Stroke(width = strokeWidth))

                        val progress = if (timerViewModel.isRunning.value) {
                            if (timerViewModel.isCountUp.value) 1f
                            else timerViewModel.timeLeft.value.toFloat() / (timerViewModel.totalTime.value.toFloat().coerceAtLeast(1f))
                        } else 1f

                        drawArc(
                            brush = Brush.sweepGradient(listOf(primaryColor, primaryColor.copy(alpha = 0.4f), primaryColor)),
                            startAngle = -90f,
                            sweepAngle = progress * 360f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth + 2f, cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = if(isPlantDead) "🥀" else "🌿", fontSize = 48.sp, modifier = Modifier.animateContentSize())
                        Text(
                            text = timerViewModel.formatTime(),
                            style = TextStyle(
                                color = onSurfaceColor,
                                fontSize = 72.sp,
                                fontWeight = FontWeight.ExtraBold,
                                shadow = Shadow(color = primaryColor.copy(alpha = 0.3f), blurRadius = 15f)
                            )
                        )
                        if (timerViewModel.isRunning.value && currentTask.isNotBlank()) {
                            Text(
                                text = currentTask,
                                color = primaryColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SLIDER / SETTINGS CARD
            AnimatedVisibility(visible = !timerViewModel.isRunning.value) {
                Surface(
                    color = surfaceColor,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Settings, null, tint = primaryColor, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(strings["strict"] ?: "", color = onSurfaceColor, fontWeight = FontWeight.Bold)
                            }
                            Switch(
                                checked = isStrictMode,
                                onCheckedChange = { timerViewModel.toggleStrictMode(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = primaryColor)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        when (selectedTimerMode) {
                            0 -> CustomDotSlider(value = countdownTime, onValueChange = { val snapped = (it * 2).roundToInt() / 2f; countdownTime = snapped; timerViewModel.setTime(snapped, false) }, valueRange = 0.5f..120f, activeColor = primaryColor)
                            1 -> {
                                Text("Focus: ${pomoFocusTime.toInt()}m", color = onSurfaceColor.copy(alpha = 0.6f), fontSize = 12.sp)
                                CustomDotSlider(value = pomoFocusTime, onValueChange = { pomoFocusTime = it.roundToInt().toFloat(); timerViewModel.setTime(pomoFocusTime, false) }, valueRange = 5f..60f, activeColor = primaryColor)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Break: ${pomoBreakTime.toInt()}m", color = onSurfaceColor.copy(alpha = 0.6f), fontSize = 12.sp)
                                CustomDotSlider(value = pomoBreakTime, onValueChange = { pomoBreakTime = it.roundToInt().toFloat() }, valueRange = 1f..15f, activeColor = Color(0xFF4ADE80))
                            }
                        }
                    }
                }
            }

            // QUICK ACTIONS PANEL
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionChip(
                    icon = Icons.Default.Star,
                    label = strings["whitelist"] ?: "",
                    primaryColor = primaryColor,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToWhitelist
                )
                ActionChip(
                    icon = Icons.Default.Edit,
                    label = if (currentTask.isBlank()) strings["goal"] ?: "" else currentTask,
                    primaryColor = primaryColor,
                    modifier = Modifier.weight(1f),
                    onClick = { showTaskDialog = true }
                )
                Surface(
                    color = surfaceColor,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.size(54.dp).clickable { showSoundPicker = true },
                    shadowElevation = 2.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = if (currentSoundRes != 0) Icons.Default.MusicNote else Icons.Default.MusicOff, contentDescription = null, tint = if (currentSoundRes != 0) primaryColor else onSurfaceColor.copy(alpha = 0.3f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // START/STOP BUTTON
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
                colors = ButtonDefaults.buttonColors(containerColor = if (timerViewModel.isRunning.value) accentColor else primaryColor),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().height(72.dp).drawBehind {
                    val shadowColor = if (timerViewModel.isRunning.value) accentColor else primaryColor
                    drawCircle(color = shadowColor.copy(alpha = 0.15f), radius = size.width / 2, center = center)
                }
            ) {
                Text(if (timerViewModel.isRunning.value) strings["stop"] ?: "" else strings["start"] ?: "", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // QUOTE SECTION
            if (currentQuote != null) {
                Surface(color = onSurfaceColor.copy(alpha = 0.05f), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "\"${currentQuote?.q}\"", fontSize = 14.sp, fontStyle = FontStyle.Italic, color = onSurfaceColor.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                        Text(text = "- ${currentQuote?.a}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = primaryColor, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }

        // DIALOGS
        if (showSoundPicker) {
            AlertDialog(
                onDismissRequest = { showSoundPicker = false },
                containerColor = surfaceColor,
                title = { Text("Ambient Sound", color = onSurfaceColor, fontWeight = FontWeight.Bold) },
                text = { Column {
                    SoundItem(name = "None", isSelected = currentSoundRes == 0, primaryColor = primaryColor) { currentSoundRes = 0; showSoundPicker = false }
                    SoundItem(name = "🌧️ Rainy Night", isSelected = currentSoundRes == rainResId, primaryColor = primaryColor) { if (rainResId != 0) { currentSoundRes = rainResId; showSoundPicker = false } }
                    SoundItem(name = "🌲 Forest Birds", isSelected = currentSoundRes == forestResId, primaryColor = primaryColor) { if (forestResId != 0) { currentSoundRes = forestResId; showSoundPicker = false } }
                } },
                confirmButton = { TextButton(onClick = { showSoundPicker = false }) { Text("Close", color = primaryColor) } }
            )
        }

        if (showTaskDialog) {
            AlertDialog(
                onDismissRequest = { showTaskDialog = false },
                containerColor = surfaceColor,
                modifier = Modifier.border(1.dp, primaryColor.copy(alpha = 0.2f), RoundedCornerShape(28.dp)),
                title = {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(strings["goal"] ?: "", color = onSurfaceColor, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { analyzeTaskWithAi() }) {
                            if (isAnalyzingTask) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = primaryColor, strokeWidth = 2.dp)
                            else Icon(Icons.Default.AutoAwesome, null, tint = primaryColor)
                        }
                    }
                },
                text = { OutlinedTextField(value = tempTaskInput, onValueChange = { tempTaskInput = it }, placeholder = { Text(strings["hint"] ?: "") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = onSurfaceColor.copy(alpha = 0.2f), focusedBorderColor = primaryColor, focusedTextColor = onSurfaceColor, unfocusedTextColor = onSurfaceColor)) },
                confirmButton = { Button(onClick = { if(tempTaskInput.isNotBlank()) currentTask = tempTaskInput.replace("\n", " | "); showTaskDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = primaryColor)) { Text(strings["save"] ?: "") } }
            )
        }

        if (showWarningDialog) {
            AlertDialog(
                onDismissRequest = { showWarningDialog = false },
                containerColor = surfaceColor,
                title = { Text(strings["warning"] ?: "", color = onSurfaceColor) },
                text = { Text(strings["warning_text"] ?: "", color = onSurfaceColor.copy(alpha = 0.7f)) },
                confirmButton = { Button(onClick = { showWarningDialog = false; timerViewModel.stopTimer(); isPlantDead = true; scope.launch { val currentCoins = dataStore.userCoinsFlow.first(); dataStore.saveUserCoins(maxOf(0, currentCoins - 50)); database.auraDao().deleteAllPlants(); Toast.makeText(context, "Penalty: -50 🪙", Toast.LENGTH_SHORT).show() } }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text(strings["stop"] ?: "") } },
                dismissButton = { TextButton(onClick = { showWarningDialog = false }) { Text(strings["back"] ?: "", color = onSurfaceColor) } }
            )
        }
    }
}

@Composable
fun SoundItem(name: String, isSelected: Boolean, primaryColor: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(name, color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isSelected) 1f else 0.6f), fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
        if (isSelected) Icon(Icons.Default.Check, null, tint = primaryColor)
    }
}

@Composable
fun ActionChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, primaryColor: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.height(54.dp).clickable { onClick() },
        shadowElevation = 2.dp
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = primaryColor, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun TabModeItem(icon: String, label: String, isSelected: Boolean, primaryColor: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier.fillMaxHeight().clip(RoundedCornerShape(24.dp))
            .background(if (isSelected) primaryColor else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = icon, fontSize = 18.sp)
            if (isSelected) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun CustomDotSlider(value: Float, onValueChange: (Float) -> Unit, valueRange: ClosedFloatingPointRange<Float>, activeColor: Color) {
    Box(modifier = Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)))
        Canvas(modifier = Modifier.fillMaxWidth().height(8.dp)) {
            val fraction = (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
            drawRoundRect(color = activeColor, size = Size(size.width * fraction, size.height), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()))
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(thumbColor = activeColor, activeTrackColor = Color.Transparent, inactiveTrackColor = Color.Transparent)
        )
    }
}
