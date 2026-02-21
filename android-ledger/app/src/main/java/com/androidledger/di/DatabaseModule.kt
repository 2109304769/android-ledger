package com.androidledger.di

import android.content.Context
import com.androidledger.data.database.LedgerDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LedgerDatabase {
        return LedgerDatabase.getDatabase(context)
    }

    @Provides
    fun provideProfileDao(db: LedgerDatabase) = db.profileDao()

    @Provides
    fun provideWalletDao(db: LedgerDatabase) = db.walletDao()

    @Provides
    fun provideSourceDao(db: LedgerDatabase) = db.sourceDao()

    @Provides
    fun provideTransactionDao(db: LedgerDatabase) = db.transactionDao()

    @Provides
    fun provideCategoryDao(db: LedgerDatabase) = db.categoryDao()
    
    @Provides
    fun provideRuleDao(db: LedgerDatabase) = db.ruleDao()

    @Provides
    fun provideTagDao(db: LedgerDatabase) = db.tagDao()

    @Provides
    fun provideExchangeRateDao(db: LedgerDatabase) = db.exchangeRateDao()
}
