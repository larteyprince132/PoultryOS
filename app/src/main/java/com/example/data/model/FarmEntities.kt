package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val fullName: String,
    val emailOrPhone: String,
    val passwordHash: String,
    val activeFarmId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "farms")
data class FarmEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val farmName: String,
    val location: String = "",
    val farmType: String = "Commercial Layers", // "Commercial Layers", "Broilers", "Mixed Poultry", "Turkeys", "Breeder Flock"
    val farmDescription: String = "",
    val currencySymbol: String = "GHS",
    val eggCollectionUnit: String = "CRATES", // "CRATES" (30 eggs), "TRAYS", "CARTONS" (12 eggs), "INDIVIDUAL_EGGS"
    val eggsPerCrate: Int = 30,
    val defaultEggCratePrice: Double = 0.0,
    val defaultFeedBagPrice: Double = 0.0,
    val isDemoFarm: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "flocks")
data class FlockEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val farmId: String,
    val userId: String,
    val name: String,
    val birdType: String, // "LAYERS", "TURKEYS", "PULLETS", "BROILERS", "OTHER"
    val breed: String = "",
    val initialCount: Int,
    val currentCount: Int,
    val isLayingFlock: Boolean = false,
    val ageWeeks: Int = 0,
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING
)

@Entity(tableName = "egg_productions")
data class EggProductionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val farmId: String,
    val userId: String,
    val dateString: String, // "YYYY-MM-DD"
    val period: String, // "MORNING", "AFTERNOON", "FULL_DAY"
    val flockId: String,
    val flockName: String,
    val cratesCollected: Int,
    val looseEggsCollected: Int = 0,
    val brokenDamagedEggs: Int = 0,
    val totalEggs: Int = (cratesCollected * 30) + looseEggsCollected,
    val layingPercentage: Float = 0f,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING
)

@Entity(tableName = "feed_inventory")
data class FeedInventoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val farmId: String,
    val userId: String,
    val feedType: String, // "Layers Mash", "Growers Mash", "Starter Crumbles", "Turkey Feed"
    val brand: String = "",
    val bagsInStock: Float,
    val bagWeightKg: Float = 50.0f,
    val reorderThresholdBags: Float = 5.0f,
    val costPerBag: Double = 0.0,
    val lastRestockedDate: String = "",
    val syncStatus: SyncStatus = SyncStatus.PENDING
)

@Entity(tableName = "feed_logs")
data class FeedLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val farmId: String,
    val userId: String,
    val dateString: String,
    val logType: String, // "CONSUMPTION", "PURCHASE"
    val feedTypeId: String,
    val feedTypeName: String,
    val flockId: String = "",
    val flockName: String = "",
    val bagsQuantity: Float,
    val costPerBag: Double = 0.0,
    val totalCost: Double = 0.0,
    val supplierName: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING
)

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val farmId: String,
    val userId: String,
    val dateString: String,
    val itemType: String, // "EGG_CRATE", "LIVE_BIRD", "MANURE", "OTHER"
    val quantity: Float,
    val unitPrice: Double,
    val totalAmount: Double,
    val amountPaid: Double,
    val paymentStatus: String, // "PAID", "PARTIAL", "UNPAID"
    val customerId: String? = null,
    val customerName: String = "Walk-in Customer",
    val customerPhone: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val farmId: String,
    val userId: String,
    val dateString: String,
    val category: String, // "FEED", "MEDICATION", "VACCINATION", "LABOUR", "UTILITIES", "TRANSPORT", "EQUIPMENT", "REPAIRS", "OTHER"
    val amount: Double,
    val paymentMethod: String = "CASH", // "CASH", "MOMO", "BANK"
    val description: String = "",
    val recipient: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING
)

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val farmId: String,
    val userId: String,
    val name: String,
    val phone: String = "",
    val notes: String = "",
    val totalPurchases: Double = 0.0,
    val outstandingBalance: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING
)

@Entity(tableName = "health_logs")
data class HealthLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val farmId: String,
    val userId: String,
    val dateString: String,
    val logType: String, // "VACCINATION", "MEDICATION", "MORTALITY", "OBSERVATION"
    val flockId: String,
    val flockName: String,
    val titleOrDrug: String,
    val countAffected: Int = 0,
    val administeredBy: String = "",
    val nextDueDate: String? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING
)

@Entity(tableName = "sync_audits")
data class SyncAuditEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val farmId: String,
    val userId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val operation: String,
    val recordsSynced: Int,
    val status: String, // "SUCCESS", "FAILED"
    val details: String = ""
)
