package com.example.pomodoro2

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Calendar

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val context = LocalContext.current
    val dataStore = remember { AuraDataStore(context) }
    val currentLanguage by dataStore.languageFlow.collectAsState(initial = "vi")
    
    var currentStep by remember { mutableIntStateOf(1) }
    var isScanning by remember { mutableStateOf(false) }

    var totalTimeMs by remember { mutableLongStateOf(0L) }
    var topApps by remember { mutableStateOf<List<AppUsageInfo>>(emptyList()) }

    val bgColor = Color(0xFFFFF9E6)
    val textColor = Color(0xFF5D4037)
    val buttonColor = Color(0xFF10B981)

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (currentStep == 2 && hasUsageStatsPermission(context)) {
                    isScanning = true 
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(isScanning) {
        if (isScanning) {
            withContext(Dispatchers.IO) {
                val stats = getRealUsageStats(context)
                totalTimeMs = stats.sumOf { it.timeInMs }
                topApps = stats.take(4)
                delay(1500)
            }
            isScanning = false
            currentStep = 3
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(colors = listOf(Color(0xFFFFFDE7), Color(0xFFFFE082)))),
        contentAlignment = Alignment.Center
    ) {
        if (currentStep == 5) {
            IconButton(onClick = onFinish, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).padding(top = 24.dp)) {
                Icon(Icons.Default.Close, contentDescription = if(currentLanguage=="vi") "Đóng" else "Close", tint = textColor.copy(alpha = 0.5f))
            }
        }

        AnimatedContent(
            targetState = currentStep,
            transitionSpec = { fadeIn(animationSpec = tween(500)) with fadeOut(animationSpec = tween(500)) },
            label = "OnboardingAnimation"
        ) { step ->
            Column(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (step) {
                    1 -> {
                        Surface(modifier = Modifier.size(100.dp), shape = CircleShape, color = Color(0xFFFF8A65)) {
                            Box(contentAlignment = Alignment.Center) { Text("🍅", fontSize = 60.sp) }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("AURA FLOW", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = textColor, letterSpacing = 2.sp)
                        Spacer(modifier = Modifier.height(32.dp))
                        Text(if(currentLanguage=="vi") "Đã thử nhiều ứng dụng tập trung,\nvẫn bị phân tâm?" else "Tried many focus apps,\nstill distracted?", fontSize = 18.sp, color = textColor, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
                    }

                    2 -> {
                        Text(if(currentLanguage=="vi") "Bạn sử dụng điện thoại bao lâu\nmỗi ngày?" else "How long do you use your phone\nevery day?", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textColor, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(if(currentLanguage=="vi") "Để Aura Flow phân tích thói quen thực tế,\nvui lòng cấp quyền truy cập dữ liệu sử dụng." else "To analyze real habits,\nplease grant usage data access.", fontSize = 14.sp, color = textColor.copy(alpha = 0.7f), textAlign = TextAlign.Center)

                        Spacer(modifier = Modifier.height(60.dp))

                        if (isScanning) {
                            CircularProgressIndicator(color = buttonColor, modifier = Modifier.size(60.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(if(currentLanguage=="vi") "Đang thu nhập dữ liệu người dùng..." else "Collecting user data...", color = textColor, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Star, contentDescription = null, tint = buttonColor, modifier = Modifier.size(80.dp))
                        }
                    }

                    3 -> {
                        Text(if(currentLanguage=="vi") "Thời gian sử dụng màn hình\nhôm nay của bạn" else "Your screen time\ntoday", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(24.dp))

                        Box(
                            modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).padding(24.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(formatTimeMs(totalTimeMs), fontSize = 36.sp, fontWeight = FontWeight.Bold, color = textColor)
                                Text(if(currentLanguage=="vi") "Cuộc đời đang trôi qua kẽ tay..." else "Life is slipping through your fingers...", fontSize = 12.sp, color = Color(0xFFE53935))
                                Spacer(modifier = Modifier.height(24.dp))

                                Divider(color = Color.LightGray.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(16.dp))

                                Text(if(currentLanguage=="vi") "Kẻ thù gây xao nhãng nhất" else "Most distracting enemies", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.7f), modifier = Modifier.align(Alignment.Start))
                                Spacer(modifier = Modifier.height(16.dp))

                                if (topApps.isEmpty()) {
                                    Text(if(currentLanguage=="vi") "Hôm nay bạn chưa xài app nào, ngoan quá!" else "No apps used today, good job!", color = textColor)
                                } else {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                        topApps.forEach { app ->
                                            AppUsageItemReal(icon = app.iconBitmap, name = app.name, time = formatTimeMsShort(app.timeInMs))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    4 -> {
                        Text("🕰️", fontSize = 100.sp)
                        Spacer(modifier = Modifier.height(32.dp))
                        Text(if(currentLanguage=="vi") "Tôi hứa sẽ tập trung vào\nhiện tại" else "I promise to focus on\nthe present", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = textColor, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(if(currentLanguage=="vi") "Bỏ điện thoại xuống và bắt đầu sống cuộc đời của bạn." else "Put your phone down and start living your life.", fontSize = 14.sp, color = textColor.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                    }
                    5 -> {
                        Text(if(currentLanguage=="vi") "Bắt đầu hành trình mới\nvà lấy lại thời gian của bạn" else "Start a new journey\nand reclaim your time", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = textColor, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(32.dp))
                        Box(modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(24.dp)).border(2.dp, Color.White, RoundedCornerShape(24.dp)).padding(24.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                PremiumFeatureRow(if(currentLanguage=="vi") "Kỹ thuật Pomodoro chuẩn" else "Standard Pomodoro Technique", if(currentLanguage=="vi") "Cải thiện sự tập trung mỗi ngày" else "Improve concentration every day", buttonColor, textColor)
                                PremiumFeatureRow(if(currentLanguage=="vi") "Khu vườn sinh thái" else "Ecological Garden", if(currentLanguage=="vi") "Trồng cây và duy trì tiến độ học tập" else "Plant trees and maintain study progress", buttonColor, textColor)
                                PremiumFeatureRow(if(currentLanguage=="vi") "Đua Top với bạn bè" else "Racing Top with friends", if(currentLanguage=="vi") "Kiếm xu và vươn lên trên bảng xét hạng" else "Earn coins and rise on the rankings", buttonColor, textColor)
                            }
                        }
                    }
                }
            }
        }

        Box(modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp)) {
            Button(
                onClick = {
                    when (currentStep) {
                        1 -> currentStep++
                        2 -> {
                            if (hasUsageStatsPermission(context)) {
                                isScanning = true
                            } else {
                                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                            }
                        }
                        3, 4 -> currentStep++
                        5 -> onFinish()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                shape = RoundedCornerShape(28.dp),
                enabled = !isScanning
            ) {
                Text(
                    text = when (currentStep) {
                        2 -> if (hasUsageStatsPermission(context)) (if(currentLanguage=="vi") "Bắt đầu quét" else "Start scanning") else (if(currentLanguage=="vi") "Cho phép truy cập" else "Grant access")
                        4 -> if(currentLanguage=="vi") "Tôi hứa sẽ tập trung" else "I promise to focus"
                        5 -> if(currentLanguage=="vi") "Bắt đầu miễn phí" else "Start for free"
                        else -> if(currentLanguage=="vi") "Tiếp tục" else "Continue"
                    },
                    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp
                )
            }
        }
    }
}

fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
    return mode == AppOpsManager.MODE_ALLOWED
}

data class AppUsageInfo(val name: String, val timeInMs: Long, val iconBitmap: ImageBitmap?)

fun getRealUsageStats(context: Context): List<AppUsageInfo> {
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val pm = context.packageManager

    val calendar = Calendar.getInstance()
    val endTime = calendar.timeInMillis
    calendar.add(Calendar.DAY_OF_YEAR, -1)
    val startTime = calendar.timeInMillis

    val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
    val usageList = mutableListOf<AppUsageInfo>()

    for (stat in stats) {
        if (stat.totalTimeInForeground > 1000 * 60) {
            try {
                if (stat.packageName.contains("com.android.systemui") || stat.packageName == context.packageName) continue

                val appInfo = pm.getApplicationInfo(stat.packageName, 0)
                val appName = pm.getApplicationLabel(appInfo).toString()

                val drawable = pm.getApplicationIcon(stat.packageName)
                val bitmap = Bitmap.createBitmap(
                    drawable.intrinsicWidth.takeIf { it > 0 } ?: 100,
                    drawable.intrinsicHeight.takeIf { it > 0 } ?: 100,
                    Bitmap.Config.ARGB_8888
                )
                val canvas = android.graphics.Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)

                usageList.add(AppUsageInfo(appName, stat.totalTimeInForeground, bitmap.asImageBitmap()))
            } catch (e: Exception) {}
        }
    }

    return usageList.groupBy { it.name }.map { entry ->
        AppUsageInfo(entry.key, entry.value.sumOf { it.timeInMs }, entry.value.first().iconBitmap)
    }.sortedByDescending { it.timeInMs }
}

@Composable
fun AppUsageItemReal(icon: ImageBitmap?, name: String, time: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(60.dp)) {
        if (icon != null) {
            Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(40.dp))
        } else {
            Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = Color.LightGray) {}
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(name, fontSize = 10.sp, color = Color(0xFF5D4037), maxLines = 1)
        Text(time, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE53935))
    }
}

@Composable
fun PremiumFeatureRow(title: String, desc: String, buttonColor: Color, textColor: Color) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(Icons.Default.CheckCircle, null, tint = buttonColor)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, color = textColor)
            Text(desc, fontSize = 12.sp, color = textColor.copy(alpha = 0.6f))
        }
    }
}

fun formatTimeMs(ms: Long): String {
    val hours = (ms / (1000 * 60 * 60))
    val minutes = (ms % (1000 * 60 * 60)) / (1000 * 60)
    return "${hours}H ${minutes}M"
}

fun formatTimeMsShort(ms: Long): String {
    val hours = (ms / (1000 * 60 * 60))
    val minutes = (ms % (1000 * 60 * 60)) / (1000 * 60)
    return if (hours > 0) "${hours}h${minutes}m" else "${minutes}m"
}
