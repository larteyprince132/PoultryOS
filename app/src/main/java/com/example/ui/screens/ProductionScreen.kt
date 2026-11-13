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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.EggProductionEntity
import com.example.ui.dialogs.RecordEggDialog
import com.example.ui.theme.EggGold
import com.example.ui.theme.EggGoldLight
import com.example.ui.theme.FarmGreenContainer
import com.example.ui.theme.FarmGreenDark
import com.example.ui.theme.FarmGreenPrimary
import com.example.ui.theme.SyncGreen
import com.example.ui.theme.SyncOrange
import com.example.ui.viewmodel.PoultryViewModel
import java.util.Locale

@Composable
fun ProductionScreen(
    viewModel: PoultryViewModel,
    modifier: Modifier = Modifier
) {
    val productions by viewModel.eggProductions.collectAsStateWithLifecycle()
    val flocks by viewModel.flocks.collectAsStateWithLifecycle()
    val metrics by viewModel.dashboardMetrics.collectAsStateWithLifecycle()
    val activeFarm by viewModel.activeFarm.collectAsStateWithLifecycle()

    var showDialog by remember { mutableStateOf(false) }
    var recordToDelete by remember { mutableStateOf<EggProductionEntity?>(null) }
    var recordToEdit by remember { mutableStateOf<EggProductionEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Record Collection") },
                containerColor = EggGold,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("record_egg_collection_fab")
            )
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("production_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Unsold Inventory & Production Performance
            item {
                Text(
                    text = "Egg Production & Inventory",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Monitor crate harvest, laying rate, and unsold stock",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Unsold Egg Inventory Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = FarmGreenContainer.copy(alpha = 0.5f)),
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
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(FarmGreenPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AVAILABLE FOR SALE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = FarmGreenDark
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.1f", metrics.unsoldEggCratesInventory)} Crates in Stock",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = FarmGreenDark
                            )
                            Text(
                                text = "Total production minus crates sold and damaged",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Production History Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Collection History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${productions.size} entries",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (productions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Egg,
                                contentDescription = null,
                                tint = EggGold,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Egg Collections Recorded Yet",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap 'Record Collection' below to log morning or afternoon crates collected.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(productions, key = { it.id }) { record ->
                    ProductionCard(
                        record = record,
                        onEdit = { recordToEdit = record },
                        onDelete = { recordToDelete = record }
                    )
                }
            }
        }
    }

    recordToDelete?.let { record ->
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = { Text("Delete Collection Record", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this collection of " + record.cratesCollected + " crates from " + record.dateString + "? This will restore unsold egg inventory calculations.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteEggProduction(record)
                        recordToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = SyncOrange)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    recordToEdit?.let { record ->
        var editCrates by remember { androidx.compose.runtime.mutableIntStateOf(record.cratesCollected) }
        var editLoose by remember { androidx.compose.runtime.mutableIntStateOf(record.looseEggsCollected) }
        var editDamaged by remember { androidx.compose.runtime.mutableIntStateOf(record.brokenDamagedEggs) }
        var editNotes by remember { mutableStateOf(record.notes) }

        AlertDialog(
            onDismissRequest = { recordToEdit = null },
            title = { Text("Edit Egg Collection", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(record.flockName + " • " + record.period + " • " + record.dateString, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    com.example.ui.components.NumberStepper(
                        value = editCrates,
                        onValueChange = { editCrates = it },
                        label = "Crates Collected",
                        unit = "crates",
                        min = 0,
                        max = 5000
                    )
                    com.example.ui.components.NumberStepper(
                        value = editLoose,
                        onValueChange = { editLoose = it },
                        label = "Loose Eggs",
                        unit = "eggs",
                        min = 0,
                        max = 29
                    )
                    com.example.ui.components.NumberStepper(
                        value = editDamaged,
                        onValueChange = { editDamaged = it },
                        label = "Damaged / Broken",
                        unit = "eggs",
                        min = 0,
                        max = 500
                    )
                    androidx.compose.material3.OutlinedTextField(
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
                        val total = (editCrates * 30) + editLoose
                        val flock = flocks.find { it.id == record.flockId }
                        val birds = flock?.currentCount ?: 0
                        val layingRate = if (birds > 0) (total.toFloat() / birds.toFloat()) * 100f else 0f
                        viewModel.updateEggProduction(
                            record.copy(
                                cratesCollected = editCrates,
                                looseEggsCollected = editLoose,
                                brokenDamagedEggs = editDamaged,
                                totalEggs = total,
                                layingPercentage = layingRate,
                                notes = editNotes
                            )
                        )
                        recordToEdit = null
                    }
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDialog) {
        RecordEggDialog(
            flocks = flocks,
            onDismiss = { showDialog = false },
            onSave = { dateStr, period, flockId, flockName, crates, loose, broken, notes ->
                viewModel.recordEggCollection(dateStr, period, flockId, flockName, crates, loose, broken, notes)
                showDialog = false
            }
        )
    }
}

@Composable
fun ProductionCard(record: EggProductionEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(EggGoldLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Egg,
                            contentDescription = null,
                            tint = EggGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "${record.cratesCollected} Crates (${record.period})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${record.dateString} • ${record.flockName}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${String.format(Locale.US, "%.1f", record.layingPercentage)}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (record.layingPercentage >= 70f) FarmGreenPrimary else EggGold
                    )
                    Text(
                        text = "laying rate",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Loose: ${record.looseEggsCollected} eggs",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Damaged: ${record.brokenDamagedEggs} eggs",
                    fontSize = 12.sp,
                    color = if (record.brokenDamagedEggs > 0) SyncOrange else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Total: ${record.totalEggs} eggs",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (record.notes.isNotBlank()) {
                Text(
                    text = "Note: ${record.notes}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (record.syncStatus.name == "SYNCED") SyncGreen else SyncOrange)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (record.syncStatus.name == "SYNCED") "Synced" else "Local (Pending)",
                        fontSize = 10.sp,
                        color = if (record.syncStatus.name == "SYNCED") SyncGreen else SyncOrange,
                        fontWeight = FontWeight.Medium
                    )
                }
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit record", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete record", modifier = Modifier.size(16.dp), tint = SyncOrange)
                    }
                }
            }
        }
    }
}
