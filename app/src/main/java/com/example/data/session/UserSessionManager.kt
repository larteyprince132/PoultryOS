package com.example.data.session

import android.content.Context
import android.content.SharedPreferences
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
import com.example.data.model.SyncStatus
import com.example.data.model.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class UserSessionManager(
    private val database: PoultryDatabase,
    context: Context,
    private val scope: CoroutineScope
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("poultry_os_session_prefs", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _activeFarm = MutableStateFlow<FarmEntity?>(null)
    val activeFarm: StateFlow<FarmEntity?> = _activeFarm.asStateFlow()

    private val _isSessionLoaded = MutableStateFlow(false)
    val isSessionLoaded: StateFlow<Boolean> = _isSessionLoaded.asStateFlow()

    init {
        restoreSession()
    }

    private fun restoreSession() {
        scope.launch(Dispatchers.IO) {
            val savedUserId = prefs.getString("saved_user_id", null)
            val savedFarmId = prefs.getString("saved_farm_id", null)

            if (savedUserId != null) {
                val user = database.userDao().getUserById(savedUserId)
                if (user != null) {
                    _currentUser.value = user
                    val farmIdToUse = savedFarmId ?: user.activeFarmId
                    if (farmIdToUse != null) {
                        val farm = database.farmDao().getFarmByIdSync(farmIdToUse)
                        _activeFarm.value = farm
                    } else {
                        val userFarms = database.farmDao().getFarmsForUserSync(user.id)
                        _activeFarm.value = userFarms.firstOrNull()
                    }
                }
            }
            _isSessionLoaded.value = true
        }
    }

    suspend fun registerUser(
        fullName: String,
        emailOrPhone: String,
        passwordPlain: String
    ): Result<UserEntity> {
        val trimmedEmail = emailOrPhone.trim().lowercase(Locale.ROOT)
        val existing = database.userDao().getUserByEmailOrPhone(trimmedEmail)
        if (existing != null) {
            return Result.failure(Exception("An account with this email/phone already exists."))
        }

        val passwordHash = hashPassword(passwordPlain)
        val user = UserEntity(
            id = UUID.randomUUID().toString(),
            fullName = fullName.trim(),
            emailOrPhone = trimmedEmail,
            passwordHash = passwordHash,
            activeFarmId = null,
            createdAt = System.currentTimeMillis()
        )
        database.userDao().insertUser(user)
        saveActiveUser(user)
        return Result.success(user)
    }

    suspend fun recoverPassword(
        emailOrPhone: String,
        newPasswordPlain: String
    ): Result<Unit> {
        val trimmed = emailOrPhone.trim().lowercase(Locale.ROOT)
        if (newPasswordPlain.length < 4) {
            return Result.failure(Exception("Password must be at least 4 characters long."))
        }
        val user = database.userDao().getUserByEmailOrPhone(trimmed)
            ?: return Result.failure(Exception("No registered account found with that email or phone."))

        val newHash = hashPassword(newPasswordPlain)
        database.userDao().resetPasswordByEmailOrPhone(trimmed, newHash)
        return Result.success(Unit)
    }

    suspend fun loginUser(
        emailOrPhone: String,
        passwordPlain: String
    ): Result<UserEntity> {
        val trimmed = emailOrPhone.trim().lowercase(Locale.ROOT)
        val user = database.userDao().getUserByEmailOrPhone(trimmed)
            ?: return Result.failure(Exception("Account not found. Please register first."))

        val inputHash = hashPassword(passwordPlain)
        if (user.passwordHash != inputHash) {
            return Result.failure(Exception("Incorrect password. Please try again."))
        }

        saveActiveUser(user)
        val userFarms = database.farmDao().getFarmsForUserSync(user.id)
        val farmToActivate = user.activeFarmId?.let { id -> userFarms.find { it.id == id } } ?: userFarms.firstOrNull()
        if (farmToActivate != null) {
            selectActiveFarm(farmToActivate)
        } else {
            _activeFarm.value = null
        }

        return Result.success(user)
    }

    suspend fun createFarm(
        userId: String,
        farmName: String,
        location: String,
        farmType: String,
        farmDescription: String = "",
        currencySymbol: String = "GHS",
        eggCollectionUnit: String = "CRATES",
        eggsPerCrate: Int = 30,
        defaultEggPrice: Double = 0.0,
        defaultFeedPrice: Double = 0.0,
        isDemo: Boolean = false
    ): FarmEntity {
        val farm = FarmEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            farmName = farmName.trim(),
            location = location.trim(),
            farmType = farmType,
            farmDescription = farmDescription.trim(),
            currencySymbol = currencySymbol.trim(),
            eggCollectionUnit = eggCollectionUnit,
            eggsPerCrate = eggsPerCrate,
            defaultEggCratePrice = defaultEggPrice,
            defaultFeedBagPrice = defaultFeedPrice,
            isDemoFarm = isDemo,
            createdAt = System.currentTimeMillis()
        )
        database.farmDao().insertFarm(farm)
        database.userDao().updateActiveFarm(userId, farm.id)
        selectActiveFarm(farm)
        return farm
    }

    fun selectActiveFarm(farm: FarmEntity) {
        _activeFarm.value = farm
        prefs.edit().putString("saved_farm_id", farm.id).apply()
        _currentUser.value?.let { user ->
            scope.launch(Dispatchers.IO) {
                database.userDao().updateActiveFarm(user.id, farm.id)
            }
        }
    }

    private fun saveActiveUser(user: UserEntity) {
        _currentUser.value = user
        prefs.edit().putString("saved_user_id", user.id).apply()
    }

    fun logout() {
        _currentUser.value = null
        _activeFarm.value = null
        prefs.edit().clear().apply()
    }

    /**
     * Creates an isolated Demo sandbox environment with sample data tagged isDemoFarm = true.
     * This sample data is NEVER mixed with real farmer accounts.
     */
    suspend fun createDemoSandbox(): Pair<UserEntity, FarmEntity> {
        val demoUserId = "demo_user_" + UUID.randomUUID().toString().take(8)
        val demoUser = UserEntity(
            id = demoUserId,
            fullName = "Demo Farmer",
            emailOrPhone = "demo@poultryos.farm",
            passwordHash = "demo_sandbox",
            activeFarmId = null,
            createdAt = System.currentTimeMillis()
        )
        database.userDao().insertUser(demoUser)
        saveActiveUser(demoUser)

        val demoFarmId = "demo_farm_" + UUID.randomUUID().toString().take(8)
        val demoFarm = FarmEntity(
            id = demoFarmId,
            userId = demoUserId,
            farmName = "Demo Model Farm",
            location = "Central Region, Ghana",
            farmType = "Commercial Layers & Turkeys",
            farmDescription = "Interactive demo farm showing live metrics and sample records",
            currencySymbol = "GH₵",
            eggCollectionUnit = "CRATES",
            eggsPerCrate = 30,
            defaultEggCratePrice = 35.0,
            defaultFeedBagPrice = 220.0,
            isDemoFarm = true
        )
        database.farmDao().insertFarm(demoFarm)
        database.userDao().updateActiveFarm(demoUserId, demoFarmId)
        selectActiveFarm(demoFarm)

        // Seed sample records ONLY in this demo farm
        val whiteLayersId = UUID.randomUUID().toString()
        database.flockDao().insertFlock(
            FlockEntity(
                id = whiteLayersId,
                farmId = demoFarmId,
                userId = demoUserId,
                name = "White Layers (Sample)",
                birdType = "LAYERS",
                breed = "Lohmann Brown",
                initialCount = 250,
                currentCount = 250,
                isLayingFlock = true,
                ageWeeks = 28,
                notes = "Sample layer flock for demo exploration",
                syncStatus = SyncStatus.SYNCED
            )
        )
        database.flockDao().insertFlock(
            FlockEntity(
                id = UUID.randomUUID().toString(),
                farmId = demoFarmId,
                userId = demoUserId,
                name = "Turkeys (Sample)",
                birdType = "TURKEYS",
                breed = "Broad Breasted Bronze",
                initialCount = 20,
                currentCount = 20,
                isLayingFlock = false,
                ageWeeks = 18,
                notes = "Sample meat flock",
                syncStatus = SyncStatus.SYNCED
            )
        )

        val layersMashId = UUID.randomUUID().toString()
        database.feedDao().insertInventory(
            FeedInventoryEntity(
                id = layersMashId,
                farmId = demoFarmId,
                userId = demoUserId,
                feedType = "Layers Mash",
                brand = "Sample Brand",
                bagsInStock = 14.0f,
                bagWeightKg = 50.0f,
                reorderThresholdBags = 5.0f,
                costPerBag = 220.0,
                lastRestockedDate = "Demo Baseline",
                syncStatus = SyncStatus.SYNCED
            )
        )

        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        database.eggProductionDao().insertProduction(
            EggProductionEntity(
                id = UUID.randomUUID().toString(),
                farmId = demoFarmId,
                userId = demoUserId,
                dateString = todayStr,
                period = "MORNING",
                flockId = whiteLayersId,
                flockName = "White Layers (Sample)",
                cratesCollected = 6,
                looseEggsCollected = 5,
                brokenDamagedEggs = 1,
                totalEggs = 185,
                layingPercentage = 74.0f,
                notes = "Sample collection for demo inspection",
                syncStatus = SyncStatus.SYNCED
            )
        )

        database.customerDao().insertCustomer(
            CustomerEntity(
                id = UUID.randomUUID().toString(),
                farmId = demoFarmId,
                userId = demoUserId,
                name = "Sample Customer",
                phone = "0240000000",
                notes = "Demo contact",
                totalPurchases = 0.0,
                outstandingBalance = 0.0,
                syncStatus = SyncStatus.SYNCED
            )
        )

        return Pair(demoUser, demoFarm)
    }

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
