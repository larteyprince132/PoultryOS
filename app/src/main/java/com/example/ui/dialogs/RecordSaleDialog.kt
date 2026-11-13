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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomerEntity
import com.example.ui.components.NumberStepper
import com.example.ui.theme.EggGold
import com.example.ui.theme.FarmGreenPrimary
import com.example.ui.theme.SyncOrange
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecordSaleDialog(
    customers: List<CustomerEntity>,
    flocks: List<com.example.data.model.FlockEntity> = emptyList(),
    availableEggCrates: Float = 0f,
    currencySymbol: String = "GH₵",
    defaultEggPrice: Double = 35.0,
    onDismiss: () -> Unit,
    onSave: (
        dateString: String,
        itemType: String,
        quantity: Float,
        unitPrice: Double,
        amountPaid: Double,
        customerName: String,
        customerPhone: String,
        customerId: String?,
        notes: String,
        targetFlockId: String?
    ) -> Unit
) {
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    var selectedItemType by remember { mutableStateOf("EGG_CRATE") } // EGG_CRATE, LIVE_BIRD, MANURE, OTHER

    var quantity by remember { mutableFloatStateOf(5f) }
    var unitPrice by remember { mutableDoubleStateOf(defaultEggPrice) }
    var amountPaidStr by remember { mutableStateOf("") }

    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var selectedCustomerId by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf("") }
    var selectedFlockId by remember { mutableStateOf(flocks.firstOrNull()?.id) }

    val totalAmount = quantity.toDouble() * unitPrice
    val enteredPaid = amountPaidStr.toDoubleOrNull() ?: totalAmount

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Record Sale",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Item category
                Text(
                    text = "Item to Sell",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedItemType == "EGG_CRATE",
                        onClick = {
                            selectedItemType = "EGG_CRATE"
                            unitPrice = defaultEggPrice
                        },
                        label = { Text("Eggs (Crates)") }
                    )
                    FilterChip(
                        selected = selectedItemType == "LIVE_BIRD",
                        onClick = {
                            selectedItemType = "LIVE_BIRD"
                            unitPrice = 80.0
                        },
                        label = { Text("Live Birds") }
                    )
                    FilterChip(
                        selected = selectedItemType == "MANURE",
                        onClick = {
                            selectedItemType = "MANURE"
                            unitPrice = 25.0
                        },
                        label = { Text("Manure") }
                    )
                }

                // Quantity Stepper
                NumberStepper(
                    value = quantity.toInt(),
                    onValueChange = { quantity = it.toFloat() },
                    label = if (selectedItemType == "EGG_CRATE") "Number of Crates" else "Quantity",
                    unit = if (selectedItemType == "EGG_CRATE") "crates" else "units",
                    min = 1,
                    max = 500
                )

                // Unit Price
                OutlinedTextField(
                    value = if (unitPrice == 0.0) "" else unitPrice.toString(),
                    onValueChange = { unitPrice = it.toDoubleOrNull() ?: 0.0 },
                    label = { Text("Price per unit (GH₵)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                // Total Calculation Display
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = FarmGreenPrimary.copy(alpha = 0.08f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Sale Amount:",
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "GH₵${String.format(Locale.US, "%.2f", totalAmount)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = FarmGreenPrimary
                        )
                    }
                }

                // Payment Status quick buttons
                Text(
                    text = "Payment Status",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = amountPaidStr.isEmpty() || enteredPaid >= totalAmount,
                        onClick = { amountPaidStr = totalAmount.toString() },
                        label = { Text("Paid Full") }
                    )
                    FilterChip(
                        selected = amountPaidStr == "0",
                        onClick = { amountPaidStr = "0" },
                        label = { Text("Unpaid / Credit") }
                    )
                    FilterChip(
                        selected = enteredPaid > 0.0 && enteredPaid < totalAmount,
                        onClick = { amountPaidStr = (totalAmount / 2).toString() },
                        label = { Text("Deposit / Partial") }
                    )
                }

                OutlinedTextField(
                    value = amountPaidStr,
                    onValueChange = { amountPaidStr = it },
                    label = { Text("Amount Paid in GH₵ (leave blank for full)") },
                    placeholder = { Text("GH₵${String.format(Locale.US, "%.2f", totalAmount)}") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                if (enteredPaid < totalAmount) {
                    Text(
                        text = "Remaining Debt: GH₵${String.format(Locale.US, "%.2f", totalAmount - enteredPaid)}",
                        color = SyncOrange,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }

                // Customer info
                OutlinedTextField(
                    value = customerName,
                    onValueChange = {
                        customerName = it
                        selectedCustomerId = null
                    },
                    label = { Text("Customer Name (optional)") },
                    placeholder = { Text("Walk-in Customer / Mama Akosua") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = customerPhone,
                    onValueChange = { customerPhone = it },
                    label = { Text("Customer Phone (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalPaid = if (amountPaidStr.isBlank()) totalAmount else (amountPaidStr.toDoubleOrNull() ?: totalAmount)
                    onSave(
                        today,
                        selectedItemType,
                        quantity,
                        unitPrice,
                        finalPaid,
                        customerName,
                        customerPhone,
                        selectedCustomerId,
                        notes,
                        if (selectedItemType == "LIVE_BIRD") selectedFlockId else null
                    )
                },
                modifier = Modifier.testTag("save_sale_button")
            ) {
                Text("Save Sale")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
