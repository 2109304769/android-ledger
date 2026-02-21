package com.androidledger.data.database

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.androidledger.data.dao.CategoryDao
import com.androidledger.data.dao.ExchangeRateDao
import com.androidledger.data.entity.Category
import com.androidledger.data.entity.ExchangeRate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Provider

class DatabaseCallback(
    private val categoryDaoProvider: Provider<CategoryDao>,
    private val exchangeRateDaoProvider: Provider<ExchangeRateDao>,
    private val scope: CoroutineScope
) : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        scope.launch(Dispatchers.IO) {
            insertPresetCategories(categoryDaoProvider.get())
            insertDefaultExchangeRate(exchangeRateDaoProvider.get())
        }
    }

    private suspend fun insertPresetCategories(categoryDao: CategoryDao) {
        val expenseCategories = listOf(
            Category(
                id = UUID.randomUUID().toString(),
                name = "餐饮",
                icon = "\uD83C\uDF5C",
                color = "#FF6B6B",
                type = "EXPENSE",
                isDefault = 1
            ),
            Category(
                id = UUID.randomUUID().toString(),
                name = "交通",
                icon = "\uD83D\uDE97",
                color = "#4ECDC4",
                type = "EXPENSE",
                isDefault = 1
            ),
            Category(
                id = UUID.randomUUID().toString(),
                name = "购物",
                icon = "\uD83D\uDED2",
                color = "#45B7D1",
                type = "EXPENSE",
                isDefault = 1
            ),
            Category(
                id = UUID.randomUUID().toString(),
                name = "居住",
                icon = "\uD83C\uDFE0",
                color = "#96CEB4",
                type = "EXPENSE",
                isDefault = 1
            ),
            Category(
                id = UUID.randomUUID().toString(),
                name = "医疗",
                icon = "\uD83D\uDC8A",
                color = "#D4A574",
                type = "EXPENSE",
                isDefault = 1
            ),
            Category(
                id = UUID.randomUUID().toString(),
                name = "通讯",
                icon = "\uD83D\uDCF1",
                color = "#9B59B6",
                type = "EXPENSE",
                isDefault = 1
            ),
            Category(
                id = UUID.randomUUID().toString(),
                name = "娱乐",
                icon = "\uD83C\uDFAE",
                color = "#3498DB",
                type = "EXPENSE",
                isDefault = 1
            ),
            Category(
                id = UUID.randomUUID().toString(),
                name = "旅行",
                icon = "✈\uFE0F",
                color = "#E67E22",
                type = "EXPENSE",
                isDefault = 1
            ),
            Category(
                id = UUID.randomUUID().toString(),
                name = "教育",
                icon = "\uD83D\uDCDA",
                color = "#1ABC9C",
                type = "EXPENSE",
                isDefault = 1
            ),
            Category(
                id = UUID.randomUUID().toString(),
                name = "工作相关",
                icon = "\uD83D\uDCBC",
                color = "#34495E",
                type = "EXPENSE",
                isDefault = 1
            ),
            Category(
                id = UUID.randomUUID().toString(),
                name = "其他支出",
                icon = "\uD83D\uDD27",
                color = "#95A5A6",
                type = "EXPENSE",
                isDefault = 1
            )
        )

        val incomeCategories = listOf(
            Category(
                id = UUID.randomUUID().toString(),
                name = "工资/薪资",
                icon = "\uD83D\uDCB0",
                color = "#2ECC71",
                type = "INCOME",
                isDefault = 1
            ),
            Category(
                id = UUID.randomUUID().toString(),
                name = "兼职收入",
                icon = "\uD83D\uDCB8",
                color = "#27AE60",
                type = "INCOME",
                isDefault = 1
            ),
            Category(
                id = UUID.randomUUID().toString(),
                name = "投资收益",
                icon = "\uD83D\uDCC8",
                color = "#F39C12",
                type = "INCOME",
                isDefault = 1
            ),
            Category(
                id = UUID.randomUUID().toString(),
                name = "红包/礼金",
                icon = "\uD83C\uDF81",
                color = "#E74C3C",
                type = "INCOME",
                isDefault = 1
            ),
            Category(
                id = UUID.randomUUID().toString(),
                name = "汇款",
                icon = "\uD83D\uDCB1",
                color = "#8E44AD",
                type = "INCOME",
                isDefault = 1
            ),
            Category(
                id = UUID.randomUUID().toString(),
                name = "其他收入",
                icon = "\uD83D\uDD27",
                color = "#7F8C8D",
                type = "INCOME",
                isDefault = 1
            )
        )

        categoryDao.insertAll(expenseCategories + incomeCategories)
    }

    private suspend fun insertDefaultExchangeRate(exchangeRateDao: ExchangeRateDao) {
        val defaultRate = ExchangeRate(
            id = UUID.randomUUID().toString(),
            fromCurrency = "EUR",
            toCurrency = "CNY",
            rate = 7.85,
            updatedAt = System.currentTimeMillis()
        )
        exchangeRateDao.insert(defaultRate)
    }
}
