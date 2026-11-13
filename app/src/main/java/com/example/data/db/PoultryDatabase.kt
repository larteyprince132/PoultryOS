package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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

@Database(
    entities = [
        UserEntity::class,
        FarmEntity::class,
        FlockEntity::class,
        EggProductionEntity::class,
        FeedInventoryEntity::class,
        FeedLogEntity::class,
        SaleEntity::class,
        ExpenseEntity::class,
        CustomerEntity::class,
        HealthLogEntity::class,
        SyncAuditEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PoultryDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun farmDao(): FarmDao
    abstract fun flockDao(): FlockDao
    abstract fun eggProductionDao(): EggProductionDao
    abstract fun feedDao(): FeedDao
    abstract fun saleDao(): SaleDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun customerDao(): CustomerDao
    abstract fun healthDao(): HealthDao
    abstract fun syncAuditDao(): SyncAuditDao

    companion object {
        @Volatile
        private var INSTANCE: PoultryDatabase? = null

        fun getDatabase(context: Context): PoultryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PoultryDatabase::class.java,
                    "poultry_os_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
