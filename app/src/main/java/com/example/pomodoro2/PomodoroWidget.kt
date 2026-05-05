package com.example.pomodoro2

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences // <-- Phải có cái này cho currentState
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.actionStartService
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider // <-- IMPORT ĐÚNG CHUẨN NÈ
import androidx.glance.currentState
import androidx.glance.layout.*
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle

class PomodoroWidget : GlanceAppWidget() {

    // Cho phép Widget lưu trữ và nhận trạng thái từ Service
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    companion object {
        val PrefTimeLeft = stringPreferencesKey("time_left")
        val PrefIsRunning = booleanPreferencesKey("is_running")
        val PrefProgress = stringPreferencesKey("progress")
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            // Lấy dữ liệu từ state (do Service đẩy qua)
            val prefs = currentState<Preferences>()
            val timeLeft = prefs[PrefTimeLeft] ?: "25:00"
            val isRunning = prefs[PrefIsRunning] ?: false
            val progress = prefs[PrefProgress]?.toFloatOrNull() ?: 0f

            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .appWidgetBackground()
                    .background(Color(0xFF0C0C1F))
                    .cornerRadius(24.dp)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AURA FLOW",
                    style = TextStyle(
                        // Lách lỗi đỏ bằng cách khai báo rõ day/night
                        color = ColorProvider(day = Color(0xFFFB7185), night = Color(0xFFFB7185)),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = GlanceModifier.height(8.dp))

                // Thời gian động nhảy số rẹt rẹt
                Text(
                    text = timeLeft,
                    style = TextStyle(
                        color = ColorProvider(day = Color.White, night = Color.White),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                // Thanh tiến trình Cyberpunk
                Spacer(modifier = GlanceModifier.height(12.dp))
                LinearProgressIndicator(
                    progress = progress,
                    modifier = GlanceModifier.fillMaxWidth().height(4.dp).cornerRadius(2.dp),
                    color = ColorProvider(day = Color(0xFFFB7185), night = Color(0xFFFB7185)),
                    backgroundColor = ColorProvider(day = Color(0xFF2D2D42), night = Color(0xFF2D2D42))
                )

                Spacer(modifier = GlanceModifier.height(16.dp))

                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Nút START/PAUSE linh hoạt
                    ActionButton(
                        text = if (isRunning) "PAUSE" else "START",
                        color = if (isRunning) Color(0xFF4ADE80) else Color(0xFFFB7185),
                        onClick = actionStartService(
                            Intent(context, TimerService::class.java).apply {
                                action = if (isRunning) "PAUSE" else "START"
                            },
                            isForegroundService = true
                        )
                    )

                    Spacer(modifier = GlanceModifier.width(12.dp))

                    ActionButton(
                        text = "STOP",
                        color = Color(0xFF2D2D42),
                        onClick = actionStartService(
                            Intent(context, TimerService::class.java).apply { action = "STOP" },
                            isForegroundService = true
                        )
                    )
                }
            }
        }
    }

    // Hàm tạo nút bấm gọn gàng
    @androidx.compose.runtime.Composable
    private fun ActionButton(text: String, color: Color, onClick: androidx.glance.action.Action) {
        Box(
            modifier = GlanceModifier
                .background(color)
                .cornerRadius(12.dp)
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .clickable(onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = TextStyle(
                    // Lách lỗi đỏ bằng cách khai báo rõ day/night
                    color = ColorProvider(day = Color.White, night = Color.White),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

class PomodoroWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PomodoroWidget()
}