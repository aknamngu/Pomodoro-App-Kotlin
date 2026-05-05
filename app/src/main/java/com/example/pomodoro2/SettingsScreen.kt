package com.example.pomodoro2

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.pomodoro2.ui.theme.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeViewModel: ThemeViewModel = viewModel(),
    onLogout: () -> Unit = {},
    onNavigateToTasks: () -> Unit = {},
    onNavigateToMotivation: () -> Unit = {},
    onNavigateToFocus: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToPremium: () -> Unit = {},
    onNavigateToWhitelist: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { AuraDatabase.getDatabase(context) }
    val dao = database.auraDao()
    val dataStore = remember { AuraDataStore(context) }
    
    val user = Firebase.auth.currentUser
    val userEmail = user?.email ?: "Chưa đăng nhập"
    val userName = user?.displayName?.takeIf { it.isNotEmpty() } ?: "Nhà vườn Aura"
    val userPhotoUrl = user?.photoUrl

    val isDark = isSystemInDarkTheme()
    val colorScheme = MaterialTheme.colorScheme
    
    val mauNenChuan = colorScheme.background
    val mauCardChuan = if (isDark) Color(0xFF252538) else Color.White
    val mauChuChuan = colorScheme.onBackground

    val currentDrops by dataStore.auraDropsFlow.collectAsState(initial = 0)
    val currentLanguage by dataStore.languageFlow.collectAsState(initial = "vi")
    val userXP by dataStore.userXPFlow.collectAsState(initial = 0)
    val userAchievements by dataStore.achievementsCountFlow.collectAsState(initial = 0)
    val userCoins by dataStore.userCoinsFlow.collectAsState(initial = 0)
    val alarmSound by dataStore.alarmSoundNameFlow.collectAsState(initial = "Mặc định")

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showRedeemDialog by remember { mutableStateOf(false) }
    var showSoundDialog by remember { mutableStateOf(false) }

    // Xử lý thay đổi ảnh đại diện
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val profileUpdates = userProfileChangeRequest { photoUri = it }
            user?.updateProfile(profileUpdates)?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(context, if(currentLanguage=="vi") "Cập nhật ảnh thành công!" else "Avatar updated!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Tính toán Level và XP: Cấp 1 cần 100, Cấp 2 cần 200, Cấp 3 cần 300...
    val levelInfo = remember(userXP) {
        var lvl = 1
        var xp = userXP
        var nextThreshold = 100
        while (xp >= nextThreshold) {
            xp -= nextThreshold
            lvl++
            nextThreshold += 100
        }
        Triple(lvl, xp, nextThreshold)
    }
    val currentLevel = levelInfo.first
    val currentLevelXP = levelInfo.second
    val xpToNextLevel = levelInfo.third
    val xpProgress = currentLevelXP.toFloat() / xpToNextLevel.toFloat()

    // Kiểm tra và cập nhật thành tựu hàng ngày
    LaunchedEffect(Unit) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastDate = dataStore.lastAchievementDateFlow.first()
        
        if (today != lastDate) {
            val allTasks = dao.getAllTasks().first()
            val mineTasks = allTasks.filter { !it.isSystem }
            if (mineTasks.isNotEmpty() && mineTasks.all { it.progress >= it.maxProgress }) {
                dataStore.saveAchievementsCount(userAchievements + 1)
                dataStore.saveLastAchievementDate(today)
                Toast.makeText(context, if(currentLanguage=="vi") "Chúc mừng! Bạn nhận được 1 thành tựu mới 🏆" else "Congrats! New achievement earned 🏆", Toast.LENGTH_LONG).show()
            }
        }
    }

    val isStrictMode by dataStore.isStrictModeFlow.collectAsState(initial = false)
    var notification by remember { mutableStateOf(true) }
    var soundFocus by remember { mutableStateOf(true) }

    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
    val googleSignInClient = GoogleSignIn.getClient(context, gso)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = mauNenChuan,
        bottomBar = {
            AppBottomNavigation(
                currentRoute = "settings",
                onNavigateToTasks = onNavigateToTasks,
                onNavigateToMotivation = onNavigateToMotivation,
                onNavigateToFocus = onNavigateToFocus,
                onNavigateToHistory = onNavigateToHistory,
                onNavigateToSettings = onNavigateToSettings,
                language = currentLanguage
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (currentLanguage == "vi") "Cài đặt" else "Settings", fontWeight = FontWeight.Bold, color = mauChuChuan, fontSize = 26.sp)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Surface(
                        modifier = Modifier.size(100.dp).clickable { imagePickerLauncher.launch("image/*") }, 
                        shape = CircleShape,
                        color = MauNhanTym.copy(alpha = 0.15f), border = androidx.compose.foundation.BorderStroke(2.dp, MauNhanTym)
                    ) {
                        if (userPhotoUrl != null) {
                            Image(
                                painter = rememberAsyncImagePainter(userPhotoUrl),
                                contentDescription = "Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Person, contentDescription = "Avatar", tint = MauNhanTym, modifier = Modifier.padding(20.dp))
                        }
                        
                        // Icon máy ảnh nhỏ để biết là có thể click
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AddAPhoto, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(24.dp))
                        }
                    }
                    Surface(
                        modifier = Modifier.size(32.dp).border(2.dp, mauNenChuan, CircleShape),
                        shape = CircleShape, color = MauNutCam
                    ) {
                        Box(contentAlignment = Alignment.Center) { Text("$currentLevel", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(userName, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = mauChuChuan)
                Text(userEmail, fontSize = 14.sp, color = mauChuChuan.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(24.dp))

                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (currentLanguage == "vi") "Cấp độ $currentLevel" else "Level $currentLevel", color = mauChuChuan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("${currentLevelXP}/${xpToNextLevel} XP", color = mauChuChuan.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { xpProgress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = MauNhanTym, trackColor = mauChuChuan.copy(alpha = 0.1f),
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(
                        modifier = Modifier.weight(1f).background(mauCardChuan, RoundedCornerShape(20.dp)).border(1.dp, mauChuChuan.copy(alpha = 0.05f), RoundedCornerShape(20.dp)).clickable { showRedeemDialog = true }.padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🏆", fontSize = 24.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("$userAchievements", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = mauChuChuan)
                            Text(if (currentLanguage == "vi") "Thành tựu" else "Achievements", fontSize = 10.sp, color = mauChuChuan.copy(alpha = 0.6f))
                        }
                    }
                    Box(
                        modifier = Modifier.weight(1f).background(mauCardChuan, RoundedCornerShape(20.dp)).border(1.dp, mauChuChuan.copy(alpha = 0.05f), RoundedCornerShape(20.dp)).padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("💧", fontSize = 24.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("$currentDrops", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MauNhanTym)
                            Text(if (currentLanguage == "vi") "Giọt mưa" else "Aura Drops", fontSize = 10.sp, color = mauChuChuan.copy(alpha = 0.6f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            item { SettingCategoryHeader(if (currentLanguage == "vi") "Tài khoản" else "Account") }
            item { SettingOptionButton(if (currentLanguage == "vi") "Quản lý gói Premium" else "Manage Premium", onClick = onNavigateToPremium) }
            item { Spacer(modifier = Modifier.height(16.dp)) }

            item { SettingCategoryHeader(if (currentLanguage == "vi") "Cài đặt tập trung" else "Focus Settings") }
            item { SettingOptionSwitch(if (currentLanguage == "vi") "Chế độ Nghiêm ngặt" else "Strict Mode", isStrictMode) { scope.launch { dataStore.saveStrictMode(it) } } }
            item { SettingOptionButton(if (currentLanguage == "vi") "Quản lý danh sách trắng" else "Manage Whitelist", onClick = onNavigateToWhitelist) }
            item { Spacer(modifier = Modifier.height(16.dp)) }

            item { SettingCategoryHeader(if (currentLanguage == "vi") "Âm thanh & Thông báo" else "Sound & Notifications") }
            item { SettingOptionSwitch(if (currentLanguage == "vi") "Thông báo nhắc nhở học" else "Study Reminder", notification) { notification = it } }
            item { SettingOptionSwitch(if (currentLanguage == "vi") "Âm thanh khi bắt đầu học" else "Focus Start Sound", soundFocus) { soundFocus = it } }
            item { 
                SettingOptionButton(
                    title = if (currentLanguage == "vi") "Chọn âm thanh khi hết giờ ($alarmSound)" else "Timer End Sound ($alarmSound)",
                    onClick = { showSoundDialog = true }
                ) 
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }

            item { SettingCategoryHeader(if (currentLanguage == "vi") "Hệ thống" else "System") }
            item {
                SettingOptionSwitch(
                    title = if (currentLanguage == "vi") "Chế độ tối" else "Dark Mode",
                    isChecked = isDark,
                    onCheckedChange = { themeViewModel.toggleTheme() }
                )
            }
            item { 
                SettingOptionButton(
                    title = if (currentLanguage == "vi") "Đổi ngôn ngữ (${if(currentLanguage=="vi") "Tiếng Việt" else "English"})" else "Change Language (${if(currentLanguage=="vi") "Tiếng Việt" else "English"})",
                    onClick = { showLanguageDialog = true }
                ) 
            }
            item { Spacer(modifier = Modifier.height(32.dp)) }

            item {
                Button(
                    onClick = {
                        Firebase.auth.signOut()
                        googleSignInClient.signOut().addOnCompleteListener { onLogout() }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373)),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (currentLanguage == "vi") "ĐĂNG XUẤT" else "LOGOUT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Version 1.0.0", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 10.sp, color = mauChuChuan.copy(alpha = 0.3f))
            }
        }

        if (showLanguageDialog) {
            AlertDialog(
                onDismissRequest = { showLanguageDialog = false },
                containerColor = mauCardChuan,
                title = { Text(if (currentLanguage == "vi") "Chọn ngôn ngữ" else "Select Language", fontWeight = FontWeight.Bold, color = mauChuChuan) },
                text = {
                    Column {
                        LanguageItem("Tiếng Việt", "vi", currentLanguage) {
                            scope.launch { dataStore.saveLanguage("vi"); showLanguageDialog = false }
                        }
                        LanguageItem("English", "en", currentLanguage) {
                            scope.launch { dataStore.saveLanguage("en"); showLanguageDialog = false }
                        }
                    }
                },
                confirmButton = {}
            )
        }

        if (showRedeemDialog) {
            AlertDialog(
                onDismissRequest = { showRedeemDialog = false },
                containerColor = mauCardChuan,
                title = { Text(if(currentLanguage=="vi") "Đổi thành tựu" else "Redeem Achievements", fontWeight = FontWeight.Bold, color = mauChuChuan) },
                text = { Text(if(currentLanguage=="vi") "Bạn đang có $userAchievements thành tựu. Mỗi thành tựu có thể đổi lấy 50 xu 🪙. Bạn có muốn đổi hết không?" else "You have $userAchievements achievements. Each can be traded for 50 coins 🪙. Trade all?") },
                confirmButton = {
                    Button(
                        onClick = {
                            if (userAchievements > 0) {
                                scope.launch {
                                    val newCoins = userCoins + (userAchievements * 50)
                                    dataStore.saveUserCoins(newCoins)
                                    dataStore.saveAchievementsCount(0)
                                    showRedeemDialog = false
                                    Toast.makeText(context, if(currentLanguage=="vi") "Đã đổi thành công!" else "Redeemed successfully!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MauNhanTym)
                    ) { Text(if(currentLanguage=="vi") "Đổi ngay" else "Redeem", color = Color.White) }
                },
                dismissButton = {
                    TextButton(onClick = { showRedeemDialog = false }) { Text(if(currentLanguage=="vi") "Hủy" else "Cancel", color = mauChuChuan) }
                }
            )
        }

        if (showSoundDialog) {
            val sounds = listOf("Mặc định", "Rừng chim", "Chuông Thiền", "Tích tắc")
            AlertDialog(
                onDismissRequest = { showSoundDialog = false },
                containerColor = mauCardChuan,
                title = { Text(if(currentLanguage=="vi") "Chọn âm thanh báo" else "Select Alarm Sound", fontWeight = FontWeight.Bold, color = mauChuChuan) },
                text = {
                    Column {
                        sounds.forEach { sound ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch {
                                            dataStore.saveAlarmSoundName(sound)
                                            showSoundDialog = false
                                        }
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(sound, color = mauChuChuan)
                                if (alarmSound == sound) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MauNhanTym)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSoundDialog = false }) { Text(if(currentLanguage=="vi") "Đóng" else "Close") }
                }
            )
        }
    }
}

@Composable
fun LanguageItem(name: String, code: String, currentCode: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(name, color = MaterialTheme.colorScheme.onBackground)
        if (code == currentCode) {
            Icon(Icons.Default.Check, contentDescription = null, tint = MauNhanTym)
        }
    }
}

@Composable
fun SettingCategoryHeader(title: String) {
    Text(title, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
}

@Composable
fun SettingOptionSwitch(title: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
        Switch(checked = isChecked, onCheckedChange = onCheckedChange, modifier = Modifier.scale(0.8f), colors = SwitchDefaults.colors(checkedThumbColor = MauNhanTym, checkedTrackColor = MauNhanTym.copy(alpha = 0.5f)))
    }
}

@Composable
fun SettingOptionButton(title: String, onClick: () -> Unit = {}) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.weight(1f))
        Icon(Icons.Default.KeyboardArrowRight, "More", tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
    }
}
