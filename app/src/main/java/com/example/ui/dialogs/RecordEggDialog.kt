package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FlockEntity
import com.example.ui.components.NumberStepper
import com.example.ui.theme.EggGold
import com.example.ui.theme.FarmGreenPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecordEggDialog(
    flocks: List<FlockEntity>,
    onDismiss: () -> Unit,
    onSave: (
        dateString: String,
        period: String,
        flockId: String,
        flockName: String,
        crates: Int,
        looseEggs: Int,
        brokenEggs: Int,
        notes: String
    ) -> Unit
) {
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    var selectedDate by remember { mutableStateOf(today) }
    var selectedPeriod by remember { mutableStateOf("Morning") }

    val defaultFlock = flocks.firstOrNull { it.isLayingFlock } ?: flocks.firstOrNull()
    var selectedFlock by remember { mutableStateOf(defaultFlock) }

    var crates by remember { mutableIntStateOf(6) } // Father's farm approx benchmark
    var looseEggs by remember { mutableIntStateOf(0) }
    var brokenEggs by remember { mutableIntStateOf(0) }
    var notes by remember { mutableStateOf("") }

    val totalEggs = (crates * 30) + looseEggs
    val activeHens = selectedFlock?.currentCount ?: 250
    val layingPct = if (activeHens > 0) (totalEggs.toFloat() / activeHens.toFloat()) * 100f else 0f

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(EggGold.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Egg,
                        contentDescription = null,
                        tint = EggGold,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Record Egg Collection",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "30 eggs per crate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Period selection
                Column {
                    Text(
                        text = "Collection Period",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Morning", "Afternoon", "Full Day").forEach { period ->
                            FilterChip(
                                selected = selectedPeriod == period,
                                onClick = { selectedPeriod = period },
                                label = { Text(period) }
                            )
                        }
                    }
                }

                // Flock choice
                if (flocks.size > 1) {
                    Column {
                        Text(
                            text = "Laying Flock",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            flocks.filter { it.isLayingFlock || it.name.contains("Layer", ignoreCase = true) }.forEach { flock ->
                                FilterChip(
                                    selected = selectedFlock?.id == flock.id,
                                    onClick = { selectedFlock = flock },
                                    label = { Text("${flock.name} (${flock.currentCount} birds)") }
                                )
                            }
                        }
                    }
                }

                // Stepper for Crates (Core prompt requirement)
                NumberStepper(
                    value = crates,
                    onValueChange = { crates = it },
                    label = "How many crates did you collect?",
                    unit = "crates",
                    min = 0,
                    max = 200,
                    modifier = Modifier.testTag("stepper_egg_crates")
                )

                // Extra loose eggs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NumberStepper(
                        value = looseEggs,
                        onValueChange = { looseEggs = it },
                        label = "Loose eggs",
                        unit = "eggs",
                        min = 0,
                        max = 29,
                        modifier = Modifier.weight(1f)
                    )
                    NumberStepper(
                        value = brokenEggs,
                        onValueChange = { brokenEggs = it },
                        label = "Damaged / Broken",
                        unit = "eggs",
                        min = 0,
                        max = 100,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Calculated Laying Rate Preview
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = FarmGreenPrimary.copy(alpha = 0.08f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total Harvest:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "$crates crates + $looseEggs eggs ($totalEggs total)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = FarmGreenPrimary
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Estimated Laying Rate:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.1f", layingPct)}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (layingPct >= 70f) FarmGreenPrimary else EggGold
                            )
                        }
                        Text(
                            text = "Based on $activeHens active laying hens",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    placeholder = { Text("e.g. Good egg size, shell quality") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val flock = selectedFlock ?: flocks.firstOrNull()
                    onSave(
                        selectedDate,
                        selectedPeriod,
                        flock?.id ?: "flock_layers",
                        flock?.name ?: "White Layers",
                        crates,
                        looseEggs,
                        brokenEggs,
                        notes
                    )
                },
                modifier = Modifier.testTag("save_egg_record_button")
            ) {
                Text("Save Record")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
