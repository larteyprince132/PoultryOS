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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ExpenseEntity
import com.example.data.model.SaleEntity
import com.example.ui.dialogs.RecordExpenseDialog
import com.example.ui.dialogs.RecordSaleDialog
import com.example.ui.theme.EggGold
import com.example.ui.theme.FarmGreenContainer
import com.example.ui.theme.FarmGreenDark
import com.example.ui.theme.FarmGreenPrimary
import com.example.ui.theme.SyncGreen
import com.example.ui.theme.SyncOrange
import com.example.ui.theme.SyncRed
import com.example.ui.viewmodel.PoultryViewModel
import java.util.Locale

@Composable
fun FinanceScreen(
    viewModel: PoultryViewModel,
    modifier: Modifier = Modifier
) {
    val sales by viewModel.sales.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val metrics by viewModel.dashboardMetrics.collectAsStateWithLifecycle()
    val activeFarm by viewModel.activeFarm.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Sales, 1 = Expenses, 2 = Summary
    var showSaleDialog by remember { mutableStateOf(false) }
    var showExpenseDialog by remember { mutableStateOf(false) }
    var saleToDelete by remember { mutableStateOf<SaleEntity?>(null) }
    var expenseToDelete by remember { mutableStateOf<ExpenseEntity?>(null) }

    val currency = activeFarm?.currencySymbol ?: "GH₵"
    val totalRevenue = sales.sumOf { it.totalAmount }
    val totalCashReceived = sales.sumOf { it.amountPaid }
    val totalOutstanding = sales.sumOf { it.totalAmount - it.amountPaid }
    val totalExpensesAmount = expenses.sumOf { it.amount }
    val operatingProfit = totalRevenue - totalExpensesAmount
    val cashOnHandProfit = totalCashReceived - totalExpensesAmount

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (selectedTab == 1) showExpenseDialog = true else showSaleDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(if (selectedTab == 1) "Record Expense" else "Record Sale") },
                containerColor = if (selectedTab == 1) Color(0xFFD32F2F) else FarmGreenPrimary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("finance_action_fab")
            )
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("finance_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Financial Management",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Sales ledger, expense records, cash flow, and operating profit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Top Financial Health Summary Card (Ghana Farm Context)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = FarmGreenContainer.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "OPERATING RESULT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FarmGreenDark
                                )
                                Text(
                                    text = "GH₵${String.format(Locale.US, "%,.2f", operatingProfit)}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (operatingProfit >= 0) FarmGreenDark else SyncRed
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (operatingProfit >= 0) SyncGreen else SyncRed)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (operatingProfit >= 0) "PROFITABLE" else "DEFICIT",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Cash Collected vs Outstanding Credit breakdown (Crucial Ghana farm requirement)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Total Revenue", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "GH₵${String.format(Locale.US, "%,.0f", totalRevenue)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Column {
                                Text(text = "Cash Received", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "GH₵${String.format(Locale.US, "%,.0f", totalCashReceived)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = SyncGreen
                                )
                            }
                            Column {
                                Text(text = "Unpaid Credit", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "GH₵${String.format(Locale.US, "%,.0f", totalOutstanding)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (totalOutstanding > 0) SyncOrange else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column {
                                Text(text = "Total Expenses", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "GH₵${String.format(Locale.US, "%,.0f", totalExpensesAmount)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = SyncRed
                                )
                            }
                        }
                    }
                }
            }

            // Tab Navigation: [ Sales ] [ Expenses ] [ Breakdown ]
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Sales (${sales.size})") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Expenses (${expenses.size})") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Summary") }
                    )
                }
            }

            // Tab 0: Sales List
            if (selectedTab == 0) {
                if (sales.isEmpty()) {
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
                                Text("No Sales Recorded Yet", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Tap 'Record Sale' below to log egg crates, live birds, or manure sales.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(sales, key = { it.id }) { sale ->
                        SaleRowCard(
                            sale = sale,
                            currency = currency,
                            onMarkPaid = { viewModel.markSalePaid(sale.id) },
                            onDelete = { saleToDelete = sale }
                        )
                    }
                }
            }

            // Tab 1: Expenses List
            if (selectedTab == 1) {
                if (expenses.isEmpty()) {
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
                                Text("No Expenses Recorded Yet", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Tap 'Record Expense' below to log feed, worker wages, medication, or utilities.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(expenses, key = { it.id }) { expense ->
                        ExpenseRowCard(
                            expense = expense,
                            currency = currency,
                            onDelete = { expenseToDelete = expense }
                        )
                    }
                }
            }

            // Tab 2: Expense Breakdown & Financial Categories
            if (selectedTab == 2) {
                item {
                    FinancialBreakdownCard(expenses = expenses, sales = sales)
                }
            }
        }
    }

    if (showSaleDialog) {
        RecordSaleDialog(
            customers = customers,
            flocks = viewModel.flocks.value,
            availableEggCrates = metrics.unsoldEggCratesInventory,
            currencySymbol = currency,
            defaultEggPrice = activeFarm?.defaultEggCratePrice ?: 35.0,
            onDismiss = { showSaleDialog = false },
            onSave = { dateStr, itemType, quantity, unitPrice, amountPaid, customerName, customerPhone, customerId, notes, targetFlockId ->
                viewModel.recordSale(dateStr, itemType, quantity, unitPrice, amountPaid, customerName, customerPhone, customerId, notes, targetFlockId)
                showSaleDialog = false
            }
        )
    }

    saleToDelete?.let { sale ->
        AlertDialog(
            onDismissRequest = { saleToDelete = null },
            title = { Text("Delete Sale Record", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this sale of " + sale.quantity + " " + sale.itemType.replace("_", " ") + " for " + currency + sale.totalAmount + "? Any outstanding customer balance from this sale will be reversed.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSale(sale)
                        saleToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = SyncOrange)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { saleToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    expenseToDelete?.let { exp ->
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text("Delete Expense Record", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this expense of " + currency + exp.amount + " (" + exp.category + ")?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteExpense(exp)
                        expenseToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = SyncOrange)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { expenseToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showExpenseDialog) {
        RecordExpenseDialog(
            onDismiss = { showExpenseDialog = false },
            onSave = { dateStr, category, amount, paymentMethod, description, recipient ->
                viewModel.recordExpense(dateStr, category, amount, paymentMethod, description, recipient)
                showExpenseDialog = false
            }
        )
    }
}

@Composable
fun SaleRowCard(
    sale: SaleEntity,
    currency: String,
    onMarkPaid: () -> Unit,
    onDelete: () -> Unit
) {
    val isUnpaid = sale.paymentStatus != "PAID"

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
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(FarmGreenPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = FarmGreenPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "${sale.quantity.toInt()} ${sale.itemType.replace("_", " ")}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${sale.dateString} • ${sale.customerName}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "GH₵${String.format(Locale.US, "%.2f", sale.totalAmount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = FarmGreenPrimary
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when (sale.paymentStatus) {
                                    "PAID" -> SyncGreen.copy(alpha = 0.15f)
                                    "PARTIAL" -> SyncOrange.copy(alpha = 0.15f)
                                    else -> SyncRed.copy(alpha = 0.15f)
                                }
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = sale.paymentStatus,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (sale.paymentStatus) {
                                "PAID" -> SyncGreen
                                "PARTIAL" -> SyncOrange
                                else -> SyncRed
                            }
                        )
                    }
                }
            }

            if (isUnpaid) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SyncOrange.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Paid: GH₵${sale.amountPaid.toInt()} • Due: GH₵${(sale.totalAmount - sale.amountPaid).toInt()}",
                        fontSize = 12.sp,
                        color = SyncOrange,
                        fontWeight = FontWeight.SemiBold
                    )
                    Button(
                        onClick = onMarkPaid,
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                    ) {
                        Text("Mark Paid", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseRowCard(expense: ExpenseEntity, currency: String = "GH₵", onDelete: () -> Unit = {}) {
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD32F2F).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingDown,
                    contentDescription = null,
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.category,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${expense.description.ifBlank { "Farm Expense" }} [${expense.paymentMethod}]",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (expense.recipient.isNotBlank()) {
                    Text(
                        text = "To: ${expense.recipient}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "-GH₵${String.format(Locale.US, "%.2f", expense.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFD32F2F)
                )
                Text(
                    text = expense.dateString,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun FinancialBreakdownCard(
    expenses: List<ExpenseEntity>,
    sales: List<SaleEntity>
) {
    val expenseByCategory = expenses.groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Expense Breakdown by Category",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (expenseByCategory.isEmpty()) {
                Text(
                    text = "No expenses logged to breakdown.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                expenseByCategory.forEach { (cat, sum) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = cat, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "GH₵${String.format(Locale.US, "%.2f", sum)}",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F)
                        )
                    }
                }
            }
        }
    }
}
