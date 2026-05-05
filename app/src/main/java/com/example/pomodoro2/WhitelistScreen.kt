package com.example.pomodoro2

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pomodoro2.ui.theme.MauNhanTym
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.launch

data class AppItem(val name: String, val packageName: String, val icon: android.graphics.drawable.Drawable)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhitelistScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val dataStore = remember { AuraDataStore(context) }
    val scope = rememberCoroutineScope()

    var installedApps by remember { mutableStateOf<List<AppItem>>(emptyList()) }
    val whitelistedPackages by dataStore.whitelistedPackagesFlow.collectAsState(initial = setOf(context.packageName))
    val currentLanguage by dataStore.languageFlow.collectAsState(initial = "vi")
    val colorScheme = MaterialTheme.colorScheme

    LaunchedEffect(Unit) {
        val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
            .map {
                AppItem(
                    name = packageManager.getApplicationLabel(it).toString(),
                    packageName = it.packageName,
                    icon = packageManager.getApplicationIcon(it)
                )
            }
            .sortedBy { it.name.lowercase() }
        installedApps = apps
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if(currentLanguage == "vi") "Ứng dụng được phép" else "Allowed Apps", fontWeight = FontWeight.Bold, color = colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = colorScheme.onBackground) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colorScheme.background)
            )
        },
        containerColor = colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (installedApps.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MauNhanTym)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(installedApps) { app ->
                        val isChecked = whitelistedPackages.contains(app.packageName)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = rememberDrawablePainter(drawable = app.icon),
                                contentDescription = null,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = app.name, fontSize = 16.sp, modifier = Modifier.weight(1f), color = colorScheme.onBackground)

                            Switch(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    scope.launch {
                                        val newSet = if (checked) {
                                            whitelistedPackages + app.packageName
                                        } else {
                                            whitelistedPackages - app.packageName
                                        }
                                        dataStore.saveWhitelist(newSet)
                                    }
                                },
                                modifier = Modifier.scale(0.8f),
                                colors = SwitchDefaults.colors(checkedThumbColor = MauNhanTym, checkedTrackColor = MauNhanTym.copy(alpha = 0.5f))
                            )
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = colorScheme.onBackground.copy(alpha = 0.1f)
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    Button(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth(0.8f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MauNhanTym),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        Text(if(currentLanguage == "vi") "Xong" else "Done", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
