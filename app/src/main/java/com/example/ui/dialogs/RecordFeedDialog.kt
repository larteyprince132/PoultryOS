package com.example.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FeedInventoryEntity
import com.example.data.model.FlockEntity
import com.example.ui.components.NumberStepper
import com.example.ui.theme.FarmGreenPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun RecordFeedDialog(
    feedInventories: List<FeedInventoryEntity>,
    flocks: List<FlockEntity>,
    defaultBagPrice: Double = 0.0,
    currencySymbol: String = "GH₵",
    onDismiss: () -> Unit,
    onSaveConsumption: (
        dateString: String,
        feedTypeId: String,
        flockId: String,
        flockName: String,
        bagsQuantity: Float,
        notes: String
    ) -> Unit,
    onSavePurchase: (
        dateString: String,
        feedTypeId: String,
        bagsQuantity: Float,
        costPerBag: Double,
        supplierName: String,
        recordAsExpense: Boolean,
        notes: String
    ) -> Unit
) {
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    var selectedTab by remember { mutableIntStateOf(if (feedInventories.isEmpty()) 1 else 0) }

    var selectedFeed by remember { mutableStateOf(feedInventories.firstOrNull()) }
    var customFeedName by remember { mutableStateOf(if (feedInventories.isEmpty()) "Layers Mash" else "") }
    var selectedFlock by remember { mutableStateOf(flocks.firstOrNull()) }

    // Consumption fields
    var bagsFed by remember { mutableFloatStateOf(1f) }
    var consumptionNotes by remember { mutableStateOf("") }

    // Purchase fields
    var bagsPurchased by remember { mutableFloatStateOf(5f) }
    var costPerBag by remember { mutableDoubleStateOf(selectedFeed?.costPerBag.takeIf { it != null && it > 0.0 } ?: defaultBagPrice) }
    var supplierName by remember { mutableStateOf("") }
    var recordAsExpense by remember { mutableStateOf(true) }
    var purchaseNotes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = if (selectedTab == 0) "Record Daily Feeding" else "Record Feed Purchase / Restock",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Feed Birds") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Buy Feed") }
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Feed Type selection
                Text(
                    text = "Feed Type",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )

                if (feedInventories.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        feedInventories.forEach { inv ->
                            FilterChip(
                                selected = selectedFeed?.id == inv.id,
                                onClick = {
                                    selectedFeed = inv
                                    costPerBag = inv.costPerBag
                                },
                                label = { Text("${inv.feedType} (${inv.bagsInStock.toInt()} in stock)") }
                            )
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = customFeedName,
                        onValueChange = { customFeedName = it },
                        label = { Text("Feed Type Name") },
                        placeholder = { Text("e.g. Layers Mash, Broiler Finisher") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (selectedTab == 0) {
                    // Feeding Consumption
                    Text(
                        text = "Flock Fed",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (flocks.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            flocks.forEach { fl ->
                                FilterChip(
                                    selected = selectedFlock?.id == fl.id,
                                    onClick = { selectedFlock = fl },
                                    label = { Text(fl.name) }
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "No flocks registered yet. Birds will be marked as 'Main Flock'.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    NumberStepper(
                        value = bagsFed.toInt(),
                        onValueChange = { bagsFed = it.toFloat() },
                        label = "Bags Fed Today",
                        unit = "bags",
                        min = 1,
                        max = 50
                    )

                    OutlinedTextField(
                        value = consumptionNotes,
                        onValueChange = { consumptionNotes = it },
                        label = { Text("Feeding notes (optional)") },
                        placeholder = { Text("e.g. Morning 0.5 bag, Evening 0.5 bag") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // Feed Purchase
                    NumberStepper(
                        value = bagsPurchased.toInt(),
                        onValueChange = { bagsPurchased = it.toFloat() },
                        label = "Bags Bought",
                        unit = "bags",
                        min = 1,
                        max = 500
                    )

                    OutlinedTextField(
                        value = if (costPerBag == 0.0) "" else costPerBag.toString(),
                        onValueChange = { costPerBag = it.toDoubleOrNull() ?: 0.0 },
                        label = { Text("Cost per bag ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    val totalCost = bagsPurchased.toDouble() * costPerBag
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = FarmGreenPrimary.copy(alpha = 0.08f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total Purchase Amount:", fontWeight = FontWeight.Medium)
                            Text(
                                "$currencySymbol${String.format(Locale.US, "%.2f", totalCost)}",
                                fontWeight = FontWeight.Bold,
                                color = FarmGreenPrimary
                            )
                        }
                    }

                    OutlinedTextField(
                        value = supplierName,
                        onValueChange = { supplierName = it },
                        label = { Text("Supplier / Store Name") },
                        placeholder = { Text("e.g. Local Feed Mill") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = recordAsExpense,
                            onCheckedChange = { recordAsExpense = it }
                        )
                        Text(
                            text = "Record automatically as Farm Expense",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val feedId = selectedFeed?.id ?: UUID.randomUUID().toString()
                    if (selectedTab == 0) {
                        val flock = selectedFlock ?: flocks.firstOrNull()
                        onSaveConsumption(
                            today,
                            feedId,
                            flock?.id ?: "",
                            flock?.name ?: "All Birds",
                            bagsFed,
                            consumptionNotes
                        )
                    } else {
                        onSavePurchase(
                            today,
                            feedId,
                            bagsPurchased,
                            costPerBag,
                            supplierName,
                            recordAsExpense,
                            purchaseNotes
                        )
                    }
                },
                modifier = Modifier.testTag("save_feed_button")
            ) {
                Text(if (selectedTab == 0) "Save Feeding" else "Save Purchase")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
