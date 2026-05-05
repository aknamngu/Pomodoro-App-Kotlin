package com.example.pomodoro2

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pomodoro2.ui.theme.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, onGoToRegister: () -> Unit) {
    val context = LocalContext.current
    val dataStore = remember { AuraDataStore(context) }
    val currentLanguage by dataStore.languageFlow.collectAsState(initial = "vi")
    
    val auth = Firebase.auth
    val colorScheme = MaterialTheme.colorScheme

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Cấu hình Google Sign In
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        // Đã cập nhật Web Client ID mới của bạn
        .requestIdToken("142763604038-jna2bbphqakjcu9kb0g75utb6iqepoej.apps.googleusercontent.com")
        .requestEmail()
        .build()
    val googleSignInClient = GoogleSignIn.getClient(context, gso)

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                auth.signInWithCredential(credential)
                    .addOnCompleteListener { firebaseTask ->
                        if (firebaseTask.isSuccessful) {
                            Toast.makeText(context, if(currentLanguage == "vi") "Chào mừng ${account.displayName}!" else "Welcome ${account.displayName}!", Toast.LENGTH_SHORT).show()
                            onLoginSuccess()
                        } else {
                            Toast.makeText(context, "Lỗi Firebase: ${firebaseTask.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            }
        } catch (e: Exception) {
            val statusCode = (e as? ApiException)?.statusCode
            Toast.makeText(context, "Lỗi Google (Code: $statusCode): ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

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
                Icon(Icons.Default.Star, contentDescription = null, tint = MauNhanTym, modifier = Modifier.size(40.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Aura Flow",
            color = colorScheme.onBackground,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(40.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(if(currentLanguage == "vi") "Địa chỉ email" else "Email address") },
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
                if (email.isNotEmpty() && password.isNotEmpty()) {
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                onLoginSuccess()
                            } else {
                                Toast.makeText(context, "Error: ${task.exception?.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MauNutCam),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text(if(currentLanguage == "vi") "ĐĂNG NHẬP" else "LOGIN", color = Color.White, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { googleSignInLauncher.launch(googleSignInClient.signInIntent) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MauNhanTym.copy(alpha = 0.5f))
        ) {
            Text(if(currentLanguage == "vi") "TIẾP TỤC VỚI GOOGLE" else "CONTINUE WITH GOOGLE", color = MauNhanTym, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(onClick = onGoToRegister) {
            Row {
                Text(if(currentLanguage == "vi") "Chưa có tài khoản? " else "Don't have an account? ", color = colorScheme.onBackground.copy(alpha = 0.6f))
                Text(if(currentLanguage == "vi") "Đăng ký ngay" else "Sign up now", color = MauNhanTym, fontWeight = FontWeight.Bold)
            }
        }
    }
}
