package com.example.pomodoro2

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pomodoro2.ui.theme.MauNhanTym
import com.example.pomodoro2.ui.theme.MauNutCam

@Composable
fun PremiumScreen(
    currentLanguage: String,
    onBack: () -> Unit,
    onUpgradeSuccess: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var selectedPlanIndex by remember { mutableIntStateOf(1) } // Mặc định chọn gói trọn đời
    var showPaymentDialog by remember { mutableStateOf(false) }
    
    val plans = listOf(
        PremiumPlan(
            title = if(currentLanguage == "vi") "Hàng tháng" else "Monthly",
            price = if(currentLanguage == "vi") "49.000đ" else "$1.99",
            priceDetail = if(currentLanguage == "vi") "/ tháng" else "/ month"
        ),
        PremiumPlan(
            title = if(currentLanguage == "vi") "Trọn đời" else "Lifetime",
            price = if(currentLanguage == "vi") "199.000đ" else "$9.99",
            priceDetail = if(currentLanguage == "vi") "(Ưu đãi nhất)" else "(Best Value)"
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(40.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(colorScheme.onBackground.copy(alpha = 0.1f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = colorScheme.onBackground)
                    }
                }
                
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = MauNutCam.copy(alpha = 0.2f)
                ) {
                    Icon(
                        Icons.Default.Star, 
                        contentDescription = null, 
                        tint = MauNutCam, 
                        modifier = Modifier.padding(16.dp).size(48.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = if(currentLanguage == "vi") "Nâng cấp Premium" else "Upgrade to Premium",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colorScheme.onBackground
                )
                
                Text(
                    text = if(currentLanguage == "vi") "Mở khóa toàn bộ tiềm năng của Aura" else "Unlock Aura's full potential",
                    fontSize = 16.sp,
                    color = colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 8.dp)
                )
                
                Spacer(modifier = Modifier.height(40.dp))
            }
            
            item {
                PremiumFeatureItem(
                    title = if(currentLanguage == "vi") "Không quảng cáo" else "No Ads",
                    desc = if(currentLanguage == "vi") "Tập trung hoàn toàn, không gián đoạn" else "Full focus, no interruptions"
                )
                PremiumFeatureItem(
                    title = if(currentLanguage == "vi") "Cây độc quyền" else "Exclusive Plants",
                    desc = if(currentLanguage == "vi") "Mở khóa những loài cây hiếm nhất" else "Unlock rare and unique plant species"
                )
                PremiumFeatureItem(
                    title = if(currentLanguage == "vi") "Sao lưu đám mây" else "Cloud Backup",
                    desc = if(currentLanguage == "vi") "Không bao giờ lo mất dữ liệu vườn" else "Never lose your garden data"
                )
                
                Spacer(modifier = Modifier.height(40.dp))
            }
            
            items(plans.size) { index ->
                val plan = plans[index]
                PremiumPlanCard(
                    title = plan.title,
                    price = plan.price,
                    priceDetail = plan.priceDetail,
                    isSelected = selectedPlanIndex == index,
                    onClick = { selectedPlanIndex = index }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { showPaymentDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MauNhanTym),
                    shape = RoundedCornerShape(30.dp)
                ) {
                    Text(
                        if(currentLanguage == "vi") "NÂNG CẤP NGAY" else "UPGRADE NOW",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }
                
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.padding(vertical = 16.dp)
                ) {
                    Text(
                        if(currentLanguage == "vi") "Để sau" else "Maybe later",
                        color = colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (showPaymentDialog) {
            val selectedPlan = plans[selectedPlanIndex]
            AlertDialog(
                onDismissRequest = { showPaymentDialog = false },
                containerColor = colorScheme.surface,
                title = { 
                    Text(
                        if(currentLanguage == "vi") "Thanh toán Chuyển khoản" else "Bank Transfer",
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) 
                },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            if(currentLanguage == "vi") "Vui lòng chuyển khoản chính xác số tiền bên dưới để nâng cấp gói ${selectedPlan.title}" 
                            else "Please transfer the exact amount below to upgrade to ${selectedPlan.title} plan",
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Surface(
                            color = MauNhanTym.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(if(currentLanguage=="vi") "Số tiền" else "Amount", fontSize = 12.sp)
                                Text(selectedPlan.price, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MauNhanTym)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Column(modifier = Modifier.fillMaxWidth()) {
                            BankInfoRow(if(currentLanguage=="vi") "Ngân hàng:" else "Bank:", "MB BANK") // Thay đổi thông tin của bạn ở đây
                            BankInfoRow(if(currentLanguage=="vi") "Số tài khoản:" else "Account No:", "0123456789") // Thay số tài khoản của bạn
                            BankInfoRow(if(currentLanguage=="vi") "Chủ tài khoản:" else "Account Holder:", "TRẦN LÊ UYÊN PHƯƠNG") // Tên của bạn
                            BankInfoRow(if(currentLanguage=="vi") "Nội dung:" else "Note:", "AURA PREMIUM " + selectedPlan.title.uppercase())
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            if(currentLanguage=="vi") "Sau khi chuyển khoản, hãy nhấn 'Xác nhận' để hệ thống kiểm tra." 
                            else "After transferring, click 'Confirm' for us to check.",
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            color = colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { 
                            showPaymentDialog = false
                            onUpgradeSuccess() 
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MauNhanTym)
                    ) {
                        Text(if(currentLanguage == "vi") "Xác nhận đã chuyển" else "I have Transferred", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPaymentDialog = false }) {
                        Text(if(currentLanguage == "vi") "Hủy" else "Cancel")
                    }
                }
            )
        }
    }
}

data class PremiumPlan(val title: String, val price: String, val priceDetail: String)

@Composable
fun BankInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
fun PremiumFeatureItem(title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(24.dp),
            shape = CircleShape,
            color = Color(0xFF4CAF50).copy(alpha = 0.2f)
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.padding(4.dp))
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
            Text(desc, fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        }
    }
}

@Composable
fun PremiumPlanCard(title: String, price: String, priceDetail: String, isSelected: Boolean, onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) MauNhanTym.copy(alpha = 0.1f) else colorScheme.surface)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MauNhanTym else colorScheme.onBackground.copy(alpha = 0.1f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (isSelected) MauNhanTym else colorScheme.onBackground)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(price, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = colorScheme.onBackground)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(priceDetail, fontSize = 14.sp, color = colorScheme.onBackground.copy(alpha = 0.6f), modifier = Modifier.padding(bottom = 2.dp))
                }
            }
            
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = MauNhanTym)
            )
        }
    }
}
