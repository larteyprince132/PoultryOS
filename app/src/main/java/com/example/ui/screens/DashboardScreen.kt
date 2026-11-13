package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Feed
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.FarmEntity
import com.example.ui.components.QuickActionButton
import com.example.ui.components.SyncStatusPill
import com.example.ui.dialogs.RecordEggDialog
import com.example.ui.dialogs.RecordExpenseDialog
import com.example.ui.dialogs.RecordFeedDialog
import com.example.ui.dialogs.RecordSaleDialog
import com.example.ui.theme.EggGold
import com.example.ui.theme.EggGoldLight
import com.example.ui.theme.FarmGreenContainer
import com.example.ui.theme.FarmGreenDark
import com.example.ui.theme.FarmGreenPrimary
import com.example.ui.theme.SyncGreen
import com.example.ui.theme.SyncOrange
import com.example.ui.theme.SyncRed
import com.example.ui.viewmodel.FarmActivityItem
import com.example.ui.viewmodel.FarmAlert
import com.example.ui.viewmodel.PoultryViewModel
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: PoultryViewModel,
    modifier: Modifier = Modifier
) {
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val metrics by viewModel.dashboardMetrics.collectAsStateWithLifecycle()
    val flocks by viewModel.flocks.collectAsStateWithLifecycle()
    val feedInventories by viewModel.feedInventories.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val activeFarm by viewModel.activeFarm.collectAsStateWithLifecycle()
    val userFarms by viewModel.userFarms.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    var showFarmSwitcherDialog by remember { mutableStateOf(false) }
    var showCreateFarmDialog by remember { mutableStateOf(false) }

    var showRecordEggDialog by remember { mutableStateOf(false) }
    var showRecordSaleDialog by remember { mutableStateOf(false) }
    var showRecordFeedDialog by remember { mutableStateOf(false) }
    var showRecordExpenseDialog by remember { mutableStateOf(false) }

    val currency = activeFarm?.currencySymbol ?: "GH₵"

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Demo Sandbox Banner if in demo mode
        if (activeFarm?.isDemoFarm == true) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("demo_mode_banner"),
                    colors = CardDefaults.cardColors(containerColor = EggGoldLight),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = EggGold, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "DEMO SANDBOX MODE (Isolated sample data)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        TextButton(
                            onClick = { viewModel.logout() },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Exit Demo", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EggGold)
                        }
                    }
                }
            }
        }

        // Farm Header & Multi-Farm Switcher
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showFarmSwitcherDialog = true }
                        .padding(vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = activeFarm?.farmName ?: "My Farm",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Switch Farm",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${activeFarm?.location.takeIf { !it.isNullOrBlank() } ?: "Active Farm"} • ${currentUser?.fullName ?: "Farmer"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    SyncStatusPill(
                        syncState = syncState,
                        onSyncClick = { viewModel.triggerSync() }
                    )
                    IconButton(
                        onClick = { viewModel.triggerSync() },
                        modifier = Modifier.size(36.dp).testTag("header_refresh_sync_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 30-Second Farm Health Summary Cockpit (Real User Figures)
        item {
            Text(
                text = "Farm Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Card 1: Active Flock
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = FarmGreenContainer.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "TOTAL BIRDS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = FarmGreenDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${metrics.totalBirds}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = FarmGreenDark
                            )
                            Text(
                                text = if (metrics.totalBirds == 0) "No birds registered yet" else "${metrics.layingHens} laying hens",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Card 2: Today's Egg Harvest
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = EggGoldLight.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "TODAY'S HARVEST",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EggGold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (metrics.todayCrates == 0 && metrics.todayTotalEggs == 0) "0 Crates" else "${metrics.todayCrates} Crates",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (metrics.layingHens == 0) "No laying flock"
                                else "${String.format(Locale.US, "%.1f", metrics.todayLayingRate)}% laying rate",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (metrics.todayLayingRate >= 70f) SyncGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Card 3: Remaining Feed Stock
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = if (metrics.totalFeedBags > 0f && metrics.estimatedFeedDaysLeft <= 5) SyncOrange.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "FEED STOCK",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (metrics.totalFeedBags == 0f) "0 Bags" else "${String.format(Locale.US, "%.1f", metrics.totalFeedBags)} Bags",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (metrics.totalFeedBags == 0f) "No feed recorded in store" else "~${metrics.estimatedFeedDaysLeft} days remaining",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Card 4: Operating Result Today
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = if (metrics.todayNet >= 0) SyncGreen.copy(alpha = 0.12f) else SyncRed.copy(alpha = 0.12f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "TODAY'S NET",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (metrics.todayNet >= 0) SyncGreen else SyncRed
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$currency${String.format(Locale.US, "%.0f", metrics.todayNet)}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "+$currency${metrics.todayRevenue.toInt()} / -$currency${metrics.todayExpenses.toInt()}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 4 Prominent Quick Action Buttons
        item {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionButton(
                        icon = Icons.Default.Egg,
                        label = "Record Eggs",
                        sublabel = "Daily crate collection",
                        backgroundColor = EggGoldLight.copy(alpha = 0.45f),
                        iconTint = EggGold,
                        onClick = { showRecordEggDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionButton(
                        icon = Icons.Default.AttachMoney,
                        label = "Record Sale",
                        sublabel = "Egg crates, birds, manure",
                        backgroundColor = FarmGreenContainer.copy(alpha = 0.45f),
                        iconTint = FarmGreenPrimary,
                        onClick = { showRecordSaleDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionButton(
                        icon = Icons.Default.Grain,
                        label = "Record Feed",
                        sublabel = "Feeding birds or restock",
                        backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        iconTint = Color(0xFF8D6E63),
                        onClick = { showRecordFeedDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionButton(
                        icon = Icons.Default.ReceiptLong,
                        label = "Record Expense",
                        sublabel = "Medication, labour, utilities",
                        backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        iconTint = Color(0xFFD32F2F),
                        onClick = { showRecordExpenseDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Empty state guide if farm is completely new
        if (metrics.totalBirds == 0 && metrics.recentActivities.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("empty_farm_card"),
                    colors = CardDefaults.cardColors(containerColor = FarmGreenContainer.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Groups, contentDescription = null, tint = FarmGreenPrimary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Welcome to your new farm!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = FarmGreenDark
                            )
                        }
                        Text(
                            text = "Your farm is completely clean. Get started by recording your flocks, today's egg collection, or feed inventory using the quick action buttons above.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // Active Alerts & Reminders
        if (metrics.alerts.isNotEmpty()) {
            item {
                Text(
                    text = "Important Reminders",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    metrics.alerts.forEach { alert ->
                        AlertItemCard(alert)
                    }
                }
            }
        }

        // Recent Farm Activities Feed
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${metrics.recentActivities.size} records",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (metrics.recentActivities.isEmpty()) {
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
                        Text(
                            text = "No Activities Recorded Yet",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Your entries will appear here once you record collections, sales, or feed.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(metrics.recentActivities, key = { it.id }) { activity ->
                ActivityRow(activity)
            }
        }
    }

    // Farm Switcher Dialog
    if (showFarmSwitcherDialog) {
        AlertDialog(
            onDismissRequest = { showFarmSwitcherDialog = false },
            title = { Text("Select Active Farm") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    userFarms.forEach { farm ->
                        Card(
                            onClick = {
                                viewModel.selectActiveFarm(farm)
                                showFarmSwitcherDialog = false
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (farm.id == activeFarm?.id) FarmGreenContainer.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = farm.farmName, fontWeight = FontWeight.Bold)
                                    Text(text = "${farm.farmType} • ${farm.location}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (farm.id == activeFarm?.id) {
                                    Text("Active", color = FarmGreenPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedButton(
                        onClick = {
                            showFarmSwitcherDialog = false
                            showCreateFarmDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Another Farm Site")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFarmSwitcherDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Create New Farm Site Dialog
    if (showCreateFarmDialog) {
        var newFarmName by remember { mutableStateOf("") }
        var newFarmLoc by remember { mutableStateOf("") }
        var newFarmType by remember { mutableStateOf("Commercial Layers") }

        AlertDialog(
            onDismissRequest = { showCreateFarmDialog = false },
            title = { Text("Add New Farm Site") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newFarmName,
                        onValueChange = { newFarmName = it },
                        label = { Text("Farm Site Name") },
                        placeholder = { Text("e.g. Coop B / Broiler Site") },
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

    // Action Dialogs
    if (showRecordEggDialog) {
        RecordEggDialog(
            flocks = flocks,
            onDismiss = { showRecordEggDialog = false },
            onSave = { dateStr, period, flockId, flockName, crates, loose, broken, notes ->
                viewModel.recordEggCollection(dateStr, period, flockId, flockName, crates, loose, broken, notes)
                showRecordEggDialog = false
            }
        )
    }

    if (showRecordSaleDialog) {
        RecordSaleDialog(
            customers = customers,
            flocks = flocks,
            availableEggCrates = metrics.unsoldEggCratesInventory,
            currencySymbol = activeFarm?.currencySymbol ?: "GH₵",
            defaultEggPrice = activeFarm?.defaultEggCratePrice ?: 35.0,
            onDismiss = { showRecordSaleDialog = false },
            onSave = { dateStr, itemType, quantity, unitPrice, amountPaid, customerName, customerPhone, customerId, notes, targetFlockId ->
                viewModel.recordSale(dateStr, itemType, quantity, unitPrice, amountPaid, customerName, customerPhone, customerId, notes, targetFlockId)
                showRecordSaleDialog = false
            }
        )
    }

    if (showRecordFeedDialog) {
        RecordFeedDialog(
            feedInventories = feedInventories,
            flocks = flocks,
            defaultBagPrice = activeFarm?.defaultFeedBagPrice ?: 220.0,
            onDismiss = { showRecordFeedDialog = false },
            onSaveConsumption = { dateStr, feedTypeId, flockId, flockName, bagsQty, notes ->
                viewModel.recordFeedConsumption(dateStr, feedTypeId, flockId, flockName, bagsQty, notes)
                showRecordFeedDialog = false
            },
            onSavePurchase = { dateStr, feedTypeId, bagsQty, costPerBag, supplierName, recordAsExpense, notes ->
                viewModel.recordFeedPurchase(dateStr, feedTypeId, "General Feed", bagsQty, costPerBag, supplierName, recordAsExpense, notes)
                showRecordFeedDialog = false
            }
        )
    }

    if (showRecordExpenseDialog) {
        RecordExpenseDialog(
            onDismiss = { showRecordExpenseDialog = false },
            onSave = { dateStr, category, amount, paymentMethod, desc, recipient ->
                viewModel.recordExpense(dateStr, category, amount, paymentMethod, desc, recipient)
                showRecordExpenseDialog = false
            }
        )
    }
}



@Composable
fun AlertItemCard(alert: com.example.ui.viewmodel.FarmAlert) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (alert.severity) {
                "HIGH" -> MaterialTheme.colorScheme.errorContainer
                "WARNING" -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (alert.severity) {
                    "HIGH" -> "⚠️"
                    "WARNING" -> "ℹ️"
                    else -> "🔔"
                },
                modifier = Modifier.padding(end = 8.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alert.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = alert.description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ActivityRow(activity: com.example.ui.viewmodel.FarmActivityItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (activity.category) {
                    "EGG" -> "🥚"
                    "FEED" -> "🌾"
                    "SALE" -> "💰"
                    "EXPENSE" -> "🧾"
                    else -> "📋"
                },
                modifier = Modifier.padding(end = 12.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Text(
                    text = "${activity.timeLabel} • ${activity.subtitle}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
