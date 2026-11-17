package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.PoultryDatabase
import com.example.data.model.CustomerEntity
import com.example.data.model.EggProductionEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.FarmEntity
import com.example.data.model.FeedInventoryEntity
import com.example.data.model.FeedLogEntity
import com.example.data.model.FlockEntity
import com.example.data.model.HealthLogEntity
import com.example.data.model.SaleEntity
import com.example.data.model.SyncAuditEntity
import com.example.data.model.UserEntity
import com.example.data.repository.PoultryRepository
import com.example.data.session.UserSessionManager
import com.example.data.sync.NetworkMonitor
import com.example.data.sync.SyncManager
import com.example.data.sync.SyncUIState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FarmAlert(
    val title: String,
    val description: String,
    val severity: String = "WARNING"
)

data class FarmActivityItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val timeLabel: String,
    val category: String,
    val isSynced: Boolean
)

data class DashboardMetrics(
    val totalBirds: Int = 0,
    val layingHens: Int = 0,
    val todayCrates: Int = 0,
    val todayTotalEggs: Int = 0,
    val todayLayingRate: Float = 0f,
    val unsoldEggCratesInventory: Float = 0f,
    val totalFeedBags: Float = 0f,
    val estimatedFeedDaysLeft: Int = 0,
    val todayRevenue: Double = 0.0,
    val todayExpenses: Double = 0.0,
    val todayNet: Double = 0.0,
    val totalOutstandingCredit: Double = 0.0,
    val alerts: List<FarmAlert> = emptyList(),
    val recentActivities: List<FarmActivityItem> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class PoultryViewModel(application: Application) : AndroidViewModel(application) {
    private val database = PoultryDatabase.getDatabase(application)
    val sessionManager = UserSessionManager(database, application, viewModelScope)

    val currentUser: StateFlow<UserEntity?> = sessionManager.currentUser
    val activeFarm: StateFlow<FarmEntity?> = sessionManager.activeFarm
    val isSessionLoaded: StateFlow<Boolean> = sessionManager.isSessionLoaded

    private val networkMonitor = NetworkMonitor(application)
    val syncManager = SyncManager(
        database = database,
        networkMonitor = networkMonitor,
        scope = viewModelScope,
        activeUserProvider = { currentUser.value?.id },
        activeFarmProvider = { activeFarm.value?.id }
    )
    private val repository = PoultryRepository(database, syncManager)

    val syncState: StateFlow<SyncUIState> = syncManager.syncState

    val userFarms: StateFlow<List<FarmEntity>> = currentUser.flatMapLatest { user ->
        if (user != null) repository.getFarmsForUser(user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Farm Scoped Data
    val flocks: StateFlow<List<FlockEntity>> = combine(activeFarm, currentUser) { farm, user ->
        Pair(farm, user)
    }.flatMapLatest { (farm, user) ->
        if (farm != null && user != null) repository.getAllFlocks(farm.id, user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val eggProductions: StateFlow<List<EggProductionEntity>> = combine(activeFarm, currentUser) { farm, user ->
        Pair(farm, user)
    }.flatMapLatest { (farm, user) ->
        if (farm != null && user != null) repository.getAllProductions(farm.id, user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val feedInventories: StateFlow<List<FeedInventoryEntity>> = combine(activeFarm, currentUser) { farm, user ->
        Pair(farm, user)
    }.flatMapLatest { (farm, user) ->
        if (farm != null && user != null) repository.getAllFeedInventories(farm.id, user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val feedLogs: StateFlow<List<FeedLogEntity>> = combine(activeFarm, currentUser) { farm, user ->
        Pair(farm, user)
    }.flatMapLatest { (farm, user) ->
        if (farm != null && user != null) repository.getAllFeedLogs(farm.id, user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sales: StateFlow<List<SaleEntity>> = combine(activeFarm, currentUser) { farm, user ->
        Pair(farm, user)
    }.flatMapLatest { (farm, user) ->
        if (farm != null && user != null) repository.getAllSales(farm.id, user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses: StateFlow<List<ExpenseEntity>> = combine(activeFarm, currentUser) { farm, user ->
        Pair(farm, user)
    }.flatMapLatest { (farm, user) ->
        if (farm != null && user != null) repository.getAllExpenses(farm.id, user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<CustomerEntity>> = combine(activeFarm, currentUser) { farm, user ->
        Pair(farm, user)
    }.flatMapLatest { (farm, user) ->
        if (farm != null && user != null) repository.getAllCustomers(farm.id, user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val healthLogs: StateFlow<List<HealthLogEntity>> = combine(activeFarm, currentUser) { farm, user ->
        Pair(farm, user)
    }.flatMapLatest { (farm, user) ->
        if (farm != null && user != null) repository.getAllHealthLogs(farm.id, user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syncAudits: StateFlow<List<SyncAuditEntity>> = combine(activeFarm, currentUser) { farm, user ->
        Pair(farm, user)
    }.flatMapLatest { (farm, user) ->
        if (farm != null && user != null) repository.getRecentAudits(farm.id, user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 30-Second Cockpit Dashboard Combined Flow
    val dashboardMetrics: StateFlow<DashboardMetrics> = combine(
        flocks,
        eggProductions,
        feedInventories,
        sales,
        expenses
    ) { flockList, productions, inventories, salesList, expenseList ->
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val totalBirds = flockList.sumOf { it.currentCount }
        val layingHens = flockList.filter { it.isLayingFlock }.sumOf { it.currentCount }

        val todayProductions = productions.filter { it.dateString == todayStr }
        val todayCrates = todayProductions.sumOf { it.cratesCollected }
        val todayTotalEggs = todayProductions.sumOf { it.totalEggs }
        val todayLayingRate = if (layingHens > 0) (todayTotalEggs.toFloat() / layingHens.toFloat()) * 100f else 0f

        val totalEggsProduced = productions.sumOf { it.totalEggs }
        val totalDamagedEggs = productions.sumOf { it.brokenDamagedEggs }
        val totalEggCratesSold = salesList.filter { it.itemType == "EGG_CRATE" }.sumOf { it.quantity.toDouble() }
        val totalEggsSold = (totalEggCratesSold * 30).toInt()
        val netUnsoldEggs = maxOf(0, totalEggsProduced - totalDamagedEggs - totalEggsSold)
        val unsoldCrates = netUnsoldEggs.toFloat() / 30f

        val totalFeedBags = inventories.sumOf { it.bagsInStock.toDouble() }.toFloat()
        val dailyFeedBurnEstimate = if (layingHens > 0) (layingHens * 0.125f) / 50f else 0.5f
        val feedDaysLeft = if (dailyFeedBurnEstimate > 0f) (totalFeedBags / dailyFeedBurnEstimate).toInt() else 0

        val todaySales = salesList.filter { it.dateString == todayStr }
        val todayRev = todaySales.sumOf { it.totalAmount }
        val todayExp = expenseList.filter { it.dateString == todayStr }.sumOf { it.amount }
        val todayNet = todayRev - todayExp

        val outstandingCredit = salesList.filter { it.paymentStatus != "PAID" }.sumOf { it.totalAmount - it.amountPaid }

        val alertList = mutableListOf<FarmAlert>()
        inventories.forEach { inv ->
            if (inv.bagsInStock <= inv.reorderThresholdBags) {
                alertList.add(
                    FarmAlert(
                        title = "Low Stock: ${inv.feedType}",
                        description = "Only ${String.format(Locale.US, "%.1f", inv.bagsInStock)} bags left. Reorder needed.",
                        severity = "WARNING"
                    )
                )
            }
        }
        if (outstandingCredit > 0) {
            alertList.add(
                FarmAlert(
                    title = "Outstanding Customer Credit",
                    description = "${activeFarm.value?.currencySymbol ?: "GH₵"}${String.format(Locale.US, "%.2f", outstandingCredit)} pending in unpaid sales.",
                    severity = "INFO"
                )
            )
        }

        val activities = mutableListOf<FarmActivityItem>()
        productions.take(4).forEach {
            activities.add(
                FarmActivityItem(
                    id = it.id,
                    title = "Collected ${it.cratesCollected} crates (${it.period})",
                    subtitle = "${it.flockName} • ${it.totalEggs} eggs • ${String.format(Locale.US, "%.1f", it.layingPercentage)}% rate",
                    timeLabel = it.dateString,
                    category = "EGG",
                    isSynced = it.syncStatus.name == "SYNCED"
                )
            )
        }
        salesList.take(4).forEach {
            activities.add(
                FarmActivityItem(
                    id = it.id,
                    title = "Sale: ${it.quantity} ${it.itemType.replace("_", " ")}",
                    subtitle = "${it.customerName} • ${activeFarm.value?.currencySymbol ?: "GH₵"}${String.format(Locale.US, "%.2f", it.totalAmount)} [${it.paymentStatus}]",
                    timeLabel = it.dateString,
                    category = "SALE",
                    isSynced = it.syncStatus.name == "SYNCED"
                )
            )
        }
        expenseList.take(4).forEach {
            activities.add(
                FarmActivityItem(
                    id = it.id,
                    title = "Expense: ${it.category}",
                    subtitle = "${activeFarm.value?.currencySymbol ?: "GH₵"}${String.format(Locale.US, "%.2f", it.amount)} • ${it.description}",
                    timeLabel = it.dateString,
                    category = "EXPENSE",
                    isSynced = it.syncStatus.name == "SYNCED"
                )
            )
        }
        activities.sortByDescending { it.timeLabel }

        DashboardMetrics(
            totalBirds = totalBirds,
            layingHens = layingHens,
            todayCrates = todayCrates,
            todayTotalEggs = todayTotalEggs,
            todayLayingRate = todayLayingRate,
            unsoldEggCratesInventory = unsoldCrates,
            totalFeedBags = totalFeedBags,
            estimatedFeedDaysLeft = feedDaysLeft,
            todayRevenue = todayRev,
            todayExpenses = todayExp,
            todayNet = todayNet,
            totalOutstandingCredit = outstandingCredit,
            alerts = alertList,
            recentActivities = activities
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardMetrics())

    // Authentication & Onboarding
    fun registerUser(
        fullName: String,
        emailOrPhone: String,
        passwordPlain: String,
        onResult: (Result<UserEntity>) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = sessionManager.registerUser(fullName, emailOrPhone, passwordPlain)
            launch(Dispatchers.Main) { onResult(result) }
        }
    }

    fun recoverPassword(email: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = sessionManager.recoverPassword(email)
            launch(Dispatchers.Main) { onResult(result) }
        }
    }

    fun updateFarmSettings(
        farmId: String,
        farmName: String,
        location: String,
        farmType: String,
        farmDescription: String,
        currencySymbol: String,
        eggCollectionUnit: String,
        eggsPerCrate: Int,
        defaultEggPrice: Double,
        defaultFeedPrice: Double,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = activeFarm.value ?: return@launch
            if (current.id == farmId) {
                val updated = current.copy(
                    farmName = farmName.trim(),
                    location = location.trim(),
                    farmType = farmType,
                    farmDescription = farmDescription.trim(),
                    currencySymbol = currencySymbol.trim(),
                    eggCollectionUnit = eggCollectionUnit,
                    eggsPerCrate = eggsPerCrate,
                    defaultEggCratePrice = defaultEggPrice,
                    defaultFeedBagPrice = defaultFeedPrice
                )
                repository.updateFarm(updated)
                sessionManager.selectActiveFarm(updated)
                launch(Dispatchers.Main) { onComplete() }
            }
        }
    }

    fun loginUser(
        emailOrPhone: String,
        passwordPlain: String,
        onResult: (Result<UserEntity>) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = sessionManager.loginUser(emailOrPhone, passwordPlain)
            launch(Dispatchers.Main) { onResult(result) }
        }
    }

    fun logout() {
        sessionManager.logout()
    }

    fun selectActiveFarm(farm: FarmEntity) {
        sessionManager.selectActiveFarm(farm)
    }

    fun startDemoMode(onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            sessionManager.createDemoSandbox()
            launch(Dispatchers.Main) { onComplete() }
        }
    }

    fun completeOnboarding(
        farmName: String,
        location: String,
        farmType: String,
        description: String,
        currencySymbol: String,
        eggCollectionUnit: String,
        eggsPerCrate: Int,
        initialFlocks: List<FlockSetupItem>,
        onComplete: () -> Unit
    ) {
        val user = currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val farm = sessionManager.createFarm(
                userId = user.id,
                farmName = farmName,
                location = location,
                farmType = farmType,
                farmDescription = description,
                currencySymbol = currencySymbol,
                eggCollectionUnit = eggCollectionUnit,
                eggsPerCrate = eggsPerCrate
            )

            // Add any user-configured initial flocks
            initialFlocks.forEach { item ->
                if (item.name.isNotBlank() && item.count > 0) {
                    repository.addFlock(
                        farmId = farm.id,
                        userId = user.id,
                        name = item.name,
                        birdType = item.birdType,
                        breed = item.breed,
                        count = item.count,
                        isLaying = item.isLaying,
                        ageWeeks = item.ageWeeks,
                        notes = "Initial onboarding flock"
                    )
                }
            }

            launch(Dispatchers.Main) { onComplete() }
        }
    }

    // Operations
    fun recordEggCollection(
        dateString: String,
        period: String,
        flockId: String,
        flockName: String,
        crates: Int,
        looseEggs: Int,
        brokenEggs: Int,
        notes: String
    ) {
        val farm = activeFarm.value ?: return
        val user = currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val layingFlock = flocks.value.find { it.id == flockId }
            val birdCount = layingFlock?.currentCount ?: 0
            repository.recordEggCollection(
                farmId = farm.id,
                userId = user.id,
                dateString = dateString,
                period = period,
                flockId = flockId,
                flockName = flockName,
                crates = crates,
                looseEggs = looseEggs,
                brokenEggs = brokenEggs,
                activeLayingBirds = birdCount,
                notes = notes
            )
        }
    }

    fun recordSale(
        dateString: String,
        itemType: String,
        quantity: Float,
        unitPrice: Double,
        amountPaid: Double,
        customerName: String,
        customerPhone: String,
        customerId: String?,
        notes: String,
        targetFlockId: String? = null
    ) {
        val farm = activeFarm.value ?: return
        val user = currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.recordSale(
                farmId = farm.id,
                userId = user.id,
                dateString = dateString,
                itemType = itemType,
                quantity = quantity,
                unitPrice = unitPrice,
                amountPaid = amountPaid,
                customerName = customerName,
                customerPhone = customerPhone,
                customerId = customerId,
                notes = notes,
                targetFlockId = targetFlockId
            )
        }
    }

    fun updateCustomer(customer: CustomerEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateCustomer(customer)
        }
    }

    fun deleteCustomer(customer: CustomerEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCustomer(customer)
        }
    }

    fun markSalePaid(saleId: String) {
        val farm = activeFarm.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.markSalePaid(saleId, farm.id)
        }
    }

    fun recordFeedConsumption(
        dateString: String,
        feedTypeId: String,
        flockId: String,
        flockName: String,
        bagsQuantity: Float,
        notes: String
    ) {
        val farm = activeFarm.value ?: return
        val user = currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.recordFeedConsumption(
                farmId = farm.id,
                userId = user.id,
                dateString = dateString,
                feedTypeId = feedTypeId,
                flockId = flockId,
                flockName = flockName,
                bagsQuantity = bagsQuantity,
                notes = notes
            )
        }
    }

    fun recordFeedPurchase(
        dateString: String,
        feedTypeId: String,
        feedTypeNameFallback: String,
        bagsQuantity: Float,
        costPerBag: Double,
        supplierName: String,
        recordAsExpense: Boolean,
        notes: String
    ) {
        val farm = activeFarm.value ?: return
        val user = currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.recordFeedPurchase(
                farmId = farm.id,
                userId = user.id,
                dateString = dateString,
                feedTypeId = feedTypeId,
                feedTypeNameFallback = feedTypeNameFallback,
                bagsQuantity = bagsQuantity,
                costPerBag = costPerBag,
                supplierName = supplierName,
                recordAsExpense = recordAsExpense,
                notes = notes
            )
        }
    }

    fun addFeedInventory(
        feedType: String,
        brand: String,
        initialBags: Float,
        costPerBag: Double,
        reorderThreshold: Float
    ) {
        val farm = activeFarm.value ?: return
        val user = currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.addFeedInventory(
                farmId = farm.id,
                userId = user.id,
                feedType = feedType,
                brand = brand,
                initialBags = initialBags,
                costPerBag = costPerBag,
                reorderThreshold = reorderThreshold
            )
        }
    }

    fun recordExpense(
        dateString: String,
        category: String,
        amount: Double,
        paymentMethod: String,
        description: String,
        recipient: String
    ) {
        val farm = activeFarm.value ?: return
        val user = currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.recordExpense(
                farmId = farm.id,
                userId = user.id,
                dateString = dateString,
                category = category,
                amount = amount,
                paymentMethod = paymentMethod,
                description = description,
                recipient = recipient
            )
        }
    }

    fun addCustomer(name: String, phone: String, notes: String) {
        val farm = activeFarm.value ?: return
        val user = currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.addCustomer(farm.id, user.id, name, phone, notes)
        }
    }

    fun settleCustomerBalance(customerId: String, amountSettled: Double) {
        val farm = activeFarm.value ?: return
        val user = currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.settleCustomerBalance(customerId, farm.id, user.id, amountSettled)
        }
    }

    fun addFlock(
        name: String,
        birdType: String,
        breed: String,
        count: Int,
        isLaying: Boolean,
        ageWeeks: Int,
        notes: String
    ) {
        val farm = activeFarm.value ?: return
        val user = currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.addFlock(farm.id, user.id, name, birdType, breed, count, isLaying, ageWeeks, notes)
        }
    }

    fun updateFlockCount(flockId: String, newCount: Int) {
        val farm = activeFarm.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateFlockCount(flockId, farm.id, newCount)
        }
    }

    fun recordHealthLog(
        dateString: String,
        logType: String,
        flockId: String,
        flockName: String,
        titleOrDrug: String,
        countAffected: Int,
        administeredBy: String,
        nextDueDate: String?,
        notes: String
    ) {
        val farm = activeFarm.value ?: return
        val user = currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.recordHealthLog(
                farmId = farm.id,
                userId = user.id,
                dateString = dateString,
                logType = logType,
                flockId = flockId,
                flockName = flockName,
                titleOrDrug = titleOrDrug,
                countAffected = countAffected,
                administeredBy = administeredBy,
                nextDueDate = nextDueDate,
                notes = notes
            )
        }
    }

    fun triggerSync() {
        syncManager.triggerSync()
    }

    fun toggleOfflineSimulation() {
        syncManager.toggleOfflineSimulation()
    }

    fun deleteEggProduction(record: EggProductionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteEggProduction(record)
        }
    }

    fun updateEggProduction(record: EggProductionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateEggProduction(record)
        }
    }

    fun recordFlockPopulationChange(
        flockId: String,
        changeType: String,
        count: Int,
        notes: String
    ) {
        val farm = activeFarm.value ?: return
        val user = currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.recordFlockPopulationChange(flockId, farm.id, user.id, changeType, count, notes)
        }
    }

    fun updateFlock(flock: FlockEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateFlock(flock)
        }
    }

    fun deleteFlock(flock: FlockEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteFlock(flock)
        }
    }

    fun deleteFeedLog(log: FeedLogEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteFeedLog(log)
        }
    }

    fun updateHealthLog(log: HealthLogEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateHealthLog(log)
        }
    }

    fun deleteHealthLog(log: HealthLogEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteHealthLog(log)
        }
    }

    fun updateExpense(expense: ExpenseEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateExpense(expense)
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteExpense(expense)
        }
    }

    fun deleteSale(sale: SaleEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSale(sale)
        }
    }

}

data class FlockSetupItem(
    val name: String,
    val birdType: String,
    val breed: String,
    val count: Int,
    val isLaying: Boolean,
    val ageWeeks: Int
)
