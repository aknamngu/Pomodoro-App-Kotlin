package com.example.pomodoro2

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pomodoro2.ui.theme.MauNhanTym
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    val context = LocalContext.current
    val dataStore = remember { AuraDataStore(context) }
    val currentLanguage by dataStore.languageFlow.collectAsState(initial = "vi")

    LaunchedEffect(Unit) {
        delay(1500) 
        onTimeout()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Aura Flow",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = MauNhanTym 
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if(currentLanguage == "vi") "Hít thở. Tập trung. Khởi sắc." else "Breathe. Focus. Flourish.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(32.dp))
        CircularProgressIndicator(color = MauNhanTym)
    }
}