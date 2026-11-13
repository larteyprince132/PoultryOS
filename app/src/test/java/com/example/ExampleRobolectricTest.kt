package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.PoultryDatabase
import com.example.data.model.CustomerEntity
import com.example.data.model.EggProductionEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.FarmEntity
import com.example.data.model.FlockEntity
import com.example.data.model.SaleEntity
import com.example.data.model.SyncStatus
import com.example.data.model.UserEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var database: PoultryDatabase
    private lateinit var context: Context

    private val testUserId = "user_alpha_1"
    private val testFarmId1 = "farm_alpha_1"
    private val testFarmId2 = "farm_alpha_2"

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PoultryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `read string from context matches PoultryOS`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("PoultryOS", appName)
    }

    @Test
    fun `test initial database is completely clean`() = runBlocking {
        val user = database.userDao().getUserById("any_user")
        val flocks = database.flockDao().getAllFlocks(testFarmId1, testUserId).first()
        val eggs = database.eggProductionDao().getAllProductions(testFarmId1, testUserId).first()
        val sales = database.saleDao().getAllSales(testFarmId1, testUserId).first()
        val expenses = database.expenseDao().getAllExpenses(testFarmId1, testUserId).first()

        assertEquals(null, user)
        assertEquals(0, flocks.size)
        assertEquals(0, eggs.size)
        assertEquals(0, sales.size)
        assertEquals(0, expenses.size)
    }

    @Test
    fun `test multi-user and multi-farm isolation`() = runBlocking {
        // Register user 1 with Farm 1
        val user1 = UserEntity(
            id = testUserId,
            fullName = "Kwame Mensah",
            emailOrPhone = "kwame@farm.gh",
            passwordHash = "hash123"
        )
        database.userDao().insertUser(user1)

        val farm1 = FarmEntity(
            id = testFarmId1,
            userId = testUserId,
            farmName = "Sunrise Haven Farm",
            location = "Sunyani, Ghana",
            farmType = "Layers",
            currencySymbol = "GHS"
        )
        database.farmDao().insertFarm(farm1)

        // Register user 2 with Farm 2
        val user2Id = "user_beta_2"
        val user2 = UserEntity(
            id = user2Id,
            fullName = "Ama Serwaa",
            emailOrPhone = "ama@poultry.gh",
            passwordHash = "hash456"
        )
        database.userDao().insertUser(user2)

        val farm2 = FarmEntity(
            id = testFarmId2,
            userId = user2Id,
            farmName = "Golden Harvest Poultry",
            location = "Kumasi, Ghana",
            farmType = "Broilers",
            currencySymbol = "USD"
        )
        database.farmDao().insertFarm(farm2)

        // Insert flock into Farm 1
        val flock1 = FlockEntity(
            id = UUID.randomUUID().toString(),
            farmId = testFarmId1,
            userId = testUserId,
            name = "White Leghorns",
            birdType = "LAYERS",
            initialCount = 300,
            currentCount = 300,
            isLayingFlock = true
        )
        database.flockDao().insertFlock(flock1)

        // Insert flock into Farm 2
        val flock2 = FlockEntity(
            id = UUID.randomUUID().toString(),
            farmId = testFarmId2,
            userId = user2Id,
            name = "Cobb 500 Broilers",
            birdType = "BROILERS",
            initialCount = 500,
            currentCount = 500,
            isLayingFlock = false
        )
        database.flockDao().insertFlock(flock2)

        // Verify Strict Farm 1 and User 1 isolation
        val farm1Flocks = database.flockDao().getAllFlocks(testFarmId1, testUserId).first()
        assertEquals(1, farm1Flocks.size)
        assertEquals("White Leghorns", farm1Flocks[0].name)

        // Querying with User 1 on Farm 2 yields 0 records (access control)
        val unauthorizedQuery = database.flockDao().getAllFlocks(testFarmId2, testUserId).first()
        assertEquals(0, unauthorizedQuery.size)

        // Verify User 2 queries only their farm
        val farm2Flocks = database.flockDao().getAllFlocks(testFarmId2, user2Id).first()
        assertEquals(1, farm2Flocks.size)
        assertEquals("Cobb 500 Broilers", farm2Flocks[0].name)
    }

    @Test
    fun `test offline egg production recording and pending sync status`() = runBlocking {
        val flockId = UUID.randomUUID().toString()
        val flock = FlockEntity(
            id = flockId,
            farmId = testFarmId1,
            userId = testUserId,
            name = "White Layers",
            birdType = "LAYERS",
            initialCount = 250,
            currentCount = 250,
            isLayingFlock = true
        )
        database.flockDao().insertFlock(flock)

        val crates = 6
        val totalEggs = crates * 30
        val layingRate = (totalEggs.toFloat() / flock.currentCount.toFloat()) * 100f

        val eggRecord = EggProductionEntity(
            id = UUID.randomUUID().toString(),
            farmId = testFarmId1,
            userId = testUserId,
            dateString = "2026-11-12",
            period = "MORNING",
            flockId = flockId,
            flockName = flock.name,
            cratesCollected = crates,
            looseEggsCollected = 0,
            brokenDamagedEggs = 2,
            totalEggs = totalEggs,
            layingPercentage = layingRate,
            syncStatus = SyncStatus.PENDING
        )
        database.eggProductionDao().insertProduction(eggRecord)

        val allRecords = database.eggProductionDao().getAllProductions(testFarmId1, testUserId).first()
        assertEquals(1, allRecords.size)
        assertEquals(6, allRecords[0].cratesCollected)
        assertEquals(180, allRecords[0].totalEggs)
        assertEquals(72.0f, allRecords[0].layingPercentage, 0.01f)
        assertEquals(SyncStatus.PENDING, allRecords[0].syncStatus)

        // Simulate sync completion
        database.eggProductionDao().markProductionsSynced(listOf(eggRecord.id))
        val syncedRecords = database.eggProductionDao().getAllProductions(testFarmId1, testUserId).first()
        assertEquals(SyncStatus.SYNCED, syncedRecords[0].syncStatus)
    }

    @Test
    fun `test sales and customer balance tracking with partial payments`() = runBlocking {
        val customerId = UUID.randomUUID().toString()
        val customer = CustomerEntity(
            id = customerId,
            farmId = testFarmId1,
            userId = testUserId,
            name = "Mama Akosua",
            phone = "0244123456",
            outstandingBalance = 0.0
        )
        database.customerDao().insertCustomer(customer)

        val crates = 5f
        val unitPrice = 35.0
        val total = crates * unitPrice
        val paid = 100.0
        val debt = total - paid

        val sale = SaleEntity(
            id = UUID.randomUUID().toString(),
            farmId = testFarmId1,
            userId = testUserId,
            dateString = "2026-11-12",
            itemType = "EGG_CRATE",
            quantity = crates,
            unitPrice = unitPrice,
            totalAmount = total,
            amountPaid = paid,
            paymentStatus = "PARTIAL",
            customerId = customerId,
            customerName = customer.name,
            syncStatus = SyncStatus.PENDING
        )
        database.saleDao().insertSale(sale)

        database.customerDao().updateCustomer(
            customer.copy(outstandingBalance = customer.outstandingBalance + debt)
        )

        val updatedCustomer = database.customerDao().getCustomerById(customerId, testFarmId1)
        assertNotNull(updatedCustomer)
        assertEquals(75.0, updatedCustomer!!.outstandingBalance, 0.01)

        val allSales = database.saleDao().getAllSales(testFarmId1, testUserId).first()
        assertEquals(1, allSales.size)
        assertEquals("PARTIAL", allSales[0].paymentStatus)
    }

    @Test
    fun `test expense recording and profit calculation`() = runBlocking {
        val sale = SaleEntity(
            id = UUID.randomUUID().toString(),
            farmId = testFarmId1,
            userId = testUserId,
            dateString = "2026-11-12",
            itemType = "EGG_CRATE",
            quantity = 10f,
            unitPrice = 35.0,
            totalAmount = 350.0,
            amountPaid = 350.0,
            paymentStatus = "PAID",
            syncStatus = SyncStatus.PENDING
        )
        database.saleDao().insertSale(sale)

        val expense = ExpenseEntity(
            id = UUID.randomUUID().toString(),
            farmId = testFarmId1,
            userId = testUserId,
            dateString = "2026-11-12",
            category = "FEED",
            amount = 220.0,
            paymentMethod = "MOMO",
            description = "1 bag Layers Mash",
            syncStatus = SyncStatus.PENDING
        )
        database.expenseDao().insertExpense(expense)

        val totalRev = database.saleDao().getAllSales(testFarmId1, testUserId).first().sumOf { it.totalAmount }
        val totalExp = database.expenseDao().getAllExpenses(testFarmId1, testUserId).first().sumOf { it.amount }
        val operatingResult = totalRev - totalExp

        assertEquals(350.0, totalRev, 0.01)
        assertEquals(220.0, totalExp, 0.01)
        assertEquals(130.0, operatingResult, 0.01)
    }

    @Test
    fun testRegistrationLoginLogoutAndRecovery() = runBlocking {
        val sessionScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        val sessionManager = com.example.data.session.UserSessionManager(database, context, sessionScope)

        // 1. Registration
        val regResult = sessionManager.registerUser(
            fullName = "Kofi Asante",
            emailOrPhone = "kofi@farmghana.com",
            passwordPlain = "Secret123"
        )
        assertTrue(regResult.isSuccess)
        val user = regResult.getOrThrow()
        assertEquals("Kofi Asante", user.fullName)

        // 2. Initial state before creating farm
        assertEquals(user.id, sessionManager.currentUser.value?.id)
        assertEquals(null, sessionManager.activeFarm.value)

        // 3. Create first farm during onboarding
        val farm = sessionManager.createFarm(
            userId = user.id,
            farmName = "Asante Green Acres",
            location = "Sunyani",
            farmType = "Commercial Layers",
            currencySymbol = "GHS",
            eggCollectionUnit = "CRATES",
            eggsPerCrate = 30
        )
        assertEquals("Asante Green Acres", farm.farmName)
        assertEquals(farm.id, sessionManager.activeFarm.value?.id)

        // 4. Logout
        sessionManager.logout()
        assertEquals(null, sessionManager.currentUser.value)
        assertEquals(null, sessionManager.activeFarm.value)

        // 5. Login with invalid password fails
        val badLogin = sessionManager.loginUser("kofi@farmghana.com", "WrongPassword")
        assertTrue(badLogin.isFailure)

        // 6. Password recovery
        val recoveryResult = sessionManager.recoverPassword("kofi@farmghana.com", "NewSecret456")
        assertTrue(recoveryResult.isSuccess)

        // 7. Login with old password fails
        val oldLogin = sessionManager.loginUser("kofi@farmghana.com", "Secret123")
        assertTrue(oldLogin.isFailure)

        // 8. Login with new password succeeds and restores active farm
        val successLogin = sessionManager.loginUser("kofi@farmghana.com", "NewSecret456")
        assertTrue(successLogin.isSuccess)
        assertEquals(user.id, sessionManager.currentUser.value?.id)
        assertEquals(farm.id, sessionManager.activeFarm.value?.id)

        // 9. Add a second farm and switch active farms
        val farm2 = sessionManager.createFarm(
            userId = user.id,
            farmName = "Asante Broiler Unit",
            location = "Techiman",
            farmType = "Broilers",
            currencySymbol = "USD"
        )
        val userFarms = database.farmDao().getFarmsForUserSync(user.id)
        assertEquals(2, userFarms.size)
        assertEquals(farm2.id, sessionManager.activeFarm.value?.id)

        // Switch back to farm 1
        sessionManager.selectActiveFarm(farm)
        assertEquals(farm.id, sessionManager.activeFarm.value?.id)
    }

    @Test
    fun testTwoIndependentFarmersIsolation() = runBlocking {
        val sessionScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        val sessionManager = com.example.data.session.UserSessionManager(database, context, sessionScope)

        // Farmer 1
        val farmer1 = sessionManager.registerUser("Farmer One", "farmer1@gh.com", "pass1").getOrThrow()
        val farm1 = sessionManager.createFarm(farmer1.id, "Farmer 1 Layers", "Kumasi", "Layers")

        // Farmer 2
        val farmer2 = sessionManager.registerUser("Farmer Two", "farmer2@gh.com", "pass2").getOrThrow()
        val farm2 = sessionManager.createFarm(farmer2.id, "Farmer 2 Turkeys", "Tamale", "Turkeys")

        // Insert records for Farmer 1
        database.flockDao().insertFlock(
            FlockEntity(
                id = UUID.randomUUID().toString(),
                farmId = farm1.id,
                userId = farmer1.id,
                name = "F1 Layers",
                birdType = "LAYERS",
                initialCount = 200,
                currentCount = 200
            )
        )

        // Insert records for Farmer 2
        database.flockDao().insertFlock(
            FlockEntity(
                id = UUID.randomUUID().toString(),
                farmId = farm2.id,
                userId = farmer2.id,
                name = "F2 Turkeys",
                birdType = "TURKEYS",
                initialCount = 45,
                currentCount = 45
            )
        )

        // Verify Farmer 1 queries
        val f1Flocks = database.flockDao().getAllFlocks(farm1.id, farmer1.id).first()
        assertEquals(1, f1Flocks.size)
        assertEquals("F1 Layers", f1Flocks[0].name)

        // Verify Farmer 1 cannot see Farmer 2 flock even if providing farm2 ID
        val leakAttempt = database.flockDao().getAllFlocks(farm2.id, farmer1.id).first()
        assertEquals(0, leakAttempt.size)

        // Verify Farmer 2 queries
        val f2Flocks = database.flockDao().getAllFlocks(farm2.id, farmer2.id).first()
        assertEquals(1, f2Flocks.size)
        assertEquals("F2 Turkeys", f2Flocks[0].name)
    }

    @Test
    fun testFlockPopulationMortalityAndAdditions() = runBlocking {
        val flockId = UUID.randomUUID().toString()
        val flock = FlockEntity(
            id = flockId,
            farmId = testFarmId1,
            userId = testUserId,
            name = "Layers Batch A",
            birdType = "LAYERS",
            initialCount = 500,
            currentCount = 500,
            isLayingFlock = true
        )
        database.flockDao().insertFlock(flock)

        val syncManager = com.example.data.sync.SyncManager(
            database = database,
            networkMonitor = com.example.data.sync.NetworkMonitor(context),
            scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined),
            activeUserProvider = { testUserId },
            activeFarmProvider = { testFarmId1 }
        )
        val repository = com.example.data.repository.PoultryRepository(database, syncManager)

        // 1. Record mortality of 15 birds
        repository.recordFlockPopulationChange(flockId, testFarmId1, testUserId, "MORTALITY", 15, "Heat stress")
        val afterMortality = database.flockDao().getFlockById(flockId, testFarmId1)
        assertNotNull(afterMortality)
        assertEquals(485, afterMortality!!.currentCount)

        // Health log should have been automatically created
        val healthLogs = database.healthDao().getAllLogs(testFarmId1, testUserId).first()
        assertEquals(1, healthLogs.size)
        assertEquals("MORTALITY", healthLogs[0].logType)
        assertEquals(15, healthLogs[0].countAffected)

        // 2. Add new 50 birds to flock
        repository.recordFlockPopulationChange(flockId, testFarmId1, testUserId, "ADDITION", 50, "Restocked point of lay")
        val afterAddition = database.flockDao().getFlockById(flockId, testFarmId1)
        assertEquals(535, afterAddition!!.currentCount)

        // 3. Edit flock metadata
        repository.updateFlock(afterAddition.copy(breed = "Hy-Line Brown", notes = "Top producers"))
        val editedFlock = database.flockDao().getFlockById(flockId, testFarmId1)
        assertEquals("Hy-Line Brown", editedFlock!!.breed)

        // 4. Delete flock
        repository.deleteFlock(editedFlock)
        val deletedFlock = database.flockDao().getFlockById(flockId, testFarmId1)
        assertEquals(null, deletedFlock)
    }

    @Test
    fun testEggProductionInventoryAndDeletionCalculations() = runBlocking {
        val syncManager = com.example.data.sync.SyncManager(
            database = database,
            networkMonitor = com.example.data.sync.NetworkMonitor(context),
            scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined),
            activeUserProvider = { testUserId },
            activeFarmProvider = { testFarmId1 }
        )
        val repository = com.example.data.repository.PoultryRepository(database, syncManager)

        val flockId = UUID.randomUUID().toString()
        val flock = FlockEntity(
            id = flockId,
            farmId = testFarmId1,
            userId = testUserId,
            name = "Isa Browns",
            birdType = "LAYERS",
            initialCount = 300,
            currentCount = 300,
            isLayingFlock = true
        )
        database.flockDao().insertFlock(flock)

        // Record morning collection: 7 crates (210 eggs) + 15 loose = 225 total eggs. 3 damaged.
        repository.recordEggCollection(
            farmId = testFarmId1,
            userId = testUserId,
            dateString = "2026-11-12",
            period = "MORNING",
            flockId = flockId,
            flockName = flock.name,
            crates = 7,
            looseEggs = 15,
            brokenEggs = 3,
            activeLayingBirds = flock.currentCount,
            notes = "Clean collection"
        )

        val records = database.eggProductionDao().getAllProductions(testFarmId1, testUserId).first()
        assertEquals(1, records.size)
        val record = records[0]
        assertEquals(225, record.totalEggs)
        assertEquals(75.0f, record.layingPercentage, 0.01f)

        // Update record
        repository.updateEggProduction(record.copy(cratesCollected = 8, totalEggs = (8 * 30) + 15))
        val updatedRecords = database.eggProductionDao().getAllProductions(testFarmId1, testUserId).first()
        assertEquals(8, updatedRecords[0].cratesCollected)

        // Delete record
        repository.deleteEggProduction(updatedRecords[0])
        val emptyRecords = database.eggProductionDao().getAllProductions(testFarmId1, testUserId).first()
        assertEquals(0, emptyRecords.size)
    }

    @Test
    fun testFeedInventoryPurchaseConsumptionAndReversal() = runBlocking {
        val syncManager = com.example.data.sync.SyncManager(
            database = database,
            networkMonitor = com.example.data.sync.NetworkMonitor(context),
            scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined),
            activeUserProvider = { testUserId },
            activeFarmProvider = { testFarmId1 }
        )
        val repository = com.example.data.repository.PoultryRepository(database, syncManager)

        val feedId = UUID.randomUUID().toString()
        // Add initial 20 bags of Layers Mash
        repository.addFeedInventory(
            farmId = testFarmId1,
            userId = testUserId,
            feedType = "Layers Mash",
            brand = "Agric Feed Ltd",
            initialBags = 20.0f,
            costPerBag = 220.0,
            reorderThreshold = 5.0f
        )
        val invList = database.feedDao().getAllInventories(testFarmId1, testUserId).first()
        assertEquals(1, invList.size)
        val actualFeedId = invList[0].id
        assertEquals(20.0f, invList[0].bagsInStock, 0.01f)

        // Consume 3 bags
        repository.recordFeedConsumption(
            farmId = testFarmId1,
            userId = testUserId,
            dateString = "2026-11-12",
            feedTypeId = actualFeedId,
            flockId = "f1",
            flockName = "Flock 1",
            bagsQuantity = 3.0f,
            notes = "Daily afternoon ration"
        )

        val afterFeedInv = database.feedDao().getInventoryById(actualFeedId, testFarmId1)
        assertNotNull(afterFeedInv)
        assertEquals(17.0f, afterFeedInv!!.bagsInStock, 0.01f)

        // Purchase 10 more bags
        repository.recordFeedPurchase(
            farmId = testFarmId1,
            userId = testUserId,
            dateString = "2026-11-12",
            feedTypeId = actualFeedId,
            feedTypeNameFallback = "Layers Mash",
            bagsQuantity = 10.0f,
            costPerBag = 220.0,
            supplierName = "Kumasi Wholesaler",
            recordAsExpense = true,
            notes = "Restock"
        )

        val afterPurchaseInv = database.feedDao().getInventoryById(actualFeedId, testFarmId1)
        assertEquals(27.0f, afterPurchaseInv!!.bagsInStock, 0.01f)

        // Delete consumption log -> automatically restores 3 bags (27 + 3 = 30)
        val logs = database.feedDao().getAllLogs(testFarmId1, testUserId).first()
        val consumptionLog = logs.find { it.logType == "CONSUMPTION" }
        assertNotNull(consumptionLog)
        repository.deleteFeedLog(consumptionLog!!)

        val afterReversal = database.feedDao().getInventoryById(actualFeedId, testFarmId1)
        assertEquals(30.0f, afterReversal!!.bagsInStock, 0.01f)
    }

    @Test
    fun testHealthVaccinationAndReminders() = runBlocking {
        val syncManager = com.example.data.sync.SyncManager(
            database = database,
            networkMonitor = com.example.data.sync.NetworkMonitor(context),
            scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined),
            activeUserProvider = { testUserId },
            activeFarmProvider = { testFarmId1 }
        )
        val repository = com.example.data.repository.PoultryRepository(database, syncManager)

        // Record Gumboro vaccination with next due date
        repository.recordHealthLog(
            farmId = testFarmId1,
            userId = testUserId,
            dateString = "2026-11-12",
            logType = "VACCINATION",
            flockId = "flock_a",
            flockName = "Layers Batch 1",
            titleOrDrug = "Gumboro Vaccine (IBD)",
            countAffected = 250,
            administeredBy = "Dr. Mensah Vet",
            nextDueDate = "2026-11-26",
            notes = "Eye-drop method administered"
        )

        val healthLogs = database.healthDao().getAllLogs(testFarmId1, testUserId).first()
        assertEquals(1, healthLogs.size)
        val log = healthLogs[0]
        assertEquals("Gumboro Vaccine (IBD)", log.titleOrDrug)
        assertEquals("2026-11-26", log.nextDueDate)

        // Delete health log
        repository.deleteHealthLog(log)
        val emptyLogs = database.healthDao().getAllLogs(testFarmId1, testUserId).first()
        assertEquals(0, emptyLogs.size)
    }

    @Test
    fun testComprehensiveSalesCustomerDebtAndEggStockReduction() = runBlocking {
        val syncManager = com.example.data.sync.SyncManager(
            database = database,
            networkMonitor = com.example.data.sync.NetworkMonitor(context),
            scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined),
            activeUserProvider = { testUserId },
            activeFarmProvider = { testFarmId1 }
        )
        val repository = com.example.data.repository.PoultryRepository(database, syncManager)

        // 1. Add customer
        repository.addCustomer(testFarmId1, testUserId, "Kwame Wholesaler", "0240123456", "Egg distributor")
        val customers = database.customerDao().getAllCustomers(testFarmId1, testUserId).first()
        assertEquals(1, customers.size)
        val cust = customers[0]
        assertEquals(0.0, cust.outstandingBalance, 0.01)

        // 2. Add egg collection: 20 crates
        val flockId = UUID.randomUUID().toString()
        database.flockDao().insertFlock(
            FlockEntity(
                id = flockId,
                farmId = testFarmId1,
                userId = testUserId,
                name = "Isa Brown 1",
                birdType = "LAYERS",
                initialCount = 600,
                currentCount = 600,
                isLayingFlock = true
            )
        )
        repository.recordEggCollection(
            farmId = testFarmId1,
            userId = testUserId,
            dateString = "2026-11-12",
            period = "MORNING",
            flockId = flockId,
            flockName = "Isa Brown 1",
            crates = 20,
            looseEggs = 0,
            brokenEggs = 0,
            activeLayingBirds = 600,
            notes = "Fresh morning harvest"
        )

        // 3. Record partially paid sale: 10 crates at GH₵35 = GH₵350. Paid: GH₵200. Debt: GH₵150
        repository.recordSale(
            farmId = testFarmId1,
            userId = testUserId,
            dateString = "2026-11-12",
            itemType = "EGG_CRATE",
            quantity = 10f,
            unitPrice = 35.0,
            amountPaid = 200.0,
            customerName = cust.name,
            customerPhone = cust.phone,
            customerId = cust.id,
            notes = "10 crates on deposit"
        )

        val updatedCust = database.customerDao().getCustomerById(cust.id, testFarmId1)
        assertNotNull(updatedCust)
        assertEquals(150.0, updatedCust!!.outstandingBalance, 0.01)
        assertEquals(350.0, updatedCust.totalPurchases, 0.01)

        val sales = database.saleDao().getAllSales(testFarmId1, testUserId).first()
        assertEquals(1, sales.size)
        val sale = sales[0]
        assertEquals("PARTIAL", sale.paymentStatus)
        assertEquals(350.0, sale.totalAmount, 0.01)
        assertEquals(200.0, sale.amountPaid, 0.01)

        // 4. Settle customer balance with GH₵150 payment
        repository.settleCustomerBalance(cust.id, testFarmId1, testUserId, 150.0)
        val clearedCust = database.customerDao().getCustomerById(cust.id, testFarmId1)
        assertEquals(0.0, clearedCust!!.outstandingBalance, 0.01)

        // Settle payment creates a PAID transaction
        val allSales = database.saleDao().getAllSales(testFarmId1, testUserId).first()
        assertEquals(2, allSales.size)
        val totalRevenue = allSales.sumOf { it.totalAmount }
        val totalCashCollected = allSales.sumOf { it.amountPaid }
        assertEquals(500.0, totalRevenue, 0.01)
        assertEquals(350.0, totalCashCollected, 0.01) // 200 initial + 150 debt clearance
    }

    @Test
    fun testLiveBirdSaleDeductsFlockPopulation() = runBlocking {
        val syncManager = com.example.data.sync.SyncManager(
            database = database,
            networkMonitor = com.example.data.sync.NetworkMonitor(context),
            scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined),
            activeUserProvider = { testUserId },
            activeFarmProvider = { testFarmId1 }
        )
        val repository = com.example.data.repository.PoultryRepository(database, syncManager)

        val flockId = UUID.randomUUID().toString()
        val flock = FlockEntity(
            id = flockId,
            farmId = testFarmId1,
            userId = testUserId,
            name = "Meat Turkeys",
            birdType = "TURKEYS",
            initialCount = 50,
            currentCount = 50,
            isLayingFlock = false
        )
        database.flockDao().insertFlock(flock)

        // Sell 12 turkeys at GH₵150 each = GH₵1,800 fully paid
        repository.recordSale(
            farmId = testFarmId1,
            userId = testUserId,
            dateString = "2026-11-12",
            itemType = "LIVE_BIRD",
            quantity = 12f,
            unitPrice = 150.0,
            amountPaid = 1800.0,
            customerName = "Restaurant Buyer",
            customerPhone = "0209998888",
            customerId = null,
            notes = "Holiday batch",
            targetFlockId = flockId
        )

        // Verify flock population automatically reduced from 50 to 38
        val updatedFlock = database.flockDao().getFlockById(flockId, testFarmId1)
        assertNotNull(updatedFlock)
        assertEquals(38, updatedFlock!!.currentCount)
    }

    @Test
    fun testSaleDeletionReversesCustomerDebt() = runBlocking {
        val syncManager = com.example.data.sync.SyncManager(
            database = database,
            networkMonitor = com.example.data.sync.NetworkMonitor(context),
            scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined),
            activeUserProvider = { testUserId },
            activeFarmProvider = { testFarmId1 }
        )
        val repository = com.example.data.repository.PoultryRepository(database, syncManager)

        // Customer with 0 debt
        val custId = UUID.randomUUID().toString()
        database.customerDao().insertCustomer(
            CustomerEntity(
                id = custId,
                farmId = testFarmId1,
                userId = testUserId,
                name = "Aba Trading",
                outstandingBalance = 0.0
            )
        )

        // Unpaid sale of GH₵200
        repository.recordSale(
            farmId = testFarmId1,
            userId = testUserId,
            dateString = "2026-11-12",
            itemType = "EGG_CRATE",
            quantity = 5f,
            unitPrice = 40.0,
            amountPaid = 0.0,
            customerName = "Aba Trading",
            customerPhone = "",
            customerId = custId,
            notes = "Credit purchase"
        )
        val withDebt = database.customerDao().getCustomerById(custId, testFarmId1)
        assertEquals(200.0, withDebt!!.outstandingBalance, 0.01)

        // Delete sale -> debt reversed
        val sales = database.saleDao().getAllSales(testFarmId1, testUserId).first()
        assertEquals(1, sales.size)
        repository.deleteSale(sales[0])

        val afterDelete = database.customerDao().getCustomerById(custId, testFarmId1)
        assertEquals(0.0, afterDelete!!.outstandingBalance, 0.01)
    }
}
