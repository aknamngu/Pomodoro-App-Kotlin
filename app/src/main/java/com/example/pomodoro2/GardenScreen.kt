package com.example.pomodoro2

import android.widget.Toast
import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pomodoro2.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random

// ================= HỆ THỐNG TIẾN HÓA =================
data class EvolutionStage(
    val wateringNeeded: Int,
    val icon: String,
    val name: String
)

val standardEvolution = listOf(
    EvolutionStage(0, "🌰", "Hạt giống"),
    EvolutionStage(20, "🌱", "Mầm cây"), 
    EvolutionStage(50, "🌸", "Nở hoa")
)

@Composable
fun GardenScreen(
    onNavigateToTasks: () -> Unit = {},
    onNavigateToMotivation: () -> Unit = {},
    onNavigateToFocus: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToShop: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { AuraDatabase.getDatabase(context) }
    val dao = database.auraDao()
    val dataStore = remember { AuraDataStore(context) }
    
    val colorScheme = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()

    val auraDrops by dataStore.auraDropsFlow.collectAsState(initial = 0)
    val userCoins by dataStore.userCoinsFlow.collectAsState(initial = 0)
    val currentLanguage by dataStore.languageFlow.collectAsState(initial = "vi")
    val unlockedPlants by dataStore.unlockedPlantsFlow.collectAsState(initial = setOf("🌸"))

    val dbPlants by dao.getAllPlants().collectAsState(initial = emptyList())
    val plotsMap = remember(dbPlants) { dbPlants.associateBy { it.plotNumber } }

    var plantToWater by remember { mutableStateOf<PlantEntity?>(null) }
    var showSeedSelector by remember { mutableStateOf<Int?>(null) }

    // Weather States
    var weatherCity by remember { mutableStateOf("Hanoi") }
    var weatherIcon by remember { mutableStateOf("☀️") }
    var isRaining by remember { mutableStateOf(false) }
    
    // Animation States
    var activeWateringPlot by remember { mutableStateOf<Int?>(null) }

    // Fetch Weather
    LaunchedEffect(weatherCity) {
        try {
            // Sử dụng API Key từ BuildConfig thay vì hardcode
            val apiKey = BuildConfig.WEATHER_API_KEY
            if (apiKey.isBlank()) {
                Log.w("WeatherGarden", "Weather API Key is missing in local.properties")
                return@LaunchedEffect
            }

            val response = WeatherApi.retrofitService.getWeather(weatherCity, apiKey)
            var main = response.weather.firstOrNull()?.main ?: ""
            
            if (weatherCity == "Ho Chi Minh City") main = "Clear"
            else if (weatherCity == "Hanoi") main = "Rain"

            isRaining = main.lowercase().contains("rain")
            weatherIcon = when {
                main.lowercase().contains("rain") -> "🌧️"
                main.lowercase().contains("cloud") -> "☁️"
                else -> "☀️"
            }
            if (isRaining) {
                dataStore.saveAuraDrops(auraDrops + 20)
                Toast.makeText(context, if(currentLanguage=="vi") "Trời đang mưa tại $weatherCity! +20 💧" else "It's raining in $weatherCity! +20 💧", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("WeatherGarden", "Error: ${e.message}")
        }
    }

    val plotPositions = listOf(
        Pair((-85).dp, (-5).dp), 
        Pair(85.dp, (-5).dp),
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colorScheme.background,
        bottomBar = { 
            AppBottomNavigation(
                currentRoute = "motivation",
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
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // HEADER - Đã chỉnh sửa để tránh chồng lấn và bỏ London
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (currentLanguage == "vi") "Khu vườn của bạn" else "Your Garden",
                        fontWeight = FontWeight.ExtraBold, 
                        color = colorScheme.onBackground, 
                        fontSize = 22.sp
                    )
                    
                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        modifier = Modifier.clickable { 
                            // Chỉ chuyển đổi giữa Hanoi và HCM
                            weatherCity = if (weatherCity == "Hanoi") "Ho Chi Minh City" else "Hanoi"
                        },
                        color = colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(14.dp), tint = MauNhanTym)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(weatherCity, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(" | $weatherIcon", fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
                            Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CurrencyChip(icon = "🪙", value = userCoins.toString(), color = Color(0xFFFFD54F))
                        CurrencyChip(icon = "💧", value = auraDrops.toString(), color = MauNhanTym, textColor = Color.White)
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Surface(
                        modifier = Modifier.clickable { onNavigateToShop() },
                        color = colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = 2.dp
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🛍️", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if(currentLanguage=="vi") "Cửa hàng" else "Shop", 
                                fontSize = 12.sp, 
                                fontWeight = FontWeight.ExtraBold, 
                                color = colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center 
            ) {
                // Hiệu ứng nắng - Đẩy xuống thấp hơn để không đè Header
                if (!isRaining && weatherIcon == "☀️") {
                    SunBeamEffect(modifier = Modifier.align(Alignment.TopCenter).offset(y = 10.dp))
                }

                // Hòn đảo
                Image(
                    painter = painterResource(id = R.drawable.bg_island),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(400.dp),
                    contentScale = ContentScale.Fit, 
                    colorFilter = if (isDark) ColorFilter.tint(Color.Black.copy(alpha = 0.25f), androidx.compose.ui.graphics.BlendMode.SrcAtop) else null
                )

                // HIỆU ỨNG MƯA TOÀN CẢNH
                if (isRaining) {
                    RainEffect(modifier = Modifier.matchParentSize())
                }

                for (i in 0 until 2) {
                    val plotNumber = i + 1
                    val plantOnPlot = plotsMap[plotNumber]
                    val (offsetX, offsetY) = plotPositions[i]

                    Box(contentAlignment = Alignment.Center) {
                        RealPlantSpot(
                            plant = plantOnPlot,
                            offsetX = offsetX,
                            offsetY = offsetY,
                            onClick = {
                                if (plantOnPlot == null) {
                                    showSeedSelector = plotNumber
                                } else {
                                    val isFullyGrown = plantOnPlot.timesWatered >= standardEvolution.last().wateringNeeded
                                    if (!isFullyGrown) {
                                        plantToWater = plantOnPlot
                                    } else {
                                        scope.launch {
                                            dataStore.saveUserCoins(userCoins + 100)
                                            dao.deletePlant(plantOnPlot)
                                            Toast.makeText(context, if (currentLanguage == "vi") "Thu hoạch hoa thành công! +100 🪙" else "Harvested! +100 🪙", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            }
                        )
                        
                        // HIỆU ỨNG TƯỚI NƯỚC TẠI CHỖ
                        if (activeWateringPlot == plotNumber) {
                            Box(modifier = Modifier.offset(x = offsetX, y = offsetY - 60.dp)) {
                                RainEffect(modifier = Modifier.size(80.dp, 100.dp), dropCount = 15)
                            }
                        }
                    }
                }
            }
            
            Surface(
                color = Color(0xFFFFEBF2),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Text(
                    if (currentLanguage == "vi") "Nhấn dấu ➕ để gieo hạt 🌰\nTưới nước để lớn thành mầm 🌱 và nở hoa 🌸\nThu hoạch để nhận Xu 🪙" 
                    else "Tap ➕ to sow 🌰\nWater to grow into 🌱 and bloom 🌸\nHarvest to get 🪙", 
                    color = Color(0xFFD81B60), textAlign = TextAlign.Center, fontSize = 13.sp, modifier = Modifier.padding(14.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (showSeedSelector != null) {
            AlertDialog(
                onDismissRequest = { showSeedSelector = null },
                containerColor = Color.White,
                title = { Text(if(currentLanguage=="vi") "Chọn hạt giống" else "Select Seed", fontWeight = FontWeight.Bold, color = MauNhanTym) },
                text = {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        unlockedPlants.forEach { plantIcon ->
                            Surface(
                                modifier = Modifier.size(65.dp).clickable { 
                                    val currentPlot = showSeedSelector
                                    if (currentPlot != null) {
                                        scope.launch {
                                            dao.insertPlant(PlantEntity(plotNumber = currentPlot, plantType = plantIcon))
                                            showSeedSelector = null
                                        }
                                    }
                                },
                                color = MauNhanTym.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(15.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) { Text(plantIcon, fontSize = 35.sp) }
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showSeedSelector = null }) { Text(if(currentLanguage=="vi") "Đóng" else "Close") } }
            )
        }

        if (plantToWater != null) {
            val currentPlant = plantToWater!!
            AlertDialog(
                onDismissRequest = { plantToWater = null },
                containerColor = Color.White,
                modifier = Modifier.padding(24.dp).border(2.dp, MauNhanTym.copy(alpha = 0.3f), RoundedCornerShape(28.dp)),
                title = { Text(if (currentLanguage == "vi") "Chăm sóc cây" else "Plant Care", color = MauNhanTym, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(text = "🚿", fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(if (currentLanguage == "vi") "Dùng 10 giọt mưa để tưới cây nhé!" else "Use 10 drops to water your plant!", textAlign = TextAlign.Center, color = Color.DarkGray)
                    }
                },
                confirmButton = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Button(
                                onClick = {
                                    val currentDrops = auraDrops
                                    if (currentDrops >= 10) {
                                        scope.launch {
                                            dataStore.saveAuraDrops(currentDrops - 10)
                                            dao.updatePlant(currentPlant.copy(timesWatered = currentPlant.timesWatered + 10))
                                            
                                            // Kích hoạt hiệu ứng tưới
                                            activeWateringPlot = currentPlant.plotNumber
                                            plantToWater = null
                                            delay(2000)
                                            activeWateringPlot = null
                                        }
                                    } else {
                                        Toast.makeText(context, if (currentLanguage == "vi") "Bạn không đủ giọt mưa!" else "Not enough drops!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                shape = RoundedCornerShape(30.dp),
                                modifier = Modifier.fillMaxWidth(0.8f).height(50.dp)
                            ) { Text(if (currentLanguage == "vi") "Tưới 10 💧" else "Water 10 💧", color = Color.White, fontWeight = FontWeight.Bold) }
                            TextButton(onClick = { plantToWater = null }) { Text(if (currentLanguage == "vi") "Đóng" else "Close", color = Color.Gray) }
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun CurrencyChip(icon: String, value: String, color: Color, textColor: Color = Color(0xFF5D4037)) {
    Surface(color = color, shape = RoundedCornerShape(16.dp)) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = icon, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = value, color = textColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

// ================= COMPOSABLES CHO HIỆU ỨNG =================

@Composable
fun RainEffect(modifier: Modifier = Modifier, dropCount: Int = 40) {
    val infiniteTransition = rememberInfiniteTransition(label = "rain")
    
    BoxWithConstraints(modifier = modifier.clip(RoundedCornerShape(20.dp))) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        
        repeat(dropCount) { index ->
            val startX = remember { Random.nextFloat() * width }
            val duration = remember { Random.nextInt(700, 1300) }
            val delay = remember { Random.nextInt(0, 1000) }
            
            val yOffset by infiniteTransition.animateFloat(
                initialValue = -50f,
                targetValue = height + 50f,
                animationSpec = infiniteRepeatable(
                    animation = tween(duration, delayMillis = delay, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "drop_$index"
            )
            
            Box(
                modifier = Modifier
                    .offset(x = (startX / 2.75).dp, y = (yOffset / 2.75).dp)
                    .size(2.dp, 10.dp)
                    .background(Color(0xFF4FC3F7).copy(alpha = 0.6f), CircleShape)
            )
        }
    }
}

@Composable
fun SunBeamEffect(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "sun")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sun_alpha"
    )
    
    Text("☀️", fontSize = 50.sp, modifier = modifier.background(Color.Yellow.copy(alpha = alpha * 0.2f), CircleShape).padding(10.dp))
}

@Composable
fun RealPlantSpot(plant: PlantEntity?, offsetX: Dp, offsetY: Dp, onClick: () -> Unit) {
    Box(
        modifier = Modifier.offset(x = offsetX, y = offsetY).size(120.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_plant_plot),
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            contentScale = ContentScale.Fit
        )

        if (plant != null) {
            val currentStage = standardEvolution.lastOrNull { plant.timesWatered >= it.wateringNeeded } ?: standardEvolution.first()
            val isFinalStage = currentStage.icon == "🌸"
            val displayIcon = if (isFinalStage) plant.plantType else currentStage.icon
            
            Box(modifier = Modifier.fillMaxSize().padding(bottom = 20.dp), contentAlignment = Alignment.Center) {
                Crossfade(targetState = displayIcon, animationSpec = tween(500), label = "") { icon ->
                    Text(text = icon, fontSize = if(icon=="🌰") 40.sp else if(icon=="🌱") 60.sp else 80.sp)
                }
            }
        } else {
            Text("➕", color = Color.White.copy(alpha = 0.8f), fontSize = 35.sp, modifier = Modifier.padding(bottom = 15.dp))
        }
    }
}
