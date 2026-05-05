package com.example.pomodoro2

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.pomodoro2.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    onNavigateToTasks: () -> Unit = {},
    onNavigateToMotivation: () -> Unit = {},
    onNavigateToFocus: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { AuraDatabase.getDatabase(context) }
    val dao = database.auraDao()
    val dataStore = remember { AuraDataStore(context) }

    val isDark = isSystemInDarkTheme()
    val colorScheme = MaterialTheme.colorScheme
    
    val mauNenChuan = colorScheme.background
    val mauCardChuan = if (isDark) Color(0xFF252538) else Color.White
    val mauChuChuan = colorScheme.onBackground

    val currentLanguage by dataStore.languageFlow.collectAsState(initial = "vi")
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var newTaskName by remember { mutableStateOf("") }
    
    var showGuide by remember { mutableStateOf(false) }
    var showDetailDialog by remember { mutableStateOf<TaskEntity?>(null) }

    var selectedDateTime by remember { mutableStateOf<Calendar?>(null) }
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    val taskList by dao.getAllTasks().collectAsState(initial = emptyList())
    val currentDrops by dataStore.auraDropsFlow.collectAsState(initial = 0)
    
    // TRẠNG THÁI CUỘN
    val scrollState = rememberScrollState()

    LaunchedEffect(currentLanguage, taskList) {
        val systemTasks = taskList.filter { it.isSystem }
        if (systemTasks.isNotEmpty()) {
            systemTasks.forEach { task ->
                val newTitle = when {
                    task.rewardDrops == 15 -> if(currentLanguage == "vi") "Hoàn thành 1 phiên tập trung" else "Complete 1 focus session"
                    task.rewardDrops == 50 -> if(currentLanguage == "vi") "Tập trung tổng cộng 60 phút" else "Focus for 60 minutes total"
                    task.rewardDrops == 100 -> if(currentLanguage == "vi") "Nâng cấp mầm cây lên giai đoạn mới" else "Upgrade sprout to a new stage"
                    else -> task.title
                }
                if (task.title != newTitle) {
                    dao.updateTask(task.copy(title = newTitle))
                }
            }
        } else if (taskList.isEmpty()) {
            val initialTasks = if (currentLanguage == "vi") {
                listOf(
                    TaskEntity(title = "Hoàn thành 1 phiên tập trung", rewardDrops = 15, maxProgress = 1, isSystem = true),
                    TaskEntity(title = "Tập trung tổng cộng 60 phút", rewardDrops = 50, maxProgress = 60, isSystem = true),
                    TaskEntity(title = "Nâng cấp mầm cây lên giai đoạn mới", rewardDrops = 100, maxProgress = 1, isSystem = true)
                )
            } else {
                listOf(
                    TaskEntity(title = "Complete 1 focus session", rewardDrops = 15, maxProgress = 1, isSystem = true),
                    TaskEntity(title = "Focus for 60 minutes total", rewardDrops = 50, maxProgress = 60, isSystem = true),
                    TaskEntity(title = "Upgrade sprout to a new stage", rewardDrops = 100, maxProgress = 1, isSystem = true)
                )
            }
            initialTasks.forEach { dao.insertTask(it) }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = mauNenChuan,
        bottomBar = {
            AppBottomNavigation(
                currentRoute = "tasks",
                onNavigateToTasks = onNavigateToTasks,
                onNavigateToMotivation = onNavigateToMotivation,
                onNavigateToFocus = onNavigateToFocus,
                onNavigateToHistory = onNavigateToHistory,
                onNavigateToSettings = onNavigateToSettings,
                language = currentLanguage
            )
        },
        floatingActionButton = {
            if (selectedTab == 1) {
                FloatingActionButton(
                    onClick = { 
                        newTaskName = ""
                        selectedDateTime = null
                        showAddTaskDialog = true 
                    },
                    containerColor = MauNhanTym,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { showGuide = true }) {
                    Text(if(currentLanguage=="vi") "Nhiệm vụ" else "Tasks", fontWeight = FontWeight.Bold, color = mauChuChuan, fontSize = 26.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.Info, contentDescription = null, tint = mauChuChuan.copy(alpha = 0.4f), modifier = Modifier.size(22.dp))
                }
                Surface(color = Color(0xFF64B5F6).copy(alpha = 0.2f), shape = RoundedCornerShape(16.dp)) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "💧", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "$currentDrops", color = Color(0xFF1E88E5), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                color = mauChuChuan.copy(alpha = 0.05f),
                shape = RoundedCornerShape(30.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    TaskTabItem(if(currentLanguage=="vi") "Hệ thống" else "System", isSelected = selectedTab == 0, modifier = Modifier.weight(1f)) { selectedTab = 0 }
                    TaskTabItem(if(currentLanguage=="vi") "Của tôi" else "Mine", isSelected = selectedTab == 1, modifier = Modifier.weight(1f)) { selectedTab = 1 }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            val filteredTasks = taskList.filter { it.isSystem == (selectedTab == 0) }

            if (filteredTasks.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("📝", fontSize = 60.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(if(currentLanguage=="vi") "Chưa có nhiệm vụ nào." else "No tasks yet.", color = mauChuChuan.copy(alpha = 0.5f))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    filteredTasks.forEach { task ->
                        TaskCardItem(
                            task = task, 
                            mauCard = mauCardChuan, 
                            textColor = mauChuChuan, 
                            currentLanguage = currentLanguage,
                            onDetailClick = { showDetailDialog = task },
                            onActionClick = {
                                val isDone = task.progress >= task.maxProgress
                                if (isDone && !task.isClaimed) {
                                    scope.launch {
                                        dao.updateTask(task.copy(isClaimed = true))
                                        dataStore.saveAuraDrops(currentDrops + task.rewardDrops)
                                        Toast.makeText(context, if(currentLanguage=="vi") "Nhận được +${task.rewardDrops} 💧" else "Received +${task.rewardDrops} 💧", Toast.LENGTH_SHORT).show()
                                    }
                                } else if (!isDone) {
                                    if (task.rewardDrops == 100) {
                                        onNavigateToMotivation()
                                    } else {
                                        val intent = Intent(context, TimerService::class.java).apply {
                                            action = "SET_ACTIVE_TASK"
                                            putExtra("TASK_ID", task.id)
                                            putExtra("TASK_TITLE", task.title)
                                        }
                                        context.startService(intent)
                                        onNavigateToFocus()
                                    }
                                }
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (showGuide) {
            GuideDialogItem(
                title = if(currentLanguage=="vi") "Hướng dẫn Nhiệm vụ" else "Task Guide",
                content = if(selectedTab == 0) listOf(
                    if(currentLanguage=="vi") "1. Đây là các nhiệm vụ từ hệ thống." else "1. These are system tasks.",
                    if(currentLanguage=="vi") "2. Hoàn thành để nhận thêm Giọt mưa 💧." else "2. Complete them to earn extra Aura Drops 💧.",
                    if(currentLanguage=="vi") "3. Nhấn 'Nhận' sau khi thanh tiến trình đầy." else "3. Tap 'Claim' when the progress bar is full."
                ) else listOf(
                    if(currentLanguage=="vi") "1. Bạn có thể tự tạo mục tiêu cho mình." else "1. You can create your own goals.",
                    if(currentLanguage=="vi") "2. Đặt lịch hẹn để nhận thông báo nhắc nhở." else "2. Set a schedule to get reminders.",
                    if(currentLanguage=="vi") "3. Nhấn 'Đi' để bắt đầu tập trung cho mục tiêu này." else "3. Tap 'Go' to start focusing on this goal."
                ),
                onDismiss = { showGuide = false }
            )
        }

        if (showAddTaskDialog) {
            AlertDialog(
                onDismissRequest = { showAddTaskDialog = false },
                containerColor = mauCardChuan,
                title = { Text(if(currentLanguage=="vi") "Mục tiêu & Lịch hẹn" else "Goal & Event", color = mauChuChuan, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newTaskName, onValueChange = { newTaskName = it },
                            label = { Text(if(currentLanguage=="vi") "Tên sự kiện / Nhiệm vụ" else "Event / Task Name") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = mauChuChuan, unfocusedTextColor = mauChuChuan)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable {
                                val now = Calendar.getInstance()
                                DatePickerDialog(context, { _, year, month, day ->
                                    TimePickerDialog(context, { _, hour, minute ->
                                        val cal = Calendar.getInstance().apply {
                                            set(year, month, day, hour, minute)
                                        }
                                        selectedDateTime = cal
                                    }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show()
                                }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
                            },
                            color = mauChuChuan.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DateRange, null, tint = MauNhanTym)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = if (selectedDateTime == null) (if(currentLanguage=="vi") "Chọn ngày & giờ (Không bắt buộc)" else "Pick Date & Time (Optional)")
                                           else dateFormatter.format(selectedDateTime!!.time),
                                    color = mauChuChuan.copy(alpha = 0.7f),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (newTaskName.isNotBlank()) {
                            scope.launch {
                                val randomReward = Random.nextInt(10, 51)
                                val newTask = TaskEntity(
                                    title = newTaskName, 
                                    rewardDrops = randomReward, 
                                    isSystem = false,
                                    eventTimestamp = selectedDateTime?.timeInMillis
                                )
                                val taskId = dao.insertTaskWithId(newTask).toInt()
                                if (selectedDateTime != null && selectedDateTime!!.timeInMillis > System.currentTimeMillis()) {
                                    scheduleNotification(context, newTask.copy(id = taskId), selectedDateTime!!.timeInMillis)
                                }
                                showAddTaskDialog = false
                            }
                        }
                    }, colors = ButtonDefaults.buttonColors(containerColor = MauNhanTym)) {
                        Text(if(currentLanguage=="vi") "Tạo" else "Create", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddTaskDialog = false }) { Text(if(currentLanguage=="vi") "Hủy" else "Cancel", color = mauChuChuan) }
                }
            )
        }

        // Dialog xem chi tiết tiến độ
        showDetailDialog?.let { task ->
            AlertDialog(
                onDismissRequest = { showDetailDialog = null },
                containerColor = mauCardChuan,
                title = { Text(task.title, color = mauChuChuan, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if(currentLanguage=="vi") "Tiến độ hiện tại" else "Current Progress",
                            color = mauChuChuan.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${task.progress} / ${task.maxProgress}",
                            color = MauNhanTym,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { task.progress.toFloat() / task.maxProgress.toFloat() },
                            modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)),
                            color = if (task.progress >= task.maxProgress) Color(0xFF66BB6A) else MauNhanTym,
                            trackColor = mauChuChuan.copy(alpha = 0.1f),
                        )
                        
                        if (!task.isSystem && task.eventTimestamp != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "⏰ ${dateFormatter.format(Date(task.eventTimestamp))}",
                                color = MauNhanTym,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showDetailDialog = null }, colors = ButtonDefaults.buttonColors(containerColor = MauNhanTym)) {
                        Text(if(currentLanguage=="vi") "Đóng" else "Close", color = Color.White)
                    }
                }
            )
        }
    }
}

@Composable
fun GuideDialogItem(title: String, content: List<String>, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF2C2C4E),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFFE57373).copy(alpha = 0.8f)).padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
                    content.forEach { line ->
                        Text(
                            text = line, color = Color.White, fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 6.dp), lineHeight = 20.sp
                        )
                    }
                }
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFE57373).copy(alpha = 0.8f)).clickable { onDismiss() }.padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

fun scheduleNotification(context: Context, task: TaskEntity, timeInMillis: Long) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, TaskReminderReceiver::class.java).apply {
        action = "com.example.pomodoro2.ACTION_TASK_REMINDER"
        putExtra("TASK_TITLE", task.title)
        putExtra("TASK_ID", task.id)
    }

    val pendingIntent = PendingIntent.getBroadcast(
        context,
        task.id,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
}

@Composable
fun TaskCardItem(
    task: TaskEntity, 
    mauCard: Color, 
    textColor: Color, 
    currentLanguage: String, 
    onDetailClick: () -> Unit,
    onActionClick: () -> Unit
) {
    val isDone = task.progress >= task.maxProgress
    val dateFormatter = remember { SimpleDateFormat("HH:mm - dd/MM", Locale.getDefault()) }
    
    Box(modifier = Modifier
        .fillMaxWidth()
        .background(mauCard, RoundedCornerShape(20.dp))
        .border(1.dp, textColor.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
        .clickable { onDetailClick() } // Bấm vào card để xem chi tiết
        .padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(color = if (task.isSystem) Color(0xFF81C784).copy(alpha = 0.2f) else MauNhanTym.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp), modifier = Modifier.size(50.dp)) {
                Icon(if (task.isSystem) Icons.Default.Star else Icons.Default.Check, null, tint = if (task.isSystem) Color(0xFF388E3C) else MauNhanTym, modifier = Modifier.padding(12.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(task.title, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                
                if (task.eventTimestamp != null) {
                    Text(
                        text = "⏰ ${dateFormatter.format(Date(task.eventTimestamp))}",
                        color = MauNhanTym,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Surface(color = Color(0xFF64B5F6).copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                        Text("💧 +${task.rewardDrops}", color = Color(0xFF1E88E5), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    LinearProgressIndicator(
                        progress = { task.progress.toFloat() / task.maxProgress.toFloat() },
                        modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = if (isDone) Color(0xFF66BB6A) else MauNhanTym,
                        trackColor = textColor.copy(alpha = 0.1f),
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onActionClick,
                enabled = !task.isClaimed,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (task.isClaimed) Color.LightGray else if (isDone) Color(0xFF66BB6A) else MauNutCam,
                    disabledContainerColor = Color.LightGray.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    if (task.isClaimed) (if(currentLanguage=="vi") "Đã nhận" else "Claimed") 
                    else if (isDone) (if(currentLanguage=="vi") "Nhận" else "Claim") 
                    else (if(currentLanguage=="vi") "Đi" else "Go"), 
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (task.isClaimed) Color.DarkGray else Color.White
                )
            }
        }
    }
}

@Composable
fun TaskTabItem(title: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier = modifier.fillMaxHeight().clip(RoundedCornerShape(30.dp)).background(if (isSelected) MauNhanTym else Color.Transparent).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Text(text = title, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp)
    }
}
