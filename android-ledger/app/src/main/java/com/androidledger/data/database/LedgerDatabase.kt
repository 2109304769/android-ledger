package com.androidledger.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.androidledger.data.dao.*
import com.androidledger.data.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Database(
    entities = [
        Profile::class, Wallet::class, Source::class, Transaction::class,
        Category::class, Rule::class, Tag::class, ExchangeRate::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LedgerDatabase : RoomDatabase() {

    abstract fun profileDao(): ProfileDao
    abstract fun walletDao(): WalletDao
    abstract fun sourceDao(): SourceDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun ruleDao(): RuleDao
    abstract fun tagDao(): TagDao
    abstract fun exchangeRateDao(): ExchangeRateDao

    companion object {
        @Volatile
        private var INSTANCE: LedgerDatabase? = null

        fun getDatabase(context: Context): LedgerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LedgerDatabase::class.java,
                    "ledger_database"
                )
                .addCallback(LedgerDatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class LedgerDatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    prePopulateCategories(database.categoryDao())
                }
            }
        }

        suspend fun prePopulateCategories(categoryDao: CategoryDao) {
            val defaultCategories = listOf(
                Category(UUID.randomUUID().toString(), "餐饮", "🍜", "#FF5722", "EXPENSE", 1),
                Category(UUID.randomUUID().toString(), "交通", "🚗", "#2196F3", "EXPENSE", 1),
                Category(UUID.randomUUID().toString(), "购物", "🛒", "#E91E63", "EXPENSE", 1),
                Category(UUID.randomUUID().toString(), "居住", "🏠", "#795548", "EXPENSE", 1),
                Category(UUID.randomUUID().toString(), "医疗", "💊", "#F44336", "EXPENSE", 1),
                Category(UUID.randomUUID().toString(), "通讯", "📱", "#9C27B0", "EXPENSE", 1),
                Category(UUID.randomUUID().toString(), "娱乐", "🎮", "#673AB7", "EXPENSE", 1),
                Category(UUID.randomUUID().toString(), "旅行", "✈️", "#00BCD4", "EXPENSE", 1),
                Category(UUID.randomUUID().toString(), "教育", "📚", "#3F51B5", "EXPENSE", 1),
                Category(UUID.randomUUID().toString(), "工作相关", "💼", "#607D8B", "EXPENSE", 1),
                Category(UUID.randomUUID().toString(), "其他支出", "🔧", "#9E9E9E", "EXPENSE", 1),
                
                Category(UUID.randomUUID().toString(), "工资/薪资", "💰", "#4CAF50", "INCOME", 1),
                Category(UUID.randomUUID().toString(), "兼职收入", "💸", "#8BC34A", "INCOME", 1),
                Category(UUID.randomUUID().toString(), "投资收益", "📈", "#009688", "INCOME", 1),
                Category(UUID.randomUUID().toString(), "红包/礼金", "🎁", "#FFEB3B", "INCOME", 1),
                Category(UUID.randomUUID().toString(), "汇款", "💱", "#FFC107", "INCOME", 1),
                Category(UUID.randomUUID().toString(), "其他收入", "🔧", "#9E9E9E", "INCOME", 1)
            )
            categoryDao.insertAll(defaultCategories)
        }
    }
}
