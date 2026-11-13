package com.example.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecordExpenseDialog(
    onDismiss: () -> Unit,
    onSave: (
        dateString: String,
        category: String,
        amount: Double,
        paymentMethod: String,
        description: String,
        recipient: String
    ) -> Unit
) {
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    var category by remember { mutableStateOf("LABOUR") }
    var amountStr by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("MOMO") } // MoMo, Cash, Bank
    var description by remember { mutableStateOf("") }
    var recipient by remember { mutableStateOf("") }

    val categories = listOf(
        "LABOUR" to "Labour / Farm Hand",
        "FEED" to "Feed",
        "MEDICATION" to "Medication",
        "VACCINATION" to "Vaccines",
        "UTILITIES" to "Electricity & Water",
        "TRANSPORT" to "Transport",
        "EQUIPMENT" to "Equipment",
        "REPAIRS" to "Repairs",
        "OTHER" to "Other"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Record Farm Expense",
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
                // Category
                Text(
                    text = "Expense Category",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    categories.chunked(3).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            row.forEach { (catKey, catLabel) ->
                                FilterChip(
                                    selected = category == catKey,
                                    onClick = { category = catKey },
                                    label = { Text(catLabel, maxLines = 1) }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount (GH₵)") },
                    placeholder = { Text("e.g. 150.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("expense_amount_input")
                )

                // Payment method
                Text(
                    text = "Payment Method",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("MOMO" to "MTN / Vodafone MoMo", "CASH" to "Cash", "BANK" to "Bank").forEach { (methodKey, label) ->
                        FilterChip(
                            selected = paymentMethod == methodKey,
                            onClick = { paymentMethod = methodKey },
                            label = { Text(label) }
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / What was this for?") },
                    placeholder = { Text("e.g. Bi-weekly wages for farm worker, coop bulb replacement") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = recipient,
                    onValueChange = { recipient = it },
                    label = { Text("Paid to (Person or Shop)") },
                    placeholder = { Text("e.g. Yaw, Kwasi Electricals") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    if (amount > 0) {
                        onSave(today, category, amount, paymentMethod, description, recipient)
                    }
                },
                enabled = (amountStr.toDoubleOrNull() ?: 0.0) > 0.0,
                modifier = Modifier.testTag("save_expense_button")
            ) {
                Text("Save Expense")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
