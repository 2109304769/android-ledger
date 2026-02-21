package com.androidledger.data.dao

import androidx.room.*
import com.androidledger.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM Profile")
    fun getAll(): Flow<List<Profile>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: Profile)
    @Delete
    suspend fun delete(profile: Profile)
}

@Dao
interface WalletDao {
    @Query("SELECT * FROM Wallet WHERE profileId = :profileId ORDER BY sortOrder")
    fun getByProfile(profileId: String): Flow<List<Wallet>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(wallet: Wallet)
}

@Dao
interface SourceDao {
    @Query("SELECT * FROM Source WHERE walletId = :walletId AND isArchived = 0")
    fun getByWallet(walletId: String): Flow<List<Source>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(source: Source)
    @Update
    suspend fun update(source: Source)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM `Transaction` ORDER BY occurredAt DESC")
    fun getAll(): Flow<List<Transaction>>
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: Transaction): Long
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM Category")
    fun getAll(): Flow<List<Category>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<Category>)
}

@Dao
interface RuleDao {
    @Query("SELECT * FROM Rule ORDER BY priority ASC")
    fun getAll(): Flow<List<Rule>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: Rule)
}

@Dao
interface TagDao {
    @Query("SELECT * FROM Tag")
    fun getAll(): Flow<List<Tag>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tag: Tag)
}

@Dao
interface ExchangeRateDao {
    @Query("SELECT * FROM ExchangeRate WHERE fromCurrency = :from AND toCurrency = :to LIMIT 1")
    fun getRate(from: String, to: String): Flow<ExchangeRate?>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rate: ExchangeRate)
}
