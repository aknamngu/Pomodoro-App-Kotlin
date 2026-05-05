package com.example.pomodoro2

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pomodoro2.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateToTasks: () -> Unit = {},
    onNavigateToMotivation: () -> Unit = {},
    onNavigateToFocus: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val database = remember { AuraDatabase.getDatabase(context) }
    val dao = database.auraDao()
    val dataStore = remember { AuraDataStore(context) }

    // SỬ DỤNG MaterialTheme.colorScheme ĐỂ ĐỒNG BỘ TỰ ĐỘNG VỚI HỆ THỐNG
    val colorScheme = MaterialTheme.colorScheme
    val currentLanguage by dataStore.languageFlow.collectAsState(initial = "vi")

    val historyList by dao.getAllHistory().collectAsState(initial = emptyList())
    val totalFocusTime by dao.getTotalFocusTime().collectAsState(initial = 0)

    // TỰ ĐỘNG ĐỔI ĐƠN VỊ THỜI GIAN
    val unitMin = if (currentLanguage == "vi") "p" else "m"
    var selectedPeriod by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colorScheme.background, // Nền màn hình đặc (ko trong suốt)
        bottomBar = {
            AppBottomNavigation(
                currentRoute = "history",
                onNavigateToTasks = onNavigateToTasks,
                onNavigateToMotivation = onNavigateToMotivation,
                onNavigateToFocus = onNavigateToFocus,
                onNavigateToHistory = onNavigateToHistory,
                onNavigateToSettings = onNavigateToSettings,
                language = currentLanguage
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if(currentLanguage == "vi") "Báo cáo" else "Report", 
                    fontWeight = FontWeight.Bold, color = colorScheme.onBackground, fontSize = 26.sp
                )
                Icon(Icons.Default.DateRange, "Calendar", tint = colorScheme.onBackground.copy(alpha = 0.5f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // TAB CHỌN THỜI GIAN
            Surface(
                color = colorScheme.onBackground.copy(alpha = 0.05f), 
                shape = RoundedCornerShape(30.dp), 
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    HistoryTabItem(if(currentLanguage == "vi") "Ngày" else "Day", isSelected = selectedPeriod == 0, modifier = Modifier.weight(1f)) { selectedPeriod = 0 }
                    HistoryTabItem(if(currentLanguage == "vi") "Tuần" else "Week", isSelected = selectedPeriod == 1, modifier = Modifier.weight(1f)) { selectedPeriod = 1 }
                    HistoryTabItem(if(currentLanguage == "vi") "Tháng" else "Month", isSelected = selectedPeriod == 2, modifier = Modifier.weight(1f)) { selectedPeriod = 2 }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // CARD TỔNG QUAN - Nền đặc từ Surface của Theme
            Surface(
                color = colorScheme.surface, 
                shape = RoundedCornerShape(20.dp), 
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(if(currentLanguage == "vi") "Tổng thời gian tập trung" else "Total focus time", fontSize = 14.sp, color = colorScheme.onBackground.copy(alpha = 0.5f))
                            val totalTime = totalFocusTime ?: 0
                            val hours = totalTime / 60
                            val minutes = totalTime % 60
                            Text("${hours}h ${minutes}$unitMin", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = MauNhanTym)
                        }
                        Text("🌿", fontSize = 40.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = colorScheme.onBackground.copy(alpha = 0.05f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        StatItem(if(currentLanguage == "vi") "Phiên" else "Sessions", "${historyList.size}")
                        StatItem(if(currentLanguage == "vi") "TB/phiên" else "Avg/session", if (historyList.isNotEmpty()) "${(totalFocusTime ?: 0) / historyList.size}$unitMin" else "0$unitMin")
                        StatItem(if(currentLanguage == "vi") "Nhiệm vụ" else "Tasks", "${historyList.distinctBy { it.taskName }.size}")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // BIỂU ĐỒ THỰC TẾ
            Text(if(currentLanguage == "vi") "Thống kê chi tiết" else "Detailed statistics", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Left, fontWeight = FontWeight.Bold, color = colorScheme.onBackground)
            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = colorScheme.surface, 
                shape = RoundedCornerShape(20.dp), 
                modifier = Modifier.fillMaxWidth().height(200.dp),
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                        val days = if(currentLanguage == "vi") listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN") else listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                        val dummyValues = listOf(0.4f, 0.7f, 0.2f, 0.9f, 0.5f, 0.3f, 0.8f)

                        days.forEachIndexed { index, day ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom, modifier = Modifier.fillMaxHeight()) {
                                val barHeight by animateFloatAsState(targetValue = dummyValues[index], animationSpec = tween(durationMillis = 1000), label = "")
                                Box(modifier = Modifier.width(12.dp).fillMaxHeight(barHeight).clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)).background(if (index == 3) MauNhanTym else MauNhanTym.copy(alpha = 0.3f)))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(day, fontSize = 10.sp, color = colorScheme.onBackground.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // DANH SÁCH NHẬT KÝ
            Text(if(currentLanguage == "vi") "Nhật ký gần đây" else "Recent logs", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Left, fontWeight = FontWeight.Bold, color = colorScheme.onBackground)
            Spacer(modifier = Modifier.height(8.dp))
            if (historyList.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(if(currentLanguage == "vi") "Chưa có nhật ký nào" else "No logs yet", color = colorScheme.onBackground.copy(alpha = 0.5f))
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(historyList) { history ->
                        SessionItem(history, unitMin)
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(title: String, value: String) {
    val colorScheme = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, fontSize = 12.sp, color = colorScheme.onBackground.copy(alpha = 0.5f))
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colorScheme.onBackground)
    }
}

@Composable
fun SessionItem(history: HistoryEntity, unitMin: String) {
    val colorScheme = MaterialTheme.colorScheme
    val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    val dateString = sdf.format(Date(history.timestamp))

    Surface(
        color = colorScheme.surface, 
        shape = RoundedCornerShape(16.dp), 
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 1.dp
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = MauNhanTym.copy(alpha = 0.1f), shape = RoundedCornerShape(10.dp), modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) { Text(history.icon, fontSize = 20.sp) }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(history.taskName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colorScheme.onBackground)
                Text(dateString, fontSize = 12.sp, color = colorScheme.onBackground.copy(alpha = 0.5f))
            }
            Text("${history.durationMinutes}$unitMin", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MauNhanTym)
        }
    }
}

@Composable
fun HistoryTabItem(title: String, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier.fillMaxHeight().padding(4.dp).clip(RoundedCornerShape(25.dp))
            .background(if (isSelected) MauNhanTym.copy(alpha = 0.15f) else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MauNhanTym else colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}
