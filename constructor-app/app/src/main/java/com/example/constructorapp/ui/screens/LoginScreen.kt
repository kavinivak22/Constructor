package com.example.constructorapp.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.constructorapp.theme.*
import com.example.constructorapp.ui.components.*
import com.example.constructorapp.ui.viewmodel.AuthUiState
import com.example.constructorapp.ui.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToRegisterCompany: (userId: String, email: String, displayName: String) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Mode: 'login' vs 'signup'
    var isSignUpMode by remember { mutableStateOf(false) }

    // Form inputs
    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Forgot password dialog
    var isForgotPasswordOpen by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }

    // Email verification sent banner
    var verificationEmailSentTo by remember { mutableStateOf<String?>(null) }

    // Access Denied State
    var isAccessDenied by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is AuthUiState.NeedsCompany -> {
                onNavigateToRegisterCompany(state.userId, state.email, state.displayName)
            }
            is AuthUiState.AccessDenied -> {
                isAccessDenied = true
            }
            is AuthUiState.EmailVerificationSent -> {
                verificationEmailSentTo = state.email
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBackground)
    ) {
        // Living Ambient Drifting Blobs
        AmbientBlobBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Constructor App Logo
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFFFFA043),
                                BrandPrimary,
                                BrandPrimaryDark
                            )
                        )
                    )
                    .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "C",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Constructor",
                fontFamily = OutfitFontFamily,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isSignUpMode) "Create your account" else "Welcome back",
                fontFamily = OutfitFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isSignUpMode) "Enter your details below to get started" else "Enter your credentials to access your account",
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ─── Main GlassCard Auth Shell ──────────────────────────────────────
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Segmented Control (Login vs Sign Up)
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(if (!isSignUpMode) Color.White else Color.Transparent)
                                    .clickable {
                                        isSignUpMode = false
                                        viewModel.resetState()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Sign In",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontWeight = if (!isSignUpMode) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = if (!isSignUpMode) TextPrimary else TextSecondary
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(if (isSignUpMode) Color.White else Color.Transparent)
                                    .clickable {
                                        isSignUpMode = true
                                        viewModel.resetState()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Create Account",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontWeight = if (isSignUpMode) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = if (isSignUpMode) TextPrimary else TextSecondary
                                )
                            }
                        }
                    }

                    // Email verification banner if sent
                    if (verificationEmailSentTo != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = BrandPrimary.copy(alpha = 0.10f),
                            border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.30f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.MarkEmailRead,
                                    contentDescription = null,
                                    tint = BrandPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = "Verify Your Email",
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "A confirmation link was sent to $verificationEmailSentTo. Please check your inbox and verify your email to log in.",
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    // Full Name (Only on Sign Up)
                    if (isSignUpMode) {
                        FluidGlossTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            label = "Full Name *",
                            placeholder = "John Doe",
                            leadingIcon = {
                                Icon(Icons.Outlined.Person, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Email Field
                    FluidGlossTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email Address *",
                        placeholder = "m@example.com",
                        leadingIcon = {
                            Icon(Icons.Outlined.Email, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Password Field
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Password *",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )

                            if (!isSignUpMode) {
                                Text(
                                    text = "Forgot password?",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BrandPrimary,
                                    modifier = Modifier.clickable {
                                        resetEmail = email
                                        isForgotPasswordOpen = true
                                    }
                                )
                            }
                        }

                        FluidGlossTextField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = if (isSignUpMode) "At least 6 characters" else "Enter password",
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            leadingIcon = {
                                Icon(Icons.Outlined.Lock, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }, modifier = Modifier.size(24.dp)) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (isSignUpMode) {
                                        viewModel.signup(
                                            email = email,
                                            password = password,
                                            displayName = displayName,
                                            onLoginSuccess = onLoginSuccess,
                                            onNeedsCompany = { uId, uEmail ->
                                                onNavigateToRegisterCompany(uId, uEmail, displayName)
                                            },
                                            onEmailSent = { verificationEmailSentTo = it }
                                        )
                                    } else {
                                        viewModel.login(
                                            email = email,
                                            password = password,
                                            onLoginSuccess = onLoginSuccess,
                                            onNeedsCompany = { uId, uEmail ->
                                                onNavigateToRegisterCompany(uId, uEmail, "")
                                            },
                                            onAccessDenied = { isAccessDenied = true }
                                        )
                                    }
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Error Banner
                    if (uiState is AuthUiState.Error) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = RoseDanger.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, RoseDanger.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = RoseDanger, modifier = Modifier.size(18.dp))
                                Text(
                                    text = (uiState as AuthUiState.Error).message,
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 12.sp,
                                    color = RoseDanger,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Primary Action Button
                    FluidGlossButton(
                        text = if (uiState is AuthUiState.Loading) {
                            if (isSignUpMode) "Creating Account..." else "Signing In..."
                        } else {
                            if (isSignUpMode) "Sign Up with Email" else "Login"
                        },
                        onClick = {
                            if (isSignUpMode) {
                                viewModel.signup(
                                    email = email,
                                    password = password,
                                    displayName = displayName,
                                    onLoginSuccess = onLoginSuccess,
                                    onNeedsCompany = { uId, uEmail ->
                                        onNavigateToRegisterCompany(uId, uEmail, displayName)
                                    },
                                    onEmailSent = { verificationEmailSentTo = it }
                                )
                            } else {
                                viewModel.login(
                                    email = email,
                                    password = password,
                                    onLoginSuccess = onLoginSuccess,
                                    onNeedsCompany = { uId, uEmail ->
                                        onNavigateToRegisterCompany(uId, uEmail, "")
                                    },
                                    onAccessDenied = { isAccessDenied = true }
                                )
                            }
                        },
                        loading = uiState is AuthUiState.Loading,
                        enabled = email.isNotBlank() && password.isNotBlank() && (!isSignUpMode || displayName.isNotBlank()),
                        variant = FluidGlossButtonVariant.Primary,
                        size = FluidGlossButtonSize.Large,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Toggle Mode Prompt
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isSignUpMode) "Already have an account? " else "Don't have an account? ",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                Text(
                    text = if (isSignUpMode) "Sign In" else "Sign Up",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandPrimary,
                    modifier = Modifier.clickable {
                        isSignUpMode = !isSignUpMode
                        viewModel.resetState()
                    }
                )
            }
        }
    }

    // ─── Forgot Password FluidGloss Dialog ───────────────────────────────────
    if (isForgotPasswordOpen) {
        FluidGlossDialog(
            onDismissRequest = { isForgotPasswordOpen = false },
            title = "Reset Password",
            subtitle = "Enter your email address and we'll send you recovery instructions",
            icon = Icons.Outlined.LockReset
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                FluidGlossTextField(
                    value = resetEmail,
                    onValueChange = { resetEmail = it },
                    label = "Account Email *",
                    placeholder = "m@example.com",
                    leadingIcon = {
                        Icon(Icons.Outlined.Email, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FluidGlossButton(
                        text = "Cancel",
                        onClick = { isForgotPasswordOpen = false },
                        variant = FluidGlossButtonVariant.Secondary,
                        modifier = Modifier.weight(1f)
                    )
                    FluidGlossButton(
                        text = "Send Instructions",
                        onClick = {
                            viewModel.forgotPassword(resetEmail) {
                                Toast.makeText(context, "Password reset email sent!", Toast.LENGTH_LONG).show()
                                isForgotPasswordOpen = false
                            }
                        },
                        enabled = resetEmail.isNotBlank(),
                        variant = FluidGlossButtonVariant.Primary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    // ─── Access Denied Modal (for inactive accounts matching webapp) ─────────
    if (isAccessDenied) {
        FluidGlossDialog(
            onDismissRequest = { isAccessDenied = false },
            title = "Access Denied",
            subtitle = "Account Inactive",
            icon = Icons.Outlined.Block
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(RoseDanger.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = RoseDanger, modifier = Modifier.size(24.dp))
                }

                Text(
                    text = "Your account has been marked as inactive. Please contact your company administrator for assistance.",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                FluidGlossButton(
                    text = "Back to Login",
                    onClick = {
                        isAccessDenied = false
                        viewModel.signOut {}
                    },
                    variant = FluidGlossButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
