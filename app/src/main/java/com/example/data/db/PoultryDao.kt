package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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
import com.example.data.model.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE emailOrPhone = :emailOrPhone LIMIT 1")
    suspend fun getUserByEmailOrPhone(emailOrPhone: String): UserEntity?

    @Query("SELECT * FROM users LIMIT 1")
    fun getFirstUser(): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET activeFarmId = :farmId WHERE id = :userId")
    suspend fun updateActiveFarm(userId: String, farmId: String)

}

@Dao
interface FarmDao {
    @Query("SELECT * FROM farms WHERE userId = :userId ORDER BY createdAt ASC")
    fun getFarmsForUser(userId: String): Flow<List<FarmEntity>>

    @Query("SELECT * FROM farms WHERE userId = :userId ORDER BY createdAt ASC")
    suspend fun getFarmsForUserSync(userId: String): List<FarmEntity>

    @Query("SELECT * FROM farms WHERE id = :farmId LIMIT 1")
    fun getFarmById(farmId: String): Flow<FarmEntity?>

    @Query("SELECT * FROM farms WHERE id = :farmId LIMIT 1")
    suspend fun getFarmByIdSync(farmId: String): FarmEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFarm(farm: FarmEntity)

    @Update
    suspend fun updateFarm(farm: FarmEntity)

    @Delete
    suspend fun deleteFarm(farm: FarmEntity)
}

@Dao
interface FlockDao {
    @Query("SELECT * FROM flocks WHERE farmId = :farmId AND userId = :userId ORDER BY updatedAt DESC")
    fun getAllFlocks(farmId: String, userId: String): Flow<List<FlockEntity>>

    @Query("SELECT * FROM flocks WHERE farmId = :farmId AND userId = :userId AND isLayingFlock = 1 ORDER BY name ASC")
    fun getLayingFlocks(farmId: String, userId: String): Flow<List<FlockEntity>>

    @Query("SELECT * FROM flocks WHERE id = :id AND farmId = :farmId LIMIT 1")
    suspend fun getFlockById(id: String, farmId: String): FlockEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlock(flock: FlockEntity)

    @Update
    suspend fun updateFlock(flock: FlockEntity)

    @Query("UPDATE flocks SET currentCount = :newCount, updatedAt = :timestamp, syncStatus = :syncStatus WHERE id = :id AND farmId = :farmId")
    suspend fun updateFlockCount(id: String, farmId: String, newCount: Int, timestamp: Long, syncStatus: SyncStatus)

    @Delete
    suspend fun deleteFlock(flock: FlockEntity)

    @Query("SELECT * FROM flocks WHERE syncStatus != 'SYNCED' AND userId = :userId")
    suspend fun getUnsyncedFlocks(userId: String): List<FlockEntity>

    @Query("UPDATE flocks SET syncStatus = 'SYNCED' WHERE id IN (:ids)")
    suspend fun markFlocksSynced(ids: List<String>)
}

@Dao
interface EggProductionDao {
    @Update
    suspend fun updateProduction(record: EggProductionEntity)
    @Query("SELECT * FROM egg_productions WHERE farmId = :farmId AND userId = :userId ORDER BY dateString DESC, createdAt DESC")
    fun getAllProductions(farmId: String, userId: String): Flow<List<EggProductionEntity>>

    @Query("SELECT * FROM egg_productions WHERE farmId = :farmId AND userId = :userId ORDER BY dateString DESC, createdAt DESC LIMIT :limit")
    fun getRecentProductions(farmId: String, userId: String, limit: Int = 30): Flow<List<EggProductionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduction(record: EggProductionEntity)

    @Delete
    suspend fun deleteProduction(record: EggProductionEntity)

    @Query("SELECT * FROM egg_productions WHERE syncStatus != 'SYNCED' AND userId = :userId")
    suspend fun getUnsyncedProductions(userId: String): List<EggProductionEntity>

    @Query("UPDATE egg_productions SET syncStatus = 'SYNCED' WHERE id IN (:ids)")
    suspend fun markProductionsSynced(ids: List<String>)
}

@Dao
interface FeedDao {
    @Delete
    suspend fun deleteInventory(item: FeedInventoryEntity)
    @Delete
    suspend fun deleteLog(log: FeedLogEntity)
    @Query("SELECT * FROM feed_inventory WHERE farmId = :farmId AND userId = :userId ORDER BY feedType ASC")
    fun getAllInventories(farmId: String, userId: String): Flow<List<FeedInventoryEntity>>

    @Query("SELECT * FROM feed_inventory WHERE id = :id AND farmId = :farmId LIMIT 1")
    suspend fun getInventoryById(id: String, farmId: String): FeedInventoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventory(item: FeedInventoryEntity)

    @Update
    suspend fun updateInventory(item: FeedInventoryEntity)

    @Query("UPDATE feed_inventory SET bagsInStock = :newStock, syncStatus = :syncStatus WHERE id = :id AND farmId = :farmId")
    suspend fun updateStock(id: String, farmId: String, newStock: Float, syncStatus: SyncStatus)

    @Query("SELECT * FROM feed_logs WHERE farmId = :farmId AND userId = :userId ORDER BY dateString DESC, createdAt DESC")
    fun getAllLogs(farmId: String, userId: String): Flow<List<FeedLogEntity>>

    @Query("SELECT * FROM feed_logs WHERE farmId = :farmId AND userId = :userId ORDER BY dateString DESC, createdAt DESC LIMIT :limit")
    fun getRecentLogs(farmId: String, userId: String, limit: Int = 30): Flow<List<FeedLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: FeedLogEntity)

    @Query("SELECT * FROM feed_inventory WHERE syncStatus != 'SYNCED' AND userId = :userId")
    suspend fun getUnsyncedInventories(userId: String): List<FeedInventoryEntity>

    @Query("UPDATE feed_inventory SET syncStatus = 'SYNCED' WHERE id IN (:ids)")
    suspend fun markInventoriesSynced(ids: List<String>)

    @Query("SELECT * FROM feed_logs WHERE syncStatus != 'SYNCED' AND userId = :userId")
    suspend fun getUnsyncedLogs(userId: String): List<FeedLogEntity>

    @Query("UPDATE feed_logs SET syncStatus = 'SYNCED' WHERE id IN (:ids)")
    suspend fun markLogsSynced(ids: List<String>)
}

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales WHERE farmId = :farmId AND userId = :userId ORDER BY dateString DESC, createdAt DESC")
    fun getAllSales(farmId: String, userId: String): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE farmId = :farmId AND userId = :userId ORDER BY dateString DESC, createdAt DESC LIMIT :limit")
    fun getRecentSales(farmId: String, userId: String, limit: Int = 30): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE farmId = :farmId AND userId = :userId AND paymentStatus != 'PAID' ORDER BY dateString DESC")
    fun getUnpaidSales(farmId: String, userId: String): Flow<List<SaleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleEntity)

    @Update
    suspend fun updateSale(sale: SaleEntity)

    @Query("UPDATE sales SET paymentStatus = 'PAID', amountPaid = totalAmount, syncStatus = :syncStatus WHERE id = :id AND farmId = :farmId")
    suspend fun markSalePaid(id: String, farmId: String, syncStatus: SyncStatus)

    @Delete
    suspend fun deleteSale(sale: SaleEntity)

    @Query("SELECT * FROM sales WHERE syncStatus != 'SYNCED' AND userId = :userId")
    suspend fun getUnsyncedSales(userId: String): List<SaleEntity>

    @Query("UPDATE sales SET syncStatus = 'SYNCED' WHERE id IN (:ids)")
    suspend fun markSalesSynced(ids: List<String>)
}

@Dao
interface ExpenseDao {
    @Update
    suspend fun updateExpense(expense: ExpenseEntity)
    @Query("SELECT * FROM expenses WHERE farmId = :farmId AND userId = :userId ORDER BY dateString DESC, createdAt DESC")
    fun getAllExpenses(farmId: String, userId: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE farmId = :farmId AND userId = :userId ORDER BY dateString DESC, createdAt DESC LIMIT :limit")
    fun getRecentExpenses(farmId: String, userId: String, limit: Int = 30): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses WHERE syncStatus != 'SYNCED' AND userId = :userId")
    suspend fun getUnsyncedExpenses(userId: String): List<ExpenseEntity>

    @Query("UPDATE expenses SET syncStatus = 'SYNCED' WHERE id IN (:ids)")
    suspend fun markExpensesSynced(ids: List<String>)
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE farmId = :farmId AND userId = :userId ORDER BY name ASC")
    fun getAllCustomers(farmId: String, userId: String): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id AND farmId = :farmId LIMIT 1")
    suspend fun getCustomerById(id: String, farmId: String): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity)

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Delete
    suspend fun deleteCustomer(customer: CustomerEntity)

    @Query("SELECT * FROM customers WHERE syncStatus != 'SYNCED' AND userId = :userId")
    suspend fun getUnsyncedCustomers(userId: String): List<CustomerEntity>

    @Query("UPDATE customers SET syncStatus = 'SYNCED' WHERE id IN (:ids)")
    suspend fun markCustomersSynced(ids: List<String>)
}

@Dao
interface HealthDao {
    @Update
    suspend fun updateLog(log: HealthLogEntity)
    @Query("SELECT * FROM health_logs WHERE farmId = :farmId AND userId = :userId ORDER BY dateString DESC, createdAt DESC")
    fun getAllLogs(farmId: String, userId: String): Flow<List<HealthLogEntity>>

    @Query("SELECT * FROM health_logs WHERE farmId = :farmId AND userId = :userId ORDER BY dateString DESC, createdAt DESC LIMIT :limit")
    fun getRecentLogs(farmId: String, userId: String, limit: Int = 30): Flow<List<HealthLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: HealthLogEntity)

    @Delete
    suspend fun deleteLog(log: HealthLogEntity)

    @Query("SELECT * FROM health_logs WHERE syncStatus != 'SYNCED' AND userId = :userId")
    suspend fun getUnsyncedLogs(userId: String): List<HealthLogEntity>

    @Query("UPDATE health_logs SET syncStatus = 'SYNCED' WHERE id IN (:ids)")
    suspend fun markLogsSynced(ids: List<String>)
}

@Dao
interface SyncAuditDao {
    @Query("SELECT * FROM sync_audits WHERE farmId = :farmId AND userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentAudits(farmId: String, userId: String, limit: Int = 20): Flow<List<SyncAuditEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudit(audit: SyncAuditEntity)
}
