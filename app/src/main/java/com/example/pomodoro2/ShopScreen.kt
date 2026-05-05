package com.example.pomodoro2

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pomodoro2.ui.theme.MauNhanTym
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ShopItem(val icon: String, val name: String, val nameEn: String, val price: Int, val requiredLevel: Int)

val shopItems = listOf(
    ShopItem("🌸", "Hoa anh đào", "Cherry Blossom", 0, 1),
    ShopItem("🌵", "Xương rồng", "Cactus", 200, 2),
    ShopItem("🌻", "Hướng dương", "Sunflower", 500, 3),
    ShopItem("🌹", "Hoa hồng", "Rose", 300, 4),
    ShopItem("🍄", "Nấm lạ", "Magic Sprout", 400, 5),
    ShopItem("🥥", "Cây dừa", "Coconut Tree", 800, 6),
    ShopItem("🌳", "Cây đại thụ", "Ancient Tree", 1000, 7)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val dataStore = remember { AuraDataStore(context) }
    val scope = rememberCoroutineScope()
    
    val userCoins by dataStore.userCoinsFlow.collectAsState(initial = 0)
    val userXP by dataStore.userXPFlow.collectAsState(initial = 0)
    val unlockedPlants by dataStore.unlockedPlantsFlow.collectAsState(initial = setOf("🌸"))
    val currentLanguage by dataStore.languageFlow.collectAsState(initial = "vi")
    val colorScheme = MaterialTheme.colorScheme

    // LOGIC TÍNH CẤP ĐỘ THEO MỐC: 100, 150, 200...
    val currentLevel = remember(userXP) {
        var level = 1
        var xpNeeded = 100
        var tempXP = userXP
        while (tempXP >= xpNeeded) {
            tempXP -= xpNeeded
            level++
            xpNeeded += 50 // Mỗi cấp tăng thêm 50 XP yêu cầu (100 -> 150 -> 200...)
        }
        level
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if(currentLanguage=="vi") "Cửa hàng hạt giống" else "Seed Shop", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    Surface(color = Color(0xFFFFD54F), shape = RoundedCornerShape(20.dp), modifier = Modifier.padding(end = 16.dp)) {
                        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🪙", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("$userCoins", fontWeight = FontWeight.Bold, color = Color(0xFF5D4037))
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(shopItems) { item ->
                val isUnlocked = unlockedPlants.contains(item.icon)
                val isLevelReached = currentLevel >= item.requiredLevel
                
                Card(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                    elevation = CardDefaults.cardElevation(if (isLevelReached) 4.dp else 1.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(12.dp).alpha(if (isLevelReached) 1f else 0.3f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(item.icon, fontSize = 50.sp)
                            Text(if(currentLanguage=="vi") item.name else item.nameEn, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            if (isUnlocked) {
                                Text(if(currentLanguage=="vi") "Đã sở hữu" else "Owned", color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Button(
                                    onClick = {
                                        if (isLevelReached) {
                                            if (userCoins >= item.price) {
                                                scope.launch {
                                                    dataStore.saveUserCoins(userCoins - item.price)
                                                    dataStore.saveUnlockedPlants(unlockedPlants + item.icon)
                                                    Toast.makeText(context, "Mở khóa thành công!", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                Toast.makeText(context, "Bạn không đủ Xu!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    enabled = isLevelReached,
                                    colors = ButtonDefaults.buttonColors(containerColor = MauNhanTym),
                                    shape = RoundedCornerShape(15.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text("🪙 ${item.price}", fontSize = 12.sp)
                                }
                            }
                        }

                        if (!isLevelReached) {
                            Surface(
                                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
                                color = Color.Black.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Lock, null, modifier = Modifier.size(10.dp), tint = Color.Gray)
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(if(currentLanguage=="vi") "Cấp ${item.requiredLevel}" else "Lv. ${item.requiredLevel}", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
