package com.example.data.sync

import com.example.data.db.PoultryDatabase
import com.example.data.model.SyncAuditEntity
import com.example.data.model.SyncStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SyncUIState(
    val globalStatus: SyncStatus = SyncStatus.ERROR,
    val pendingCount: Int = 0,
    val isOnline: Boolean = true,
    val isSyncing: Boolean = false,
    val lastSyncTime: String = "Not synced yet",
    val lastSyncMessage: String = "Cloud sync is not configured. Records remain saved on this device.",
    val isSimulatedOffline: Boolean = false
)

class SyncManager(
    private val database: PoultryDatabase,
    private val networkMonitor: NetworkMonitor,
    private val scope: CoroutineScope,
    private val activeUserProvider: () -> String?,
    private val activeFarmProvider: () -> String?
) {
    private val _syncState = MutableStateFlow(SyncUIState())
    val syncState: StateFlow<SyncUIState> = _syncState.asStateFlow()

    init {
        // Monitor network
        scope.launch {
            networkMonitor.isOnline.collect { online ->
                val simulatedOffline = _syncState.value.isSimulatedOffline
                val effectiveOnline = online && !simulatedOffline
                _syncState.update { it.copy(isOnline = effectiveOnline) }
                if (effectiveOnline && _syncState.value.pendingCount > 0) {
                    performSync()
                }
            }
        }

        // Periodically refresh pending count
        scope.launch(Dispatchers.IO) {
            while (true) {
                refreshPendingCount()
                delay(3000)
            }
        }
    }

    suspend fun refreshPendingCount() {
        val currentUserId = activeUserProvider() ?: return
        val unsyncedFlocks = database.flockDao().getUnsyncedFlocks(currentUserId)
        val unsyncedProductions = database.eggProductionDao().getUnsyncedProductions(currentUserId)
        val unsyncedSales = database.saleDao().getUnsyncedSales(currentUserId)
        val unsyncedExpenses = database.expenseDao().getUnsyncedExpenses(currentUserId)
        val unsyncedFeedLogs = database.feedDao().getUnsyncedLogs(currentUserId)
        val unsyncedInventories = database.feedDao().getUnsyncedInventories(currentUserId)
        val unsyncedCustomers = database.customerDao().getUnsyncedCustomers(currentUserId)
        val unsyncedHealth = database.healthDao().getUnsyncedLogs(currentUserId)

        val totalPending = unsyncedFlocks.size +
                unsyncedProductions.size +
                unsyncedSales.size +
                unsyncedExpenses.size +
                unsyncedFeedLogs.size +
                unsyncedInventories.size +
                unsyncedCustomers.size +
                unsyncedHealth.size

        val hasConfirmedSync = _syncState.value.lastSyncTime != "Not synced yet"
        val newStatus = when {
            totalPending > 0 && _syncState.value.globalStatus == SyncStatus.ERROR -> SyncStatus.ERROR
            totalPending > 0 -> SyncStatus.PENDING
            hasConfirmedSync -> SyncStatus.SYNCED
            else -> SyncStatus.ERROR
        }

        _syncState.update { current ->
            current.copy(
                pendingCount = totalPending,
                globalStatus = if (current.globalStatus == SyncStatus.ERROR && totalPending > 0) SyncStatus.ERROR else newStatus
            )
        }
    }

    fun toggleOfflineSimulation() {
        val newSimulated = !_syncState.value.isSimulatedOffline
        val realOnline = networkMonitor.checkCurrentConnectivity()
        val effectiveOnline = realOnline && !newSimulated
        _syncState.update {
            it.copy(
                isSimulatedOffline = newSimulated,
                isOnline = effectiveOnline,
                lastSyncMessage = if (newSimulated) "Offline Simulation Mode enabled." else "Online mode restored."
            )
        }
        if (effectiveOnline) {
            triggerSync()
        }
    }

    fun triggerSync() {
        scope.launch(Dispatchers.IO) {
            performSync()
        }
    }

    private suspend fun performSync() {
        if (_syncState.value.isSyncing) return

        val currentUserId = activeUserProvider()
        val currentFarmId = activeFarmProvider()
        if (currentUserId == null || currentFarmId == null) return

        val isOnline = _syncState.value.isOnline
        if (!isOnline) {
            _syncState.update {
                it.copy(
                    lastSyncMessage = "Device is offline. All records are safely preserved in local SQLite."
                )
            }
            return
        }

        _syncState.update { it.copy(isSyncing = true, lastSyncMessage = "Synchronizing tenant records...") }

        try {
            val flocks = database.flockDao().getUnsyncedFlocks(currentUserId)
            val productions = database.eggProductionDao().getUnsyncedProductions(currentUserId)
            val sales = database.saleDao().getUnsyncedSales(currentUserId)
            val expenses = database.expenseDao().getUnsyncedExpenses(currentUserId)
            val feedLogs = database.feedDao().getUnsyncedLogs(currentUserId)
            val inventories = database.feedDao().getUnsyncedInventories(currentUserId)
            val customers = database.customerDao().getUnsyncedCustomers(currentUserId)
            val healthLogs = database.healthDao().getUnsyncedLogs(currentUserId)

            val batchSize = flocks.size + productions.size + sales.size + expenses.size +
                    feedLogs.size + inventories.size + customers.size + healthLogs.size

            if (batchSize == 0) {
                val hasConfirmedSync = _syncState.value.lastSyncTime != "Not synced yet"
                _syncState.update {
                    it.copy(
                        isSyncing = false,
                        globalStatus = if (hasConfirmedSync) SyncStatus.SYNCED else SyncStatus.ERROR,
                        lastSyncMessage = if (hasConfirmedSync) {
                            "All farm records up to date. No pending records."
                        } else {
                            "Cloud sync is not configured. Records remain saved on this device."
                        }
                    )
                }
                return
            }

            // This project does not yet have an authenticated cloud adapter. A network connection
            // alone is not evidence that these records reached durable cloud storage, so leave
            // every row pending until a backend confirms persistence.
            throw IllegalStateException(
                "Cloud sync is not configured. Records remain saved on this device."
            )

        } catch (e: Exception) {
            database.syncAuditDao().insertAudit(
                SyncAuditEntity(
                    farmId = currentFarmId,
                    userId = currentUserId,
                    operation = "TENANT_SYNC_FAILED",
                    recordsSynced = 0,
                    status = "FAILED",
                    details = e.message ?: "Sync error"
                )
            )
            _syncState.update {
                it.copy(
                    isSyncing = false,
                    globalStatus = SyncStatus.ERROR,
                    lastSyncMessage = "Sync needs attention: ${e.message}. Local records remain safe and can be retried."
                )
            }
        }
    }
}
