package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CustomerEntity
import com.example.data.model.FarmEntity
import com.example.data.model.FlockEntity
import com.example.data.model.HealthLogEntity
import com.example.ui.components.NumberStepper
import com.example.ui.theme.EggGold
import com.example.ui.theme.FarmGreenContainer
import com.example.ui.theme.FarmGreenDark
import com.example.ui.theme.FarmGreenPrimary
import com.example.ui.theme.SyncGreen
import com.example.ui.theme.SyncOrange
import com.example.ui.theme.SyncRed
import com.example.ui.viewmodel.PoultryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MoreScreen(
    viewModel: PoultryViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val flocks by viewModel.flocks.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val healthLogs by viewModel.healthLogs.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val activeFarm by viewModel.activeFarm.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val userFarms by viewModel.userFarms.collectAsStateWithLifecycle()
    val metrics by viewModel.dashboardMetrics.collectAsStateWithLifecycle()
    val sales by viewModel.sales.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()

    var selectedSection by remember { mutableIntStateOf(0) }
    // 0 = Flocks, 1 = Health, 2 = Customers, 3 = Settings & Multi-Farm

    var showAddFlockDialog by remember { mutableStateOf(false) }
    var showAdjustFlockDialog by remember { mutableStateOf<FlockEntity?>(null) }
    var flockToEdit by remember { mutableStateOf<FlockEntity?>(null) }
    var flockToDelete by remember { mutableStateOf<FlockEntity?>(null) }
    var healthLogToDelete by remember { mutableStateOf<HealthLogEntity?>(null) }
    var showAddHealthDialog by remember { mutableStateOf(false) }
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var showSettleCustomerDialog by remember { mutableStateOf<CustomerEntity?>(null) }
    var customerToEdit by remember { mutableStateOf<CustomerEntity?>(null) }
    var customerToDelete by remember { mutableStateOf<CustomerEntity?>(null) }
    var showCreateFarmDialog by remember { mutableStateOf(false) }
    var showEditFarmSettingsDialog by remember { mutableStateOf(false) }

    val currency = activeFarm?.currencySymbol ?: "GH₵"

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("more_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Farm Management & Tools",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Flock populations, health logs, customer accounts, and farm settings",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Section Tabs
        item {
            TabRow(
                selectedTabIndex = selectedSection,
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedSection == 0,
                    onClick = { selectedSection = 0 },
                    text = { Text("Flocks (${flocks.size})") }
                )
                Tab(
                    selected = selectedSection == 1,
                    onClick = { selectedSection = 1 },
                    text = { Text("Health") }
                )
                Tab(
                    selected = selectedSection == 2,
                    onClick = { selectedSection = 2 },
                    text = { Text("Customers (${customers.size})") }
                )
                Tab(
                    selected = selectedSection == 3,
                    onClick = { selectedSection = 3 },
                    text = { Text("Settings") }
                )
            }
        }

        // Section 0: Flocks
        if (selectedSection == 0) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Flock Populations (${metrics.totalBirds} Total Birds)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = { showAddFlockDialog = true },
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Flock", fontSize = 12.sp)
                    }
                }
            }

            if (flocks.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No Flocks Registered Yet", fontWeight = FontWeight.Bold)
                            Text(
                                "Tap 'New Flock' to add your bird categories, breeds, and starting populations.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(flocks, key = { it.id }) { flock ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = flock.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (flock.isLayingFlock) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(EggGold.copy(alpha = 0.15f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("LAYING", color = EggGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Text(
                                    text = "Type: ${flock.birdType}${if (flock.breed.isNotBlank()) " • Breed: ${flock.breed}" else ""} • Age: ~${flock.ageWeeks} wks",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (flock.notes.isNotBlank()) {
                                    Text(
                                        text = flock.notes,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${flock.currentCount} birds",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = FarmGreenPrimary
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(
                                        onClick = { showAdjustFlockDialog = flock },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Adjust", fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { flockToEdit = flock },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit flock", modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { flockToDelete = flock },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete flock", modifier = Modifier.size(15.dp), tint = SyncOrange)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 1: Health & Medication
        if (selectedSection == 1) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Health & Medication Logs",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = { showAddHealthDialog = true },
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Log Event", fontSize = 12.sp)
                    }
                }
            }

            // Veterinary disclaimer card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.HealthAndSafety,
                            contentDescription = null,
                            tint = FarmGreenPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Record-keeping log only. Consult a certified veterinary officer for official diagnosis and prescriptions.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (healthLogs.isEmpty()) {
                item {
                    Text(
                        text = "No health or mortality events logged for this farm.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else {
                items(healthLogs, key = { it.id }) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(10.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (log.logType == "MORTALITY") SyncRed.copy(alpha = 0.15f)
                                        else FarmGreenPrimary.copy(alpha = 0.15f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (log.logType == "MORTALITY") Icons.Default.Warning else Icons.Default.HealthAndSafety,
                                    contentDescription = null,
                                    tint = if (log.logType == "MORTALITY") SyncRed else FarmGreenPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${log.logType}: ${log.titleOrDrug}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "${log.flockName} • ${log.dateString}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (log.countAffected > 0) {
                                Text(
                                    text = "${log.countAffected} birds",
                                    fontWeight = FontWeight.Bold,
                                    color = if (log.logType == "MORTALITY") SyncRed else FarmGreenPrimary,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { healthLogToDelete = log },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete log", modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Customers
        if (selectedSection == 2) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Customer Directory",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = { showAddCustomerDialog = true },
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Customer", fontSize = 12.sp)
                    }
                }
            }

            if (customers.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No Customers Registered Yet", fontWeight = FontWeight.Bold)
                            Text(
                                "Add buyers and wholesalers to track credit balances and repeat sales.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(customers, key = { it.id }) { customer ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = customer.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (customer.phone.isNotBlank()) {
                                    Text(
                                        text = "Phone: ${customer.phone}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                if (customer.outstandingBalance > 0) {
                                    Text(
                                        text = "Owes: $currency${customer.outstandingBalance.toInt()}",
                                        fontWeight = FontWeight.Bold,
                                        color = SyncOrange,
                                        fontSize = 13.sp
                                    )
                                    Button(
                                        onClick = { showSettleCustomerDialog = customer },
                                        modifier = Modifier.height(32.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                    ) {
                                        Text("Settle", fontSize = 11.sp)
                                    }
                                } else {
                                    Text(
                                        text = "Settled",
                                        color = SyncGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Settings, User Account & Multi-Farm
        if (selectedSection == 3) {
            // User Account Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = FarmGreenContainer.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(FarmGreenPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = currentUser?.fullName ?: "Farmer",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = currentUser?.emailOrPhone ?: "Signed in",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.logout() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SyncRed)
                        ) {
                            Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sign Out", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Multi-Farm Manager Card
            item {
                Text(
                    text = "My Farms & Sites",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        userFarms.forEach { farm ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (farm.id == activeFarm?.id) FarmGreenContainer.copy(alpha = 0.5f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(farm.farmName, fontWeight = FontWeight.Bold)
                                    Text("${farm.farmType} • Currency: ${farm.currencySymbol}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (farm.id == activeFarm?.id) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Active", color = FarmGreenPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        TextButton(
                                            onClick = { showEditFarmSettingsDialog = true },
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("Edit", fontSize = 12.sp)
                                        }
                                    }
                                } else {
                                    TextButton(
                                        onClick = { viewModel.selectActiveFarm(farm) },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Switch", fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = { showCreateFarmDialog = true },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Another Farm Site")
                        }
                    }
                }
            }

            // Offline Synchronization Controls
            item {
                Text(
                    text = "Offline Synchronization",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Sync Status", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = syncState.globalStatus.label,
                                    fontWeight = FontWeight.Bold,
                                    color = syncState.globalStatus.badgeColor,
                                    fontSize = 14.sp
                                )
                            }
                            Button(
                                onClick = { viewModel.triggerSync() },
                                enabled = !syncState.isSyncing
                            ) {
                                Text(if (syncState.isSyncing) "Syncing..." else "Sync Now")
                            }
                        }

                        Text(
                            text = "Pending Local Records: ${syncState.pendingCount}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Simulate Offline Mode", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Test recording without network connection", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = syncState.isSimulatedOffline,
                                onCheckedChange = { viewModel.toggleOfflineSimulation() }
                            )
                        }
                    }
                }
            }

            // Report Export
            item {
                Button(
                    onClick = {
                        val reportText = buildString {
                            appendLine("📊 PoultryOS Farm Report")
                            appendLine("Farm: ${activeFarm?.farmName}")
                            appendLine("Location: ${activeFarm?.location}")
                            appendLine("Date: ${SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date())}")
                            appendLine("-------------------------------")
                            appendLine("Total Birds: ${metrics.totalBirds} (${metrics.layingHens} laying hens)")
                            appendLine("Today's Eggs: ${metrics.todayCrates} crates (${metrics.todayTotalEggs} eggs)")
                            appendLine("Feed In Stock: ${String.format(Locale.US, "%.1f", metrics.totalFeedBags)} bags")
                            appendLine("-------------------------------")
                            appendLine("Financial Summary ($currency):")
                            appendLine("Total Revenue: $currency${sales.sumOf { it.totalAmount }}")
                            appendLine("Cash Collected: $currency${sales.sumOf { it.amountPaid }}")
                            appendLine("Total Expenses: $currency${expenses.sumOf { it.amount }}")
                            appendLine("Operating Profit: $currency${sales.sumOf { it.totalAmount } - expenses.sumOf { it.amount }}")
                        }
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, reportText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Poultry Farm Report"))
                    },
                    modifier = Modifier.fillMaxWidth().testTag("share_farm_report_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share Farm Summary Report")
                }
            }
        }
    }

    // Add Flock Dialog
    if (showAddFlockDialog) {
        var name by remember { mutableStateOf("") }
        var birdType by remember { mutableStateOf("LAYERS") }
        var breed by remember { mutableStateOf("") }
        var count by remember { mutableIntStateOf(50) }
        var isLaying by remember { mutableStateOf(true) }
        var ageWeeks by remember { mutableIntStateOf(20) }

        AlertDialog(
            onDismissRequest = { showAddFlockDialog = false },
            title = { Text("Register New Flock") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Flock Name (e.g. Isa Brown)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = breed,
                        onValueChange = { breed = it },
                        label = { Text("Breed / Variety") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    NumberStepper(
                        value = count,
                        onValueChange = { count = it },
                        label = "Initial Bird Count",
                        unit = "birds",
                        min = 1,
                        max = 100000
                    )
                    NumberStepper(
                        value = ageWeeks,
                        onValueChange = { ageWeeks = it },
                        label = "Bird Age",
                        unit = "weeks",
                        min = 1,
                        max = 120
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = isLaying,
                            onClick = { isLaying = true },
                            label = { Text("Laying Flock") }
                        )
                        FilterChip(
                            selected = !isLaying,
                            onClick = { isLaying = false },
                            label = { Text("Meat / Other") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.addFlock(name, birdType, breed, count, isLaying, ageWeeks, "")
                            showAddFlockDialog = false
                        }
                    }
                ) {
                    Text("Register Flock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFlockDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Adjust Flock Dialog (Supports Mortality, Additions, Sales, Transfers)
    showAdjustFlockDialog?.let { flock ->
        var changeType by remember { mutableStateOf("MORTALITY") }
        var countToChange by remember { mutableIntStateOf(1) }
        var reasonNotes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAdjustFlockDialog = null },
            title = { Text("Flock Change: ${flock.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Current Population: ${flock.currentCount} birds", fontWeight = FontWeight.Bold, color = FarmGreenPrimary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = changeType == "MORTALITY",
                            onClick = { changeType = "MORTALITY" },
                            label = { Text("Mortality") }
                        )
                        FilterChip(
                            selected = changeType == "ADDITION",
                            onClick = { changeType = "ADDITION" },
                            label = { Text("New Birds") }
                        )
                        FilterChip(
                            selected = changeType == "SALE",
                            onClick = { changeType = "SALE" },
                            label = { Text("Sale/Transfer") }
                        )
                    }
                    NumberStepper(
                        value = countToChange,
                        onValueChange = { countToChange = it },
                        label = if (changeType == "ADDITION") "Birds to Add" else "Birds Deducted",
                        unit = "birds",
                        min = 1,
                        max = if (changeType == "ADDITION") 10000 else flock.currentCount.coerceAtLeast(1)
                    )
                    OutlinedTextField(
                        value = reasonNotes,
                        onValueChange = { reasonNotes = it },
                        label = { Text("Reason / Observations") },
                        placeholder = { Text(if (changeType == "MORTALITY") "e.g. Heat stress, respiratory symptoms" else "e.g. New day-old chicks batch") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.recordFlockPopulationChange(
                            flockId = flock.id,
                            changeType = changeType,
                            count = countToChange,
                            notes = reasonNotes
                        )
                        showAdjustFlockDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (changeType == "MORTALITY") SyncRed else FarmGreenPrimary
                    )
                ) {
                    Text(if (changeType == "ADDITION") "Add Birds" else "Record Deduction")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdjustFlockDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    flockToDelete?.let { flock ->
        AlertDialog(
            onDismissRequest = { flockToDelete = null },
            title = { Text("Delete Flock Group", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete ${flock.name}? This will remove this bird group from active counts.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFlock(flock)
                        flockToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SyncOrange)
                ) {
                    Text("Delete Flock")
                }
            },
            dismissButton = {
                TextButton(onClick = { flockToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    flockToEdit?.let { flock ->
        var editName by remember { mutableStateOf(flock.name) }
        var editBreed by remember { mutableStateOf(flock.breed) }
        var editType by remember { mutableStateOf(flock.birdType) }
        var editAge by remember { mutableIntStateOf(flock.ageWeeks) }
        var editIsLaying by remember { mutableStateOf(flock.isLayingFlock) }
        var editNotes by remember { mutableStateOf(flock.notes) }

        AlertDialog(
            onDismissRequest = { flockToEdit = null },
            title = { Text("Edit Flock Details", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Flock Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editBreed,
                        onValueChange = { editBreed = it },
                        label = { Text("Breed") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    NumberStepper(
                        value = editAge,
                        onValueChange = { editAge = it },
                        label = "Age",
                        unit = "weeks",
                        min = 1,
                        max = 150
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = editIsLaying,
                            onClick = { editIsLaying = true },
                            label = { Text("Laying Flock") }
                        )
                        FilterChip(
                            selected = !editIsLaying,
                            onClick = { editIsLaying = false },
                            label = { Text("Meat / Other") }
                        )
                    }
                    OutlinedTextField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateFlock(
                            flock.copy(
                                name = editName.trim(),
                                breed = editBreed.trim(),
                                birdType = editType,
                                ageWeeks = editAge,
                                isLayingFlock = editIsLaying,
                                notes = editNotes.trim()
                            )
                        )
                        flockToEdit = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { flockToEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    healthLogToDelete?.let { log ->
        AlertDialog(
            onDismissRequest = { healthLogToDelete = null },
            title = { Text("Delete Health Log", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this ${log.logType} record for ${log.flockName}?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteHealthLog(log)
                        healthLogToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SyncOrange)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { healthLogToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Health Dialog
    if (showAddHealthDialog) {
        var logType by remember { mutableStateOf("VACCINATION") }
        var title by remember { mutableStateOf("") }
        var countAffected by remember { mutableIntStateOf(0) }
        var notes by remember { mutableStateOf("") }
        val defaultFlock = flocks.firstOrNull()
        var selectedFlock by remember { mutableStateOf(defaultFlock) }

        AlertDialog(
            onDismissRequest = { showAddHealthDialog = false },
            title = { Text("Record Health Event") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("VACCINATION", "MEDICATION", "MORTALITY").forEach { type ->
                            FilterChip(
                                selected = logType == type,
                                onClick = { logType = type },
                                label = { Text(type) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(if (logType == "VACCINATION") "Vaccine Name" else if (logType == "MEDICATION") "Medication Name" else "Cause of Mortality") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (logType == "MORTALITY") {
                        NumberStepper(
                            value = countAffected,
                            onValueChange = { countAffected = it },
                            label = "Dead birds (will automatically deduct from flock count)",
                            unit = "birds",
                            min = 1,
                            max = 1000
                        )
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Observations & Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val flock = selectedFlock ?: flocks.firstOrNull() ?: return@Button
                        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        viewModel.recordHealthLog(
                            today,
                            logType,
                            flock.id,
                            flock.name,
                            title.ifBlank { logType },
                            countAffected,
                            "Farm Manager",
                            null,
                            notes
                        )
                        showAddHealthDialog = false
                    }
                ) {
                    Text("Save Event")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddHealthDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Customer Dialog
    if (showAddCustomerDialog) {
        var custName by remember { mutableStateOf("") }
        var custPhone by remember { mutableStateOf("") }
        var custNotes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddCustomerDialog = false },
            title = { Text("Register Customer") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = custName,
                        onValueChange = { custName = it },
                        label = { Text("Customer Name / Shop") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = custPhone,
                        onValueChange = { custPhone = it },
                        label = { Text("Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = custNotes,
                        onValueChange = { custNotes = it },
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (custName.isNotBlank()) {
                            viewModel.addCustomer(custName, custPhone, custNotes)
                            showAddCustomerDialog = false
                        }
                    }
                ) {
                    Text("Save Customer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    customerToDelete?.let { cust ->
        AlertDialog(
            onDismissRequest = { customerToDelete = null },
            title = { Text("Delete Customer", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete ${cust.name}? Outstanding balance: $currency${cust.outstandingBalance}") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCustomer(cust)
                        customerToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SyncOrange)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { customerToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    customerToEdit?.let { cust ->
        var editName by remember { mutableStateOf(cust.name) }
        var editPhone by remember { mutableStateOf(cust.phone) }
        var editNotes by remember { mutableStateOf(cust.notes) }

        AlertDialog(
            onDismissRequest = { customerToEdit = null },
            title = { Text("Edit Customer Details", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Customer Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        label = { Text("Notes / Store Location") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank()) {
                            viewModel.updateCustomer(
                                cust.copy(
                                    name = editName.trim(),
                                    phone = editPhone.trim(),
                                    notes = editNotes.trim()
                                )
                            )
                            customerToEdit = null
                        }
                    }
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { customerToEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Settle Customer Dialog
    showSettleCustomerDialog?.let { customer ->
        var settleAmountStr by remember { mutableStateOf(customer.outstandingBalance.toInt().toString()) }

        AlertDialog(
            onDismissRequest = { showSettleCustomerDialog = null },
            title = { Text("Settle Debt: ${customer.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Current outstanding balance: $currency${String.format(Locale.US, "%.2f", customer.outstandingBalance)}",
                        fontWeight = FontWeight.Bold,
                        color = SyncOrange
                    )
                    OutlinedTextField(
                        value = settleAmountStr,
                        onValueChange = { settleAmountStr = it },
                        label = { Text("Amount Paid ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = settleAmountStr.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            viewModel.settleCustomerBalance(customer.id, amount)
                            showSettleCustomerDialog = null
                        }
                    }
                ) {
                    Text("Confirm Receipt")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettleCustomerDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Create New Farm Dialog
    if (showEditFarmSettingsDialog && activeFarm != null) {
        val farm = activeFarm!!
        var editName by remember { mutableStateOf(farm.farmName) }
        var editLocation by remember { mutableStateOf(farm.location) }
        var editType by remember { mutableStateOf(farm.farmType) }
        var editDesc by remember { mutableStateOf(farm.farmDescription) }
        var editCurrency by remember { mutableStateOf(farm.currencySymbol) }
        var editUnit by remember { mutableStateOf(farm.eggCollectionUnit) }
        var editEggsPerCrate by remember { mutableIntStateOf(farm.eggsPerCrate) }
        var editEggPrice by remember { mutableStateOf(farm.defaultEggCratePrice.toString()) }
        var editFeedPrice by remember { mutableStateOf(farm.defaultFeedBagPrice.toString()) }

        AlertDialog(
            onDismissRequest = { showEditFarmSettingsDialog = false },
            title = { Text("Edit Farm Settings", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Farm Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_farm_name_input")
                    )
                    OutlinedTextField(
                        value = editLocation,
                        onValueChange = { editLocation = it },
                        label = { Text("Location / Region") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editType,
                        onValueChange = { editType = it },
                        label = { Text("Farming Type (e.g. Layers, Broilers)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editDesc,
                        onValueChange = { editDesc = it },
                        label = { Text("Farm Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editCurrency,
                        onValueChange = { editCurrency = it },
                        label = { Text("Currency Symbol (e.g. GHS, USD, NGN)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_farm_currency_input")
                    )
                    OutlinedTextField(
                        value = editEggPrice,
                        onValueChange = { editEggPrice = it },
                        label = { Text("Default Egg Crate Price") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editFeedPrice,
                        onValueChange = { editFeedPrice = it },
                        label = { Text("Default Feed Bag Price") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateFarmSettings(
                            farmId = farm.id,
                            farmName = editName,
                            location = editLocation,
                            farmType = editType,
                            farmDescription = editDesc,
                            currencySymbol = editCurrency,
                            eggCollectionUnit = editUnit,
                            eggsPerCrate = editEggsPerCrate,
                            defaultEggPrice = editEggPrice.toDoubleOrNull() ?: farm.defaultEggCratePrice,
                            defaultFeedPrice = editFeedPrice.toDoubleOrNull() ?: farm.defaultFeedBagPrice,
                            onComplete = { showEditFarmSettingsDialog = false }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditFarmSettingsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCreateFarmDialog) {
        var newFarmName by remember { mutableStateOf("") }
        var newFarmLoc by remember { mutableStateOf("") }
        var newFarmType by remember { mutableStateOf("Commercial Layers") }

        AlertDialog(
            onDismissRequest = { showCreateFarmDialog = false },
            title = { Text("Add Farm Site") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newFarmName,
                        onValueChange = { newFarmName = it },
                        label = { Text("Farm Site Name") },
                        placeholder = { Text("e.g. Coop 2 / Broiler Unit") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newFarmLoc,
                        onValueChange = { newFarmLoc = it },
                        label = { Text("Location") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFarmName.isNotBlank()) {
                            viewModel.completeOnboarding(
                                farmName = newFarmName,
                                location = newFarmLoc,
                                farmType = newFarmType,
                                description = "",
                                currencySymbol = activeFarm?.currencySymbol ?: "GHS",
                                eggCollectionUnit = activeFarm?.eggCollectionUnit ?: "CRATES",
                                eggsPerCrate = activeFarm?.eggsPerCrate ?: 30,
                                initialFlocks = emptyList(),
                                onComplete = { showCreateFarmDialog = false }
                            )
                        }
                    }
                ) {
                    Text("Create Farm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFarmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
