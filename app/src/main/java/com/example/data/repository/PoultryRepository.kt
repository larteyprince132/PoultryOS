package com.example.data.repository

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
import com.example.data.model.SyncStatus
import com.example.data.sync.SyncManager
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class PoultryRepository(
    private val database: PoultryDatabase,
    private val syncManager: SyncManager
) {
    // Farm operations
    fun getFarmsForUser(userId: String): Flow<List<FarmEntity>> =
        database.farmDao().getFarmsForUser(userId)

    fun getFarmById(farmId: String): Flow<FarmEntity?> =
        database.farmDao().getFarmById(farmId)

    suspend fun updateFarm(farm: FarmEntity) {
        database.farmDao().updateFarm(farm)
        syncManager.refreshPendingCount()
    }

    // Flocks
    fun getAllFlocks(farmId: String, userId: String): Flow<List<FlockEntity>> =
        database.flockDao().getAllFlocks(farmId, userId)

    fun getLayingFlocks(farmId: String, userId: String): Flow<List<FlockEntity>> =
        database.flockDao().getLayingFlocks(farmId, userId)

    suspend fun addFlock(
        farmId: String,
        userId: String,
        name: String,
        birdType: String,
        breed: String,
        count: Int,
        isLaying: Boolean,
        ageWeeks: Int,
        notes: String
    ) {
        val flock = FlockEntity(
            id = UUID.randomUUID().toString(),
            farmId = farmId,
            userId = userId,
            name = name.trim(),
            birdType = birdType,
            breed = breed.trim(),
            initialCount = count,
            currentCount = count,
            isLayingFlock = isLaying,
            ageWeeks = ageWeeks,
            notes = notes.trim(),
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        )
        database.flockDao().insertFlock(flock)
        syncManager.refreshPendingCount()
    }

    suspend fun updateFlockCount(flockId: String, farmId: String, newCount: Int) {
        database.flockDao().updateFlockCount(
            id = flockId,
            farmId = farmId,
            newCount = newCount,
            timestamp = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        )
        syncManager.refreshPendingCount()
    }

    // Egg Production
    fun getAllProductions(farmId: String, userId: String): Flow<List<EggProductionEntity>> =
        database.eggProductionDao().getAllProductions(farmId, userId)

    fun getRecentProductions(farmId: String, userId: String, limit: Int = 50): Flow<List<EggProductionEntity>> =
        database.eggProductionDao().getRecentProductions(farmId, userId, limit)

    suspend fun recordEggCollection(
        farmId: String,
        userId: String,
        dateString: String,
        period: String,
        flockId: String,
        flockName: String,
        crates: Int,
        looseEggs: Int,
        brokenEggs: Int,
        activeLayingBirds: Int,
        notes: String
    ) {
        val totalEggs = (crates * 30) + looseEggs
        val layingPct = if (activeLayingBirds > 0) {
            (totalEggs.toFloat() / activeLayingBirds.toFloat()) * 100f
        } else 0f

        val record = EggProductionEntity(
            id = UUID.randomUUID().toString(),
            farmId = farmId,
            userId = userId,
            dateString = dateString,
            period = period,
            flockId = flockId,
            flockName = flockName,
            cratesCollected = crates,
            looseEggsCollected = looseEggs,
            brokenDamagedEggs = brokenEggs,
            totalEggs = totalEggs,
            layingPercentage = layingPct,
            notes = notes.trim(),
            createdAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        )
        database.eggProductionDao().insertProduction(record)
        syncManager.refreshPendingCount()
    }

    // Feed
    fun getAllFeedInventories(farmId: String, userId: String): Flow<List<FeedInventoryEntity>> =
        database.feedDao().getAllInventories(farmId, userId)

    fun getAllFeedLogs(farmId: String, userId: String): Flow<List<FeedLogEntity>> =
        database.feedDao().getAllLogs(farmId, userId)

    fun getRecentFeedLogs(farmId: String, userId: String, limit: Int = 50): Flow<List<FeedLogEntity>> =
        database.feedDao().getRecentLogs(farmId, userId, limit)

    suspend fun addFeedInventory(
        farmId: String,
        userId: String,
        feedType: String,
        brand: String,
        initialBags: Float,
        costPerBag: Double,
        reorderThreshold: Float
    ) {
        val item = FeedInventoryEntity(
            id = UUID.randomUUID().toString(),
            farmId = farmId,
            userId = userId,
            feedType = feedType.trim(),
            brand = brand.trim(),
            bagsInStock = initialBags,
            costPerBag = costPerBag,
            reorderThresholdBags = reorderThreshold,
            lastRestockedDate = "Initial Stock",
            syncStatus = SyncStatus.PENDING
        )
        database.feedDao().insertInventory(item)
        syncManager.refreshPendingCount()
    }

    suspend fun recordFeedConsumption(
        farmId: String,
        userId: String,
        dateString: String,
        feedTypeId: String,
        flockId: String,
        flockName: String,
        bagsQuantity: Float,
        notes: String
    ) {
        val inventory = database.feedDao().getInventoryById(feedTypeId, farmId)
        val feedTypeName = inventory?.feedType ?: "Feed"

        if (inventory != null) {
            val updatedBags = maxOf(0f, inventory.bagsInStock - bagsQuantity)
            database.feedDao().updateStock(feedTypeId, farmId, updatedBags, SyncStatus.PENDING)
        }

        val log = FeedLogEntity(
            id = UUID.randomUUID().toString(),
            farmId = farmId,
            userId = userId,
            dateString = dateString,
            logType = "CONSUMPTION",
            feedTypeId = feedTypeId,
            feedTypeName = feedTypeName,
            flockId = flockId,
            flockName = flockName,
            bagsQuantity = bagsQuantity,
            costPerBag = inventory?.costPerBag ?: 0.0,
            totalCost = (inventory?.costPerBag ?: 0.0) * bagsQuantity,
            notes = notes.trim(),
            createdAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        )
        database.feedDao().insertLog(log)
        syncManager.refreshPendingCount()
    }

    suspend fun recordFeedPurchase(
        farmId: String,
        userId: String,
        dateString: String,
        feedTypeId: String,
        feedTypeNameFallback: String,
        bagsQuantity: Float,
        costPerBag: Double,
        supplierName: String,
        recordAsExpense: Boolean,
        notes: String
    ) {
        val inventory = database.feedDao().getInventoryById(feedTypeId, farmId)
        val feedTypeName = inventory?.feedType ?: feedTypeNameFallback

        if (inventory != null) {
            val updatedBags = inventory.bagsInStock + bagsQuantity
            database.feedDao().updateStock(feedTypeId, farmId, updatedBags, SyncStatus.PENDING)
        } else {
            // New inventory type created via purchase
            database.feedDao().insertInventory(
                FeedInventoryEntity(
                    id = feedTypeId,
                    farmId = farmId,
                    userId = userId,
                    feedType = feedTypeName,
                    brand = supplierName,
                    bagsInStock = bagsQuantity,
                    costPerBag = costPerBag,
                    lastRestockedDate = dateString,
                    syncStatus = SyncStatus.PENDING
                )
            )
        }

        val totalCost = costPerBag * bagsQuantity

        val log = FeedLogEntity(
            id = UUID.randomUUID().toString(),
            farmId = farmId,
            userId = userId,
            dateString = dateString,
            logType = "PURCHASE",
            feedTypeId = feedTypeId,
            feedTypeName = feedTypeName,
            bagsQuantity = bagsQuantity,
            costPerBag = costPerBag,
            totalCost = totalCost,
            supplierName = supplierName.trim(),
            notes = notes.trim(),
            createdAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        )
        database.feedDao().insertLog(log)

        if (recordAsExpense) {
            recordExpense(
                farmId = farmId,
                userId = userId,
                dateString = dateString,
                category = "FEED",
                amount = totalCost,
                paymentMethod = "CASH",
                description = "Purchase of $bagsQuantity bags of $feedTypeName",
                recipient = supplierName
            )
        }

        syncManager.refreshPendingCount()
    }

    // Sales
    fun getAllSales(farmId: String, userId: String): Flow<List<SaleEntity>> =
        database.saleDao().getAllSales(farmId, userId)

    fun getRecentSales(farmId: String, userId: String, limit: Int = 50): Flow<List<SaleEntity>> =
        database.saleDao().getRecentSales(farmId, userId, limit)

    fun getUnpaidSales(farmId: String, userId: String): Flow<List<SaleEntity>> =
        database.saleDao().getUnpaidSales(farmId, userId)

    suspend fun recordSale(
        farmId: String,
        userId: String,
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
        val totalAmount = quantity.toDouble() * unitPrice
        val paymentStatus = when {
            amountPaid >= totalAmount -> "PAID"
            amountPaid > 0.0 -> "PARTIAL"
            else -> "UNPAID"
        }
        val finalCustName = if (customerName.isBlank()) "Walk-in Customer" else customerName.trim()
        val sale = SaleEntity(
            id = UUID.randomUUID().toString(),
            farmId = farmId,
            userId = userId,
            dateString = dateString,
            itemType = itemType,
            quantity = quantity,
            unitPrice = unitPrice,
            totalAmount = totalAmount,
            amountPaid = amountPaid,
            paymentStatus = paymentStatus,
            customerId = customerId,
            customerName = finalCustName,
            customerPhone = customerPhone.trim(),
            notes = notes.trim(),
            createdAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        )
        database.saleDao().insertSale(sale)

        // Flock population reduction if live birds sold
        if (itemType == "LIVE_BIRD" && targetFlockId != null && quantity > 0) {
            val flock = database.flockDao().getFlockById(targetFlockId, farmId)
            if (flock != null) {
                val updatedCount = maxOf(0, flock.currentCount - quantity.toInt())
                database.flockDao().updateFlockCount(targetFlockId, farmId, updatedCount, System.currentTimeMillis(), SyncStatus.PENDING)
            }
        }

        // Customer balance and purchase history update
        if (!customerId.isNullOrBlank()) {
            val customer = database.customerDao().getCustomerById(customerId, farmId)
            if (customer != null) {
                val unpaid = maxOf(0.0, totalAmount - amountPaid)
                database.customerDao().updateCustomer(
                    customer.copy(
                        outstandingBalance = customer.outstandingBalance + unpaid,
                        totalPurchases = customer.totalPurchases + totalAmount,
                        updatedAt = System.currentTimeMillis(),
                        syncStatus = SyncStatus.PENDING
                    )
                )
            }
        }
        syncManager.refreshPendingCount()
    }

    suspend fun updateCustomer(customer: CustomerEntity) {
        database.customerDao().updateCustomer(customer.copy(updatedAt = System.currentTimeMillis(), syncStatus = SyncStatus.PENDING))
        syncManager.refreshPendingCount()
    }

    suspend fun deleteCustomer(customer: CustomerEntity) {
        database.customerDao().deleteCustomer(customer)
        syncManager.refreshPendingCount()
    }

    suspend fun markSalePaid(saleId: String, farmId: String) {
        database.saleDao().markSalePaid(saleId, farmId, SyncStatus.PENDING)
        syncManager.refreshPendingCount()
    }

    // Expenses
    fun getAllExpenses(farmId: String, userId: String): Flow<List<ExpenseEntity>> =
        database.expenseDao().getAllExpenses(farmId, userId)

    fun getRecentExpenses(farmId: String, userId: String, limit: Int = 50): Flow<List<ExpenseEntity>> =
        database.expenseDao().getRecentExpenses(farmId, userId, limit)

    suspend fun recordExpense(
        farmId: String,
        userId: String,
        dateString: String,
        category: String,
        amount: Double,
        paymentMethod: String,
        description: String,
        recipient: String
    ) {
        val expense = ExpenseEntity(
            id = UUID.randomUUID().toString(),
            farmId = farmId,
            userId = userId,
            dateString = dateString,
            category = category,
            amount = amount,
            paymentMethod = paymentMethod,
            description = description.trim(),
            recipient = recipient.trim(),
            createdAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        )
        database.expenseDao().insertExpense(expense)
        syncManager.refreshPendingCount()
    }

    // Customers
    fun getAllCustomers(farmId: String, userId: String): Flow<List<CustomerEntity>> =
        database.customerDao().getAllCustomers(farmId, userId)

    suspend fun addCustomer(
        farmId: String,
        userId: String,
        name: String,
        phone: String,
        notes: String
    ) {
        val customer = CustomerEntity(
            id = UUID.randomUUID().toString(),
            farmId = farmId,
            userId = userId,
            name = name.trim(),
            phone = phone.trim(),
            notes = notes.trim(),
            totalPurchases = 0.0,
            outstandingBalance = 0.0,
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        )
        database.customerDao().insertCustomer(customer)
        syncManager.refreshPendingCount()
    }

    suspend fun settleCustomerBalance(
        customerId: String,
        farmId: String,
        userId: String,
        amountSettled: Double
    ) {
        val customer = database.customerDao().getCustomerById(customerId, farmId) ?: return
        val newBalance = maxOf(0.0, customer.outstandingBalance - amountSettled)
        database.customerDao().updateCustomer(
            customer.copy(
                outstandingBalance = newBalance,
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING
            )
        )
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        database.saleDao().insertSale(
            SaleEntity(
                id = UUID.randomUUID().toString(),
                farmId = farmId,
                userId = userId,
                dateString = today,
                itemType = "OTHER",
                quantity = 1f,
                unitPrice = amountSettled,
                totalAmount = amountSettled,
                amountPaid = amountSettled,
                paymentStatus = "PAID",
                customerId = customerId,
                customerName = customer.name,
                customerPhone = customer.phone,
                notes = "Debt settlement of ${customer.name}",
                createdAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING
            )
        )
        syncManager.refreshPendingCount()
    }

    // Health
    fun getAllHealthLogs(farmId: String, userId: String): Flow<List<HealthLogEntity>> =
        database.healthDao().getAllLogs(farmId, userId)

    fun getRecentHealthLogs(farmId: String, userId: String, limit: Int = 50): Flow<List<HealthLogEntity>> =
        database.healthDao().getRecentLogs(farmId, userId, limit)

    suspend fun recordHealthLog(
        farmId: String,
        userId: String,
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
        val log = HealthLogEntity(
            id = UUID.randomUUID().toString(),
            farmId = farmId,
            userId = userId,
            dateString = dateString,
            logType = logType,
            flockId = flockId,
            flockName = flockName,
            titleOrDrug = titleOrDrug.trim(),
            countAffected = countAffected,
            administeredBy = administeredBy.trim(),
            nextDueDate = nextDueDate,
            notes = notes.trim(),
            createdAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        )
        database.healthDao().insertLog(log)

        if (logType == "MORTALITY" && countAffected > 0) {
            val flock = database.flockDao().getFlockById(flockId, farmId)
            if (flock != null) {
                val updatedCount = maxOf(0, flock.currentCount - countAffected)
                database.flockDao().updateFlockCount(flockId, farmId, updatedCount, System.currentTimeMillis(), SyncStatus.PENDING)
            }
        }

        syncManager.refreshPendingCount()
    }

    fun getRecentAudits(farmId: String, userId: String): Flow<List<SyncAuditEntity>> =
        database.syncAuditDao().getRecentAudits(farmId, userId, 20)

    suspend fun updateFlock(flock: FlockEntity) {
        database.flockDao().updateFlock(flock.copy(updatedAt = System.currentTimeMillis(), syncStatus = SyncStatus.PENDING))
        syncManager.refreshPendingCount()
    }

    suspend fun deleteFlock(flock: FlockEntity) {
        database.flockDao().deleteFlock(flock)
        syncManager.refreshPendingCount()
    }

    suspend fun recordFlockPopulationChange(
        flockId: String,
        farmId: String,
        userId: String,
        changeType: String, // 'ADDITION', 'MORTALITY', 'SALE', 'TRANSFER'
        count: Int,
        notes: String
    ) {
        val flock = database.flockDao().getFlockById(flockId, farmId) ?: return
        val newCount = when (changeType) {
            "ADDITION" -> flock.currentCount + count
            "MORTALITY", "SALE", "TRANSFER" -> maxOf(0, flock.currentCount - count)
            else -> flock.currentCount
        }
        database.flockDao().updateFlockCount(flockId, farmId, newCount, System.currentTimeMillis(), SyncStatus.PENDING)

        // Also record mortality in health log if mortality
        if (changeType == "MORTALITY") {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            database.healthDao().insertLog(
                HealthLogEntity(
                    id = UUID.randomUUID().toString(),
                    farmId = farmId,
                    userId = userId,
                    dateString = today,
                    logType = "MORTALITY",
                    flockId = flockId,
                    flockName = flock.name,
                    titleOrDrug = "Flock Mortality:  birds",
                    countAffected = count,
                    notes = notes,
                    syncStatus = SyncStatus.PENDING
                )
            )
        }
        syncManager.refreshPendingCount()
    }

    suspend fun updateEggProduction(record: EggProductionEntity) {
        database.eggProductionDao().updateProduction(record.copy(syncStatus = SyncStatus.PENDING))
        syncManager.refreshPendingCount()
    }

    suspend fun deleteEggProduction(record: EggProductionEntity) {
        database.eggProductionDao().deleteProduction(record)
        syncManager.refreshPendingCount()
    }

    suspend fun deleteFeedLog(log: FeedLogEntity) {
        // Revert inventory deduction/addition if needed
        if (log.logType == "CONSUMPTION") {
            val inv = database.feedDao().getInventoryById(log.feedTypeId, log.farmId)
            if (inv != null) {
                database.feedDao().updateStock(log.feedTypeId, log.farmId, inv.bagsInStock + log.bagsQuantity, SyncStatus.PENDING)
            }
        } else if (log.logType == "PURCHASE") {
            val inv = database.feedDao().getInventoryById(log.feedTypeId, log.farmId)
            if (inv != null) {
                database.feedDao().updateStock(log.feedTypeId, log.farmId, maxOf(0f, inv.bagsInStock - log.bagsQuantity), SyncStatus.PENDING)
            }
        }
        database.feedDao().deleteLog(log)
        syncManager.refreshPendingCount()
    }

    suspend fun updateHealthLog(log: HealthLogEntity) {
        database.healthDao().updateLog(log.copy(syncStatus = SyncStatus.PENDING))
        syncManager.refreshPendingCount()
    }

    suspend fun deleteHealthLog(log: HealthLogEntity) {
        database.healthDao().deleteLog(log)
        syncManager.refreshPendingCount()
    }

    suspend fun updateExpense(expense: ExpenseEntity) {
        database.expenseDao().updateExpense(expense.copy(syncStatus = SyncStatus.PENDING))
        syncManager.refreshPendingCount()
    }

    suspend fun deleteExpense(expense: ExpenseEntity) {
        database.expenseDao().deleteExpense(expense)
        syncManager.refreshPendingCount()
    }

    suspend fun deleteSale(sale: SaleEntity) {
        // Revert customer balance if unpaid sale is deleted
        if (!sale.customerId.isNullOrBlank() && sale.totalAmount > sale.amountPaid) {
            val unpaid = sale.totalAmount - sale.amountPaid
            val cust = database.customerDao().getCustomerById(sale.customerId, sale.farmId)
            if (cust != null) {
                database.customerDao().updateCustomer(
                    cust.copy(
                        outstandingBalance = maxOf(0.0, cust.outstandingBalance - unpaid),
                        totalPurchases = maxOf(0.0, cust.totalPurchases - sale.totalAmount),
                        updatedAt = System.currentTimeMillis(),
                        syncStatus = SyncStatus.PENDING
                    )
                )
            }
        }
        database.saleDao().deleteSale(sale)
        syncManager.refreshPendingCount()
    }

}
