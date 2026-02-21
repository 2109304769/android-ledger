package com.androidledger.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.androidledger.data.dao.CategoryDao
import com.androidledger.data.dao.ExchangeRateDao
import com.androidledger.data.dao.ProfileDao
import com.androidledger.data.dao.RuleDao
import com.androidledger.data.dao.SourceDao
import com.androidledger.data.dao.TagDao
import com.androidledger.data.dao.TransactionDao
import com.androidledger.data.dao.WalletDao
import com.androidledger.data.entity.Category
import com.androidledger.data.entity.ExchangeRate
import com.androidledger.data.entity.Profile
import com.androidledger.data.entity.Rule
import com.androidledger.data.entity.Source
import com.androidledger.data.entity.Tag
import com.androidledger.data.entity.Transaction
import com.androidledger.data.entity.Wallet

@Database(
    entities = [
        Profile::class,
        Wallet::class,
        Source::class,
        Transaction::class,
        Category::class,
        Rule::class,
        Tag::class,
        ExchangeRate::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun profileDao(): ProfileDao

    abstract fun walletDao(): WalletDao

    abstract fun sourceDao(): SourceDao

    abstract fun transactionDao(): TransactionDao

    abstract fun categoryDao(): CategoryDao

    abstract fun ruleDao(): RuleDao

    abstract fun tagDao(): TagDao

    abstract fun exchangeRateDao(): ExchangeRateDao
}
