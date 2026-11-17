package com.example.data.session

import android.content.Context
import android.content.SharedPreferences
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class UserSessionManager(
    private val database: PoultryDatabase,
    context: Context,
    private val scope: CoroutineScope
) {
    private val firebaseAuth: FirebaseAuth? =
        FirebaseApp.getApps(context).firstOrNull()?.let { FirebaseAuth.getInstance(it) }

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
            val isDemoSession = prefs.getBoolean("is_demo_session", false)
            val authenticatedEmail = firebaseAuth?.currentUser?.email
            val user = when {
                isDemoSession && savedUserId != null -> database.userDao().getUserById(savedUserId)
                !authenticatedEmail.isNullOrBlank() -> resolveLocalUser(authenticatedEmail, "")
                savedUserId != null -> database.userDao().getUserById(savedUserId)
                else -> null
            }

            if (user != null) {
                if (!isDemoSession && authenticatedEmail != null) saveActiveUser(user)
                else _currentUser.value = user

                val savedFarm = savedFarmId?.let { database.farmDao().getFarmByIdSync(it) }
                    ?.takeIf { it.userId == user.id }
                val activeFarm = savedFarm
                    ?: user.activeFarmId?.let { database.farmDao().getFarmByIdSync(it) }
                        ?.takeIf { it.userId == user.id }
                    ?: database.farmDao().getFarmsForUserSync(user.id).firstOrNull()
                _activeFarm.value = activeFarm
            } else if (!isDemoSession) {
                _activeFarm.value = null
                _currentUser.value = null
            }
            _isSessionLoaded.value = true
        }
    }

    suspend fun registerUser(
        fullName: String,
        emailOrPhone: String,
        passwordPlain: String
    ): Result<UserEntity> {
        val email = emailOrPhone.trim().lowercase(Locale.ROOT)
        return try {
            val auth = requireFirebaseAuth()
            auth.createUserWithEmailAndPassword(email, passwordPlain).awaitResult()
            val user = resolveLocalUser(email, fullName.trim())
            saveActiveUser(user)
            Result.success(user)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(Exception(friendlyAuthError(error)))
        }
    }

    suspend fun recoverPassword(emailOrPhone: String): Result<Unit> {
        val email = emailOrPhone.trim().lowercase(Locale.ROOT)
        return try {
            requireFirebaseAuth().sendPasswordResetEmail(email).awaitCompletion()
            Result.success(Unit)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(Exception(friendlyAuthError(error)))
        }
    }

    suspend fun loginUser(
        emailOrPhone: String,
        passwordPlain: String
    ): Result<UserEntity> {
        val email = emailOrPhone.trim().lowercase(Locale.ROOT)
        return try {
            val authResult = requireFirebaseAuth()
                .signInWithEmailAndPassword(email, passwordPlain)
                .awaitResult()
            val firebaseUser = authResult.user
                ?: throw IllegalStateException("Firebase signed in without returning an account.")
            val user = resolveLocalUser(
                email = firebaseUser.email ?: email,
                fullName = firebaseUser.displayName.orEmpty()
            )
            saveActiveUser(user)
            val userFarms = database.farmDao().getFarmsForUserSync(user.id)
            val farmToActivate = user.activeFarmId?.let { id -> userFarms.find { it.id == id } }
                ?: userFarms.firstOrNull()
            if (farmToActivate != null) selectActiveFarm(farmToActivate)
            else _activeFarm.value = null
            Result.success(user)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(Exception(friendlyAuthError(error)))
        }
    }

    private fun requireFirebaseAuth(): FirebaseAuth = firebaseAuth
        ?: throw IllegalStateException(
            "Firebase Authentication is not configured. Add this app's google-services.json file."
        )

    private suspend fun resolveLocalUser(email: String, fullName: String): UserEntity {
        val normalizedEmail = email.trim().lowercase(Locale.ROOT)
        val displayName = fullName.trim().ifBlank { normalizedEmail.substringBefore("@") }
        val existing = database.userDao().getUserByEmailOrPhone(normalizedEmail)
        if (existing != null) {
            val updated = existing.copy(
                fullName = existing.fullName.ifBlank { displayName },
                passwordHash = ""
            )
            if (updated != existing) database.userDao().updateUser(updated)
            return updated
        }

        return UserEntity(
            id = UUID.randomUUID().toString(),
            fullName = displayName,
            emailOrPhone = normalizedEmail,
            passwordHash = "",
            activeFarmId = null,
            createdAt = System.currentTimeMillis()
        ).also { database.userDao().insertUser(it) }
    }

    private fun friendlyAuthError(error: Exception): String {
        val message = when ((error as? FirebaseAuthException)?.errorCode) {
            "ERROR_INVALID_EMAIL" -> "Enter a valid email address."
            "ERROR_EMAIL_ALREADY_IN_USE" -> "An account already uses this email. Sign in instead."
            "ERROR_WEAK_PASSWORD" -> "Choose a password with at least 6 characters."
            "ERROR_USER_NOT_FOUND" -> "No account was found for this email."
            "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" -> "The email or password is incorrect."
            "ERROR_NETWORK_REQUEST_FAILED" -> "Connect to the internet to sign in or create an account."
            "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Wait a little and try again."
            else -> error.localizedMessage?.takeIf { it.isNotBlank() }
                ?: "Authentication failed. Please try again."
        }
        return message
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

    private fun saveActiveUser(user: UserEntity, isDemo: Boolean = false) {
        _currentUser.value = user
        prefs.edit()
            .putString("saved_user_id", user.id)
            .putBoolean("is_demo_session", isDemo)
            .apply()
    }

    fun logout() {
        firebaseAuth?.signOut()
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
            passwordHash = "",
            activeFarmId = null,
            createdAt = System.currentTimeMillis()
        )
        database.userDao().insertUser(demoUser)
        saveActiveUser(demoUser, isDemo = true)

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

}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { result ->
        if (continuation.isActive) continuation.resume(result)
    }
    addOnFailureListener { error ->
        if (continuation.isActive) continuation.resumeWithException(error)
    }
    addOnCanceledListener {
        if (continuation.isActive) continuation.cancel()
    }
}

private suspend fun Task<Void>.awaitCompletion(): Unit = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener {
        if (continuation.isActive) continuation.resume(Unit)
    }
    addOnFailureListener { error ->
        if (continuation.isActive) continuation.resumeWithException(error)
    }
    addOnCanceledListener {
        if (continuation.isActive) continuation.cancel()
    }
}
