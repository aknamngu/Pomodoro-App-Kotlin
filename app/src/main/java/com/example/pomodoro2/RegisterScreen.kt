package com.example.pomodoro2

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pomodoro2.ui.theme.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(onRegisterSuccess: () -> Unit, onGoToLogin: () -> Unit) {
    val context = LocalContext.current
    val dataStore = remember { AuraDataStore(context) }
    val currentLanguage by dataStore.languageFlow.collectAsState(initial = "vi")
    
    val auth = Firebase.auth
    val colorScheme = MaterialTheme.colorScheme

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = RoundedCornerShape(20.dp),
            color = MauNhanTym.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, contentDescription = null, tint = MauNhanTym, modifier = Modifier.size(40.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if(currentLanguage == "vi") "Tạo tài khoản" else "Create Account",
            color = colorScheme.onBackground,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if(currentLanguage == "vi") "Bắt đầu hành trình cùng Aura Flow" else "Start your journey with Aura Flow",
            color = colorScheme.onBackground.copy(alpha = 0.6f),
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(if(currentLanguage == "vi") "Họ và tên" else "Full Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            textStyle = TextStyle(color = colorScheme.onBackground),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MauNhanTym,
                unfocusedBorderColor = colorScheme.onBackground.copy(alpha = 0.2f),
                cursorColor = MauNhanTym,
                focusedLabelColor = MauNhanTym,
                unfocusedLabelColor = colorScheme.onBackground.copy(alpha = 0.6f),
                focusedTextColor = colorScheme.onBackground,
                unfocusedTextColor = colorScheme.onBackground
            ),
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MauNhanTym) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(if(currentLanguage == "vi") "Email" else "Email") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            textStyle = TextStyle(color = colorScheme.onBackground),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MauNhanTym,
                unfocusedBorderColor = colorScheme.onBackground.copy(alpha = 0.2f),
                cursorColor = MauNhanTym,
                focusedLabelColor = MauNhanTym,
                unfocusedLabelColor = colorScheme.onBackground.copy(alpha = 0.6f),
                focusedTextColor = colorScheme.onBackground,
                unfocusedTextColor = colorScheme.onBackground
            ),
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = MauNhanTym) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(if(currentLanguage == "vi") "Mật khẩu" else "Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            textStyle = TextStyle(color = colorScheme.onBackground),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MauNhanTym,
                unfocusedBorderColor = colorScheme.onBackground.copy(alpha = 0.2f),
                cursorColor = MauNhanTym,
                focusedLabelColor = MauNhanTym,
                unfocusedLabelColor = colorScheme.onBackground.copy(alpha = 0.6f),
                focusedTextColor = colorScheme.onBackground,
                unfocusedTextColor = colorScheme.onBackground
            ),
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MauNhanTym) }
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (email.isNotEmpty() && password.isNotEmpty() && name.isNotEmpty()) {
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                auth.currentUser?.sendEmailVerification()
                                    ?.addOnCompleteListener { verifyTask ->
                                        if (verifyTask.isSuccessful) {
                                            Toast.makeText(context, if(currentLanguage == "vi") "Đăng ký thành công! Nhớ check mail nhé!" else "Register success! Check your email!", Toast.LENGTH_LONG).show()
                                            onRegisterSuccess()
                                        }
                                    }
                            } else {
                                Toast.makeText(context, "Error: ${task.exception?.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                } else {
                    Toast.makeText(context, if(currentLanguage == "vi") "Vui lòng điền hết thông tin!" else "Please fill in all info!", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MauNhanTym),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text(if(currentLanguage == "vi") "ĐĂNG KÝ" else "REGISTER", color = Color.White, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(onClick = onGoToLogin) {
            Row {
                Text(if(currentLanguage == "vi") "Đã có tài khoản? " else "Already have an account? ", color = colorScheme.onBackground.copy(alpha = 0.6f))
                Text(if(currentLanguage == "vi") "Đăng nhập ngay" else "Login now", color = MauNhanTym, fontWeight = FontWeight.Bold)
            }
        }
    }
}
