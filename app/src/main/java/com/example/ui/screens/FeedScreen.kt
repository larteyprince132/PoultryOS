package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.FeedInventoryEntity
import com.example.data.model.FeedLogEntity
import com.example.ui.dialogs.RecordFeedDialog
import com.example.ui.theme.EggGold
import com.example.ui.theme.FarmGreenContainer
import com.example.ui.theme.FarmGreenPrimary
import com.example.ui.theme.SyncGreen
import com.example.ui.theme.SyncOrange
import com.example.ui.viewmodel.PoultryViewModel
import java.util.Locale

@Composable
fun FeedScreen(
    viewModel: PoultryViewModel,
    modifier: Modifier = Modifier
) {
    val feedInventories by viewModel.feedInventories.collectAsStateWithLifecycle()
    val feedLogs by viewModel.feedLogs.collectAsStateWithLifecycle()
    val flocks by viewModel.flocks.collectAsStateWithLifecycle()
    val metrics by viewModel.dashboardMetrics.collectAsStateWithLifecycle()
    val activeFarm by viewModel.activeFarm.collectAsStateWithLifecycle()

    val currency = activeFarm?.currencySymbol ?: "GH₵"
    var showFeedDialog by remember { mutableStateOf(false) }
    var showAddTypeDialog by remember { mutableStateOf(false) }
    var logToDelete by remember { mutableStateOf<FeedLogEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showFeedDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Feed / Restock") },
                containerColor = FarmGreenPrimary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("feed_action_fab")
            )
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("feed_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Feed Management",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Track feed stocks, daily consumption, and reorder alerts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Shortage Prediction Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (metrics.totalFeedBags > 0f && metrics.estimatedFeedDaysLeft <= 5) SyncOrange.copy(alpha = 0.15f)
                        else FarmGreenContainer.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (metrics.totalFeedBags > 0f && metrics.estimatedFeedDaysLeft <= 5) SyncOrange else FarmGreenPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (metrics.totalFeedBags > 0f && metrics.estimatedFeedDaysLeft <= 5) Icons.Default.Warning else Icons.Default.Grain,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "FEED RUN-OUT PREDICTION",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (metrics.totalFeedBags > 0f && metrics.estimatedFeedDaysLeft <= 5) SyncOrange else FarmGreenPrimary
                            )
                            Text(
                                text = if (metrics.totalFeedBags == 0f) "No Feed Stock in Store"
                                else "~${metrics.estimatedFeedDaysLeft} Days of Feed Left",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (metrics.totalFeedBags == 0f) "Record feed purchases or restock below."
                                else "${String.format(Locale.US, "%.1f", metrics.totalFeedBags)} total bags in store for ${metrics.totalBirds} birds",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Current Feed Inventories by Type
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Feed Stock by Variety",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = { showAddTypeDialog = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Variety", fontSize = 12.sp)
                    }
                }
            }

            if (feedInventories.isEmpty()) {
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
                            Text("No Feed Varieties Logged", fontWeight = FontWeight.Bold)
                            Text(
                                "Add feed varieties (e.g. Layers Mash, Broiler Starter) to monitor stock levels.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(onClick = { showAddTypeDialog = true }) {
                                Text("Add Feed Variety")
                            }
                        }
                    }
                }
            } else {
                items(feedInventories, key = { it.id }) { item ->
                    FeedStockCard(item = item, currency = currency)
                }
            }

            // History Log Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Feed Activity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${feedLogs.size} logs",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (feedLogs.isEmpty()) {
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
                                text = "No Feed Activities Logged Yet",
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Tap 'Feed / Restock' to record daily feeding or newly purchased bags.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(feedLogs, key = { it.id }) { log ->
                    FeedLogRow(
                        log = log,
                        currency = currency,
                        onDelete = { logToDelete = log }
                    )
                }
            }
        }
    }

    logToDelete?.let { log ->
        AlertDialog(
            onDismissRequest = { logToDelete = null },
            title = { Text("Delete Feed Log", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this " + log.logType + " of " + log.bagsQuantity + " bags? The inventory count will be automatically restored.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFeedLog(log)
                        logToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = SyncOrange)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { logToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showFeedDialog) {
        RecordFeedDialog(
            feedInventories = feedInventories,
            flocks = flocks,
            defaultBagPrice = activeFarm?.defaultFeedBagPrice ?: 0.0,
            currencySymbol = currency,
            onDismiss = { showFeedDialog = false },
            onSaveConsumption = { dateStr, feedTypeId, flockId, flockName, bagsQty, notes ->
                viewModel.recordFeedConsumption(dateStr, feedTypeId, flockId, flockName, bagsQty, notes)
                showFeedDialog = false
            },
            onSavePurchase = { dateStr, feedTypeId, bagsQty, costPerBag, supplierName, recordAsExpense, notes ->
                viewModel.recordFeedPurchase(dateStr, feedTypeId, "Feed", bagsQty, costPerBag, supplierName, recordAsExpense, notes)
                showFeedDialog = false
            }
        )
    }

    if (showAddTypeDialog) {
        var typeName by remember { mutableStateOf("") }
        var brand by remember { mutableStateOf("") }
        var initialBagsStr by remember { mutableStateOf("0") }
        var costPerBagStr by remember { mutableStateOf("") }
        var reorderStr by remember { mutableStateOf("5") }

        AlertDialog(
            onDismissRequest = { showAddTypeDialog = false },
            title = { Text("Add Feed Variety") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = typeName,
                        onValueChange = { typeName = it },
                        label = { Text("Feed Name (e.g. Layers Mash)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = brand,
                        onValueChange = { brand = it },
                        label = { Text("Brand / Supplier") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = initialBagsStr,
                        onValueChange = { initialBagsStr = it },
                        label = { Text("Initial Bags in Stock") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = costPerBagStr,
                        onValueChange = { costPerBagStr = it },
                        label = { Text("Cost per bag ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (typeName.isNotBlank()) {
                            viewModel.addFeedInventory(
                                feedType = typeName,
                                brand = brand,
                                initialBags = initialBagsStr.toFloatOrNull() ?: 0f,
                                costPerBag = costPerBagStr.toDoubleOrNull() ?: 0.0,
                                reorderThreshold = reorderStr.toFloatOrNull() ?: 5f
                            )
                            showAddTypeDialog = false
                        }
                    }
                ) {
                    Text("Save Variety")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTypeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun FeedStockCard(item: FeedInventoryEntity, currency: String) {
    val isLow = item.bagsInStock <= item.reorderThresholdBags

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = item.feedType,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${item.brand.ifBlank { "Standard Brand" }} • ${item.bagWeightKg.toInt()}kg bag",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isLow) SyncOrange.copy(alpha = 0.15f) else SyncGreen.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isLow) "LOW STOCK" else "IN STOCK",
                        color = if (isLow) SyncOrange else SyncGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "${String.format(Locale.US, "%.1f", item.bagsInStock)} bags",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isLow) SyncOrange else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (item.costPerBag > 0) "Ref: $currency${item.costPerBag.toInt()}/bag" else "Reorder at ${item.reorderThresholdBags.toInt()}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LinearProgressIndicator(
                progress = { (item.bagsInStock / 30f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (isLow) SyncOrange else FarmGreenPrimary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
fun FeedLogRow(log: FeedLogEntity, currency: String, onDelete: () -> Unit = {}) {
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
                        if (log.logType == "CONSUMPTION") Color(0xFF8D6E63).copy(alpha = 0.15f)
                        else FarmGreenPrimary.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (log.logType == "CONSUMPTION") Icons.Default.Restaurant else Icons.Default.LocalShipping,
                    contentDescription = null,
                    tint = if (log.logType == "CONSUMPTION") Color(0xFF8D6E63) else FarmGreenPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${if (log.logType == "CONSUMPTION") "Fed" else "Restocked"} ${log.bagsQuantity} bags",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = "${log.feedTypeName} • ${log.flockName.ifBlank { log.supplierName }}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = log.dateString,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (log.logType == "PURCHASE" && log.totalCost > 0) {
                    Text(
                        text = "$currency${log.totalCost.toInt()}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = FarmGreenPrimary
                    )
                }
            }
        }
    }
}
