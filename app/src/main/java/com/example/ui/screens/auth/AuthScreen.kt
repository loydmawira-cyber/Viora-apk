package com.example.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VioraRepository
import kotlinx.coroutines.launch
import com.example.ui.components.LiveBadge
import com.example.ui.theme.*

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    isPasswordRecovery: Boolean = false,
    initialError: String? = null
) {
    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var ageConfirmed by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var errorText by remember(initialError) { mutableStateOf(initialError) }
    var infoText by remember { mutableStateOf<String?>(null) }
    val authScope = rememberCoroutineScope()
    val usernameOk = username.matches(Regex("^[A-Za-z0-9_]{3,20}$"))

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(VioraDeepViolet, VioraDarkBg, VioraDarkBg)
                )
            )
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live-stage brand banner with soft spotlights and a subtle equalizer field.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF21103B), Color(0xFF170D2A), Color(0xFF130B22))
                        )
                    )
                    .border(1.dp, Color(0x557B37E8), RoundedCornerShape(26.dp))
            ) {
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .offset(x = (-90).dp, y = (-110).dp)
                        .background(
                            Brush.radialGradient(listOf(Color(0x55FF3E9D), Color.Transparent)),
                            CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 95.dp, y = (-125).dp)
                        .background(
                            Brush.radialGradient(listOf(Color(0x664B2CFF), Color.Transparent)),
                            CircleShape
                        )
                )
                Canvas(modifier = Modifier.matchParentSize()) {
                    val count = 44
                    val gap = size.width * 0.006f
                    val barWidth = (size.width - gap * (count - 1)) / count
                    val horizon = size.height * 0.72f
                    for (i in 0 until count) {
                        val centerDistance = kotlin.math.abs(i - (count - 1) / 2f) / (count / 2f)
                        val rhythm = listOf(0.22f, 0.38f, 0.28f, 0.56f, 0.34f, 0.46f, 0.24f, 0.62f)[i % 8]
                        val barHeight = size.height * rhythm * (1f - centerDistance * 0.52f)
                        drawRoundRect(
                            color = if (i % 3 == 0) Color(0x40FF4BBD) else Color(0x354B55FF),
                            topLeft = Offset(i * (barWidth + gap), horizon - barHeight),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .border(1.dp, Color(0xAA9F5CFF), RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = com.example.R.drawable.viora_icon),
                            contentDescription = "Viora app icon",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Text(
                        text = "Viora",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Go live. Find your people.",
                        color = VioraCoral,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

                    if (!isPasswordRecovery) Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xC6100C1B))
                            .border(1.dp, Color(0x557D45C9), RoundedCornerShape(18.dp))
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (!isSignUp) Brush.horizontalGradient(listOf(Color(0xFF872AFF), Color(0xFFCA27D9)))
                                    else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                                )
                                .clickable {
                                    isSignUp = false
                                    errorText = null
                                    infoText = null
                                }
                                .padding(vertical = 13.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Sign In",
                                color = if (!isSignUp) Color.White else VioraMutedText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSignUp) Brush.horizontalGradient(listOf(Color(0xFF872AFF), Color(0xFFCA27D9)))
                                    else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                                )
                                .clickable {
                                    isSignUp = true
                                    errorText = null
                                    infoText = null
                                }
                                .padding(vertical = 13.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Create Account",
                                color = if (isSignUp) Color.White else VioraMutedText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Input Fields Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = VioraCardDark),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isPasswordRecovery) {
                        Text(
                            text = "Choose a new password",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (isSignUp) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Username") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = VioraElectricPurple) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("username_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VioraElectricPurple,
                                unfocusedBorderColor = VioraBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }

                    if (!isPasswordRecovery) OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email address") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = VioraElectricPurple) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("email_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VioraElectricPurple,
                            unfocusedBorderColor = VioraBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(if (isPasswordRecovery) "New password" else "Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = VioraElectricPurple) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VioraElectricPurple,
                            unfocusedBorderColor = VioraBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    if (isSignUp || isPasswordRecovery) {
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text(if (isPasswordRecovery) "Confirm new password" else "Confirm password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = VioraElectricPurple) },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("confirm_password_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VioraElectricPurple,
                                unfocusedBorderColor = VioraBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    } else if (!isPasswordRecovery) {
                        Text(
                            text = "Forgot password?",
                            color = VioraCoral,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .align(Alignment.End)
                                .clickable {
                                    errorText = null
                                    infoText = null
                                    if (email.isBlank()) {
                                        errorText = "Enter your email address first."
                                    } else {
                                        loading = true
                                        authScope.launch {
                                            VioraRepository.requestPasswordReset(email.trim())
                                                .onSuccess {
                                                    infoText = "If an account exists for that email, password reset instructions have been sent."
                                                }
                                                .onFailure { errorText = it.message ?: "Could not send the reset email. Try again." }
                                            loading = false
                                        }
                                    }
                                }
                        )
                    }

                    if (isSignUp) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = ageConfirmed,
                                onCheckedChange = { ageConfirmed = it },
                                colors = CheckboxDefaults.colors(checkedColor = VioraCoral)
                            )
                            Text(
                                text = "I confirm I am at least 18 years of age & agree to Viora Community Guidelines",
                                color = VioraMutedText,
                                fontSize = 11.sp
                            )
                        }
                    }

                    if (isSignUp && username.isNotBlank() && !usernameOk) {
                        Text("Username: 3 to 20 letters, numbers or underscore.", color = VioraMutedText, fontSize = 11.sp)
                    }
                    if ((isSignUp || isPasswordRecovery) && password.isNotEmpty() && password.length < 8) {
                        Text("Password needs at least 8 characters.", color = VioraMutedText, fontSize = 11.sp)
                    }
                    if ((isSignUp || isPasswordRecovery) && confirmPassword.isNotEmpty() && password != confirmPassword) {
                        Text("Passwords do not match.", color = VioraErrorRed, fontSize = 11.sp)
                    }
                    errorText?.let { Text(it, color = VioraErrorRed, fontSize = 12.sp) }
                    infoText?.let { Text(it, color = VioraSuccessGreen, fontSize = 12.sp) }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            errorText = null
                            infoText = null
                            loading = true
                            authScope.launch {
                                if (isPasswordRecovery) {
                                    VioraRepository.updatePassword(password)
                                        .onSuccess { onAuthSuccess() }
                                        .onFailure { errorText = it.message ?: "Could not update the password. Request a new reset link and try again." }
                                } else if (isSignUp) {
                                    VioraRepository.signUp(username.trim(), email.trim(), password)
                                        .onSuccess { signedIn ->
                                            if (signedIn) {
                                                onAuthSuccess()
                                            } else {
                                                infoText = "Account created. Open the confirmation email, tap the link, then sign in."
                                                isSignUp = false
                                            }
                                        }
                                        .onFailure { errorText = it.message ?: "Something went wrong. Try again." }
                                } else {
                                    VioraRepository.signIn(email.trim(), password)
                                        .onSuccess { onAuthSuccess() }
                                        .onFailure { errorText = it.message ?: "Something went wrong. Try again." }
                                }
                                loading = false
                            }
                        },
                        enabled = !loading && if (isPasswordRecovery) {
                            password.length >= 8 && password == confirmPassword
                        } else {
                            email.isNotBlank() && if (isSignUp) {
                                password.length >= 8 && password == confirmPassword && ageConfirmed && usernameOk
                            } else {
                                password.isNotBlank()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("auth_submit_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = VioraElectricPurple),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = if (loading) "Please wait..." else when {
                                isPasswordRecovery -> "Update Password"
                                isSignUp -> "Create Viora Account"
                                else -> "Sign In to Viora"
                            },
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Legal & Terms
            Text(
                text = "Protected by Supabase Auth with Row-Level Security • End-to-end moderated",
                color = VioraSubtleText,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
