package com.example.constructorapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.constructorapp.theme.*
import com.example.constructorapp.ui.components.*
import com.example.constructorapp.ui.viewmodel.AuthUiState
import com.example.constructorapp.ui.viewmodel.AuthViewModel

val BUSINESS_TYPES = listOf(
    "General Contractor" to "general_contractor",
    "Sub-Contractor" to "sub_contractor",
    "Developer" to "developer",
    "Architectural Firm" to "architectural_firm",
    "Other" to "other"
)

val COMPANY_SIZES = listOf(
    "1-10 employees" to "1-10",
    "11-50 employees" to "11-50",
    "51-200 employees" to "51-200",
    "201-500 employees" to "201-500",
    "500+ employees" to "500+"
)

@Composable
fun RegisterCompanyScreen(
    viewModel: AuthViewModel,
    initialDisplayName: String = "",
    onRegistrationSuccess: () -> Unit,
    onBackToLogin: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var companyName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var selectedBusinessType by remember { mutableStateOf("general_contractor") }
    var selectedCompanySize by remember { mutableStateOf("1-10") }
    var displayName by remember { mutableStateOf(initialDisplayName) }

    var isBusinessTypeDropdownOpen by remember { mutableStateOf(false) }
    var isCompanySizeDropdownOpen by remember { mutableStateOf(false) }

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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Constructor Logo & App Header
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFFFFA043),
                                BrandPrimary,
                                BrandPrimaryDark
                            )
                        )
                    )
                    .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "C",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Constructor",
                fontFamily = OutfitFontFamily,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Register your Company",
                fontFamily = OutfitFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "You're not part of a company yet. Register yours to continue, or ask your admin for an invitation.",
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Main FluidGloss Form Card
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
                    Text(
                        text = "Company Details",
                        fontFamily = OutfitFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    FluidGlossTextField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        label = "Company Name *",
                        placeholder = "e.g. Apex Civil Infra Ltd.",
                        leadingIcon = {
                            Icon(Icons.Outlined.Business, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    FluidGlossTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = "Company Address *",
                        placeholder = "e.g. 123 Tech Park, Chennai, India",
                        leadingIcon = {
                            Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    FluidGlossTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = "Company Phone *",
                        placeholder = "e.g. +91 98401 23456",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                        leadingIcon = {
                            Icon(Icons.Outlined.Phone, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    FluidGlossTextField(
                        value = website,
                        onValueChange = { website = it },
                        label = "Company Website",
                        placeholder = "www.yourcompany.com",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                        leadingIcon = {
                            Icon(Icons.Outlined.Language, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Type of Business Dropdown
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Type of Business *",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )

                        Box(modifier = Modifier.fillMaxWidth()) {
                            val selectedTypeLabel = BUSINESS_TYPES.find { it.second == selectedBusinessType }?.first ?: "General Contractor"
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isBusinessTypeDropdownOpen = true },
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xD9FFFFFF),
                                border = BorderStroke(1.dp, GlassBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = selectedTypeLabel,
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 13.sp,
                                        color = TextPrimary
                                    )
                                    Icon(Icons.Outlined.ArrowDropDown, contentDescription = null, tint = TextSecondary)
                                }
                            }

                            FluidGlossDropdownMenu(
                                expanded = isBusinessTypeDropdownOpen,
                                onDismissRequest = { isBusinessTypeDropdownOpen = false },
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                BUSINESS_TYPES.forEach { (label, value) ->
                                    FluidGlossDropdownMenuItem(
                                        text = label,
                                        selected = selectedBusinessType == value,
                                        onClick = {
                                            selectedBusinessType = value
                                            isBusinessTypeDropdownOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Company Size Dropdown
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Company Size *",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )

                        Box(modifier = Modifier.fillMaxWidth()) {
                            val selectedSizeLabel = COMPANY_SIZES.find { it.second == selectedCompanySize }?.first ?: "1-10 employees"
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isCompanySizeDropdownOpen = true },
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xD9FFFFFF),
                                border = BorderStroke(1.dp, GlassBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = selectedSizeLabel,
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 13.sp,
                                        color = TextPrimary
                                    )
                                    Icon(Icons.Outlined.ArrowDropDown, contentDescription = null, tint = TextSecondary)
                                }
                            }

                            FluidGlossDropdownMenu(
                                expanded = isCompanySizeDropdownOpen,
                                onDismissRequest = { isCompanySizeDropdownOpen = false },
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                COMPANY_SIZES.forEach { (label, value) ->
                                    FluidGlossDropdownMenuItem(
                                        text = label,
                                        selected = selectedCompanySize == value,
                                        onClick = {
                                            selectedCompanySize = value
                                            isCompanySizeDropdownOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }

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

                    FluidGlossButton(
                        text = if (uiState is AuthUiState.Loading) "Creating Company..." else "Create Company & Continue",
                        onClick = {
                            viewModel.registerCompany(
                                companyName = companyName,
                                address = address,
                                phone = phone,
                                website = website,
                                businessType = selectedBusinessType,
                                companySize = selectedCompanySize,
                                displayName = displayName,
                                onSuccess = onRegistrationSuccess
                            )
                        },
                        loading = uiState is AuthUiState.Loading,
                        enabled = companyName.isNotBlank() && address.isNotBlank() && phone.isNotBlank(),
                        variant = FluidGlossButtonVariant.Primary,
                        size = FluidGlossButtonSize.Large,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Logout & Try Different Account Link
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        viewModel.signOut {
                            onBackToLogin()
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Logout,
                    contentDescription = null,
                    tint = BrandPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Signed up with wrong email? Try a different account",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandPrimary
                )
            }
        }
    }
}
