package com.androidledger.di

import android.content.Context
import androidx.room.Room
import com.androidledger.data.dao.CategoryDao
import com.androidledger.data.dao.ExchangeRateDao
import com.androidledger.data.dao.ProfileDao
import com.androidledger.data.dao.RuleDao
import com.androidledger.data.dao.SourceDao
import com.androidledger.data.dao.TagDao
import com.androidledger.data.dao.TransactionDao
import com.androidledger.data.dao.WalletDao
import com.androidledger.data.database.AppDatabase
import com.androidledger.data.database.DatabaseCallback
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        categoryDaoProvider: Provider<CategoryDao>,
        exchangeRateDaoProvider: Provider<ExchangeRateDao>
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "android_ledger.db"
        )
            .addCallback(
                DatabaseCallback(
                    categoryDaoProvider = categoryDaoProvider,
                    exchangeRateDaoProvider = exchangeRateDaoProvider,
                    scope = CoroutineScope(SupervisorJob())
                )
            )
            .build()
    }

    @Provides
    fun provideProfileDao(database: AppDatabase): ProfileDao {
        return database.profileDao()
    }

    @Provides
    fun provideWalletDao(database: AppDatabase): WalletDao {
        return database.walletDao()
    }

    @Provides
    fun provideSourceDao(database: AppDatabase): SourceDao {
        return database.sourceDao()
    }

    @Provides
    fun provideTransactionDao(database: AppDatabase): TransactionDao {
        return database.transactionDao()
    }

    @Provides
    fun provideCategoryDao(database: AppDatabase): CategoryDao {
        return database.categoryDao()
    }

    @Provides
    fun provideRuleDao(database: AppDatabase): RuleDao {
        return database.ruleDao()
    }

    @Provides
    fun provideTagDao(database: AppDatabase): TagDao {
        return database.tagDao()
    }

    @Provides
    fun provideExchangeRateDao(database: AppDatabase): ExchangeRateDao {
        return database.exchangeRateDao()
    }
}
