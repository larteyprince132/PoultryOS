package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.NumberStepper
import com.example.ui.theme.EggGold
import com.example.ui.theme.FarmGreenContainer
import com.example.ui.theme.FarmGreenDark
import com.example.ui.theme.FarmGreenPrimary
import com.example.ui.theme.SyncGreen
import com.example.ui.theme.SyncOrange
import com.example.ui.theme.SyncRed
import com.example.ui.viewmodel.FlockSetupItem
import com.example.ui.viewmodel.PoultryViewModel

enum class AuthScreenMode {
    WELCOME,
    SIGN_IN,
    ONBOARDING_WIZARD
}

@Composable
fun OnboardingScreen(
    viewModel: PoultryViewModel,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mode by remember {
        mutableStateOf(
            if (viewModel.currentUser.value != null && viewModel.activeFarm.value == null) {
                AuthScreenMode.ONBOARDING_WIZARD
            } else {
                AuthScreenMode.WELCOME
            }
        )
    }

    when (mode) {
        AuthScreenMode.WELCOME -> {
            WelcomeView(
                onGoToRegister = { mode = AuthScreenMode.ONBOARDING_WIZARD },
                onGoToSignIn = { mode = AuthScreenMode.SIGN_IN },
                onTryDemo = {
                    viewModel.startDemoMode {
                        onFinished()
                    }
                },
                modifier = modifier
            )
        }
        AuthScreenMode.SIGN_IN -> {
            SignInView(
                viewModel = viewModel,
                onSuccess = onFinished,
                onBack = { mode = AuthScreenMode.WELCOME },
                onGoToRegister = { mode = AuthScreenMode.ONBOARDING_WIZARD },
                modifier = modifier
            )
        }
        AuthScreenMode.ONBOARDING_WIZARD -> {
            OnboardingWizardView(
                viewModel = viewModel,
                onComplete = onFinished,
                onCancel = { mode = AuthScreenMode.WELCOME },
                modifier = modifier
            )
        }
    }
}

@Composable
fun WelcomeView(
    onGoToRegister: () -> Unit,
    onGoToSignIn: () -> Unit,
    onTryDemo: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isStartingDemo by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .testTag("welcome_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App emblem
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(FarmGreenPrimary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Egg,
                contentDescription = null,
                tint = EggGold,
                modifier = Modifier.size(54.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "PoultryOS",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = FarmGreenDark
        )

        Text(
            text = "Commercial Poultry Farm Management",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Track egg harvests, flock populations, feed inventories, sales and finances. Offline-first, reliable and private to your farm.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(36.dp))

        Button(
            onClick = onGoToRegister,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("welcome_register_button"),
            colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Register New Farm Account", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onGoToSignIn,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("welcome_signin_button"),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Sign In to Existing Farm", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Demo Sandbox Mode Button
        Card(
            onClick = {
                isStartingDemo = true
                onTryDemo()
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("welcome_demo_mode_button"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.HelpOutline,
                    contentDescription = null,
                    tint = EggGold,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Try Demo Farm (Sandbox)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Explore features with isolated sample records without creating an account.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isStartingDemo) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                }
            }
        }
    }
}

@Composable
fun SignInView(
    viewModel: PoultryViewModel,
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    onGoToRegister: () -> Unit,
    modifier: Modifier = Modifier
) {
    var emailOrPhone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var recoveryEmail by remember { mutableStateOf("") }
    var recoverySuccessMsg by remember { mutableStateOf<String?>(null) }
    var recoveryErrorMsg by remember { mutableStateOf<String?>(null) }
    var isRecovering by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome Back",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = FarmGreenDark
        )
        Text(
            text = "Sign in to access your farm's records",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (errorMessage != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SyncRed.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = errorMessage!!,
                    color = SyncRed,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = emailOrPhone,
            onValueChange = { emailOrPhone = it },
            label = { Text("Email Address") },
            placeholder = { Text("e.g. farmer@example.com") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("signin_email_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle password visibility"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().testTag("signin_password_input")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = { showForgotPasswordDialog = true },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Forgot Password?", fontSize = 12.sp, color = FarmGreenPrimary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (emailOrPhone.isBlank() || password.isBlank()) {
                    errorMessage = "Please enter your email and password."
                    return@Button
                }
                isLoading = true
                errorMessage = null
                viewModel.loginUser(emailOrPhone, password) { result ->
                    isLoading = false
                    result.fold(
                        onSuccess = { onSuccess() },
                        onFailure = { errorMessage = it.message ?: "Authentication failed." }
                    )
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("signin_submit_button"),
            colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
            } else {
                Text("Sign In", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("Back")
            }
            TextButton(onClick = onGoToRegister) {
                Text("Need an account? Register")
            }
        }

        if (showForgotPasswordDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = {
                    showForgotPasswordDialog = false
                    recoverySuccessMsg = null
                    recoveryErrorMsg = null
                },
                title = { Text("Reset Farm Password", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Enter your email address. If an account uses it, Firebase will send a password reset link.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (recoveryErrorMsg != null) {
                            Text(recoveryErrorMsg!!, color = SyncRed, fontSize = 12.sp)
                        }
                        if (recoverySuccessMsg != null) {
                            Text(recoverySuccessMsg!!, color = SyncGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedTextField(
                            value = recoveryEmail,
                            onValueChange = { recoveryEmail = it },
                            label = { Text("Email Address") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("recovery_email_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (recoveryEmail.isBlank()) {
                                recoveryErrorMsg = "Please enter your email address."
                                return@Button
                            }
                            isRecovering = true
                            recoveryErrorMsg = null
                            recoverySuccessMsg = null
                            viewModel.recoverPassword(recoveryEmail) { result ->
                                isRecovering = false
                                result.fold(
                                    onSuccess = {
                                        recoverySuccessMsg = "If an account uses this email, a reset link has been sent."
                                    },
                                    onFailure = {
                                        recoveryErrorMsg = it.message ?: "Password recovery failed."
                                    }
                                )
                            }
                        },
                        enabled = !isRecovering,
                        colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
                    ) {
                        if (isRecovering) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                        } else {
                            Text("Send Reset Link")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showForgotPasswordDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

@Composable
fun OnboardingWizardView(
    viewModel: PoultryViewModel,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember {
        mutableIntStateOf(if (viewModel.currentUser.value != null) 2 else 1)
    } // 1..6

    // Step 1: Account
    var fullName by remember { mutableStateOf("") }
    var emailOrPhone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Step 2: Farm Profile
    var farmName by remember { mutableStateOf("") }
    var farmLocation by remember { mutableStateOf("") }
    var farmType by remember { mutableStateOf("Commercial Layers") }
    var farmDescription by remember { mutableStateOf("") }

    // Step 3: Flock Config
    val initialFlocks = remember {
        mutableStateListOf(
            FlockSetupItem("Main Layers", "LAYERS", "White Layers", 0, true, 20)
        )
    }

    // Step 4: Production Preferences
    var eggUnit by remember { mutableStateOf("CRATES") } // CRATES, TRAYS, CARTONS, INDIVIDUAL_EGGS
    var eggsPerCrate by remember { mutableIntStateOf(30) }

    // Step 5: Financial Configuration
    var currencySymbol by remember { mutableStateOf("GHS") } // GHS, USD, NGN, KES, EUR, GBP

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
            .testTag("onboarding_wizard")
    ) {
        // Step progress header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Step $step of 6",
                fontWeight = FontWeight.Bold,
                color = FarmGreenPrimary,
                fontSize = 13.sp
            )
            TextButton(onClick = onCancel) {
                Text("Cancel", fontSize = 12.sp)
            }
        }

        LinearProgressIndicator(
            progress = { step / 6f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = FarmGreenPrimary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (errorMessage != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SyncRed.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = errorMessage!!,
                    color = SyncRed,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Step content area
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (step) {
                1 -> {
                    // Step 1: Account
                    Text(
                        text = "1. Create Farmer Account",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Sign in to protect your account. Farm records stay available on this device and can sync when cloud sync is configured.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Your Full Name") },
                        placeholder = { Text("e.g. Kwame Mensah") },
                        modifier = Modifier.fillMaxWidth().testTag("onboarding_fullname_input")
                    )

                    OutlinedTextField(
                        value = emailOrPhone,
                        onValueChange = { emailOrPhone = it },
                        label = { Text("Email Address") },
                        placeholder = { Text("e.g. kwame@example.com") },
                        modifier = Modifier.fillMaxWidth().testTag("onboarding_email_input")
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Choose a Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().testTag("onboarding_password_input")
                    )
                }

                2 -> {
                    // Step 2: Farm Profile
                    Text(
                        text = "2. Farm Profile",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tell us about your poultry operation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = farmName,
                        onValueChange = { farmName = it },
                        label = { Text("Farm Name") },
                        placeholder = { Text("e.g. Sunrise Poultry Farm") },
                        modifier = Modifier.fillMaxWidth().testTag("onboarding_farmname_input")
                    )

                    OutlinedTextField(
                        value = farmLocation,
                        onValueChange = { farmLocation = it },
                        label = { Text("Farm Location (Region / Town)") },
                        placeholder = { Text("e.g. Kumasi / Eastern Region") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Primary Operation Type:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    val types = listOf("Commercial Layers", "Broilers (Meat)", "Mixed Poultry", "Turkeys / Specialty")
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        types.chunked(2).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { t ->
                                    FilterChip(
                                        selected = farmType == t,
                                        onClick = { farmType = t },
                                        label = { Text(t, fontSize = 12.sp) }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = farmDescription,
                        onValueChange = { farmDescription = it },
                        label = { Text("Description (Optional)") },
                        placeholder = { Text("e.g. 5 coops, automated drinkers") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                3 -> {
                    // Step 3: Flock Config
                    Text(
                        text = "3. Starting Flocks (Optional)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Add any birds currently on your farm. You can skip this step and start with 0 birds.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    initialFlocks.forEachIndexed { index, flockItem ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = flockItem.name,
                                    onValueChange = { initialFlocks[index] = flockItem.copy(name = it) },
                                    label = { Text("Flock Name") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = flockItem.breed,
                                        onValueChange = { initialFlocks[index] = flockItem.copy(breed = it) },
                                        label = { Text("Breed / Variety") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = if (flockItem.count == 0) "" else flockItem.count.toString(),
                                        onValueChange = { initialFlocks[index] = flockItem.copy(count = it.toIntOrNull() ?: 0) },
                                        label = { Text("Bird Count") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = flockItem.isLaying,
                                        onClick = { initialFlocks[index] = flockItem.copy(isLaying = true) },
                                        label = { Text("Laying Birds") }
                                    )
                                    FilterChip(
                                        selected = !flockItem.isLaying,
                                        onClick = { initialFlocks[index] = flockItem.copy(isLaying = false) },
                                        label = { Text("Non-Laying / Meat") }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            initialFlocks.add(FlockSetupItem("New Flock ${initialFlocks.size + 1}", "LAYERS", "", 0, true, 20))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Another Bird Category")
                    }
                }

                4 -> {
                    // Step 4: Production Preferences
                    Text(
                        text = "4. Production Measurement",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Select how your farm counts and packages eggs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val units = listOf(
                        "CRATES" to "Standard Crates (30 eggs/crate)",
                        "TRAYS" to "Flat Paper Trays (30 eggs/tray)",
                        "CARTONS" to "Retail Cartons (12 eggs/carton)",
                        "INDIVIDUAL_EGGS" to "Individual Loose Eggs Only"
                    )

                    units.forEach { (key, label) ->
                        Card(
                            onClick = {
                                eggUnit = key
                                eggsPerCrate = if (key == "CARTONS") 12 else 30
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (eggUnit == key) FarmGreenContainer.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (eggUnit == key) Icons.Default.CheckCircle else Icons.Default.Egg,
                                    contentDescription = null,
                                    tint = if (eggUnit == key) FarmGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = label,
                                    fontWeight = if (eggUnit == key) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                5 -> {
                    // Step 5: Financial Configuration
                    Text(
                        text = "5. Currency & Financials",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Select your preferred currency symbol. Default is GHS (GH₵).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val currencies = listOf(
                        "GHS" to "GH₵ (Ghanaian Cedi)",
                        "USD" to "$ (US Dollar)",
                        "NGN" to "₦ (Nigerian Naira)",
                        "KES" to "KSh (Kenyan Shilling)",
                        "EUR" to "€ (Euro)",
                        "GBP" to "£ (British Pound)"
                    )

                    currencies.forEach { (currCode, label) ->
                        Card(
                            onClick = { currencySymbol = currCode },
                            colors = CardDefaults.cardColors(
                                containerColor = if (currencySymbol == currCode) FarmGreenContainer.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (currencySymbol == currCode) Icons.Default.CheckCircle else Icons.Default.AttachMoney,
                                    contentDescription = null,
                                    tint = if (currencySymbol == currCode) FarmGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = label,
                                    fontWeight = if (currencySymbol == currCode) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                6 -> {
                    // Step 6: Confirmation
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(FarmGreenContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = FarmGreenPrimary,
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        Text(
                            text = "Farm Ready for Setup",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Text(
                            text = "You will start with a completely clean, isolated farm database. No sample transactions or invented figures are pre-populated.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Farmer: $fullName", fontWeight = FontWeight.SemiBold)
                                Text("Farm: ${farmName.ifBlank { "My Farm" }} ($farmType)")
                                Text("Currency: $currencySymbol")
                                Text("Egg Unit: $eggUnit")
                                val birdSum = initialFlocks.sumOf { it.count }
                                Text("Starting Birds: $birdSum")
                            }
                        }
                    }
                }
            }
        }

        // Navigation Footer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (step > 1 && step < 6) {
                OutlinedButton(onClick = { step-- }) {
                    Text("Previous")
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            if (step < 6) {
                Button(
                    onClick = {
                        errorMessage = null
                        if (step == 1) {
                            if (fullName.isBlank()) {
                                errorMessage = "Please enter your full name."
                                return@Button
                            }
                            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailOrPhone.trim()).matches()) {
                                errorMessage = "Please enter a valid email address."
                                return@Button
                            }
                            if (password.length < 6) {
                                errorMessage = "Please choose a password with at least 6 characters."
                                return@Button
                            }
                            // Register the user
                            isSubmitting = true
                            viewModel.registerUser(fullName, emailOrPhone, password) { result ->
                                isSubmitting = false
                                result.fold(
                                    onSuccess = { step++ },
                                    onFailure = { errorMessage = it.message }
                                )
                            }
                            return@Button
                        }
                        if (step == 2) {
                            if (farmName.isBlank()) {
                                errorMessage = "Please enter a name for your farm."
                                return@Button
                            }
                        }
                        step++
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                    modifier = Modifier.testTag("onboarding_next_step_button")
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                    } else {
                        Text(if (step == 1) "Create Account" else "Next")
                    }
                }
            } else {
                Button(
                    onClick = {
                        isSubmitting = true
                        viewModel.completeOnboarding(
                            farmName = farmName.ifBlank { "My Poultry Farm" },
                            location = farmLocation,
                            farmType = farmType,
                            description = farmDescription,
                            currencySymbol = currencySymbol,
                            eggCollectionUnit = eggUnit,
                            eggsPerCrate = eggsPerCrate,
                            initialFlocks = initialFlocks.filter { it.count > 0 },
                            onComplete = {
                                isSubmitting = false
                                onComplete()
                            }
                        )
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("onboarding_finish_button")
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    } else {
                        Text("Enter Farm Dashboard", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
