package com.androidledger.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.androidledger.data.entity.Transaction
import kotlinx.coroutines.flow.Flow

data class CategoryExpense(
    val categoryId: String,
    val total: Long
)

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions")
    fun getAll(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getById(id: String): Flow<Transaction?>

    @Query("SELECT * FROM transactions WHERE profileId = :profileId")
    fun getByProfileId(profileId: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE sourceId = :sourceId")
    fun getBySourceId(sourceId: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE occurredAt >= :startTime AND occurredAt <= :endTime")
    fun getByDateRange(startTime: Long, endTime: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE profileId = :profileId AND occurredAt >= :startTime AND occurredAt <= :endTime")
    fun getByProfileAndDateRange(profileId: String, startTime: Long, endTime: Long): Flow<List<Transaction>>

    @Query("SELECT SUM(amountMinor) FROM transactions WHERE profileId = :profileId AND direction = 'OUT' AND isConfirmed = 1 AND occurredAt >= :startTime AND occurredAt <= :endTime")
    fun getMonthlyExpense(profileId: String, startTime: Long, endTime: Long): Flow<Long?>

    @Query("SELECT SUM(amountMinor) FROM transactions WHERE profileId = :profileId AND direction = 'IN' AND isConfirmed = 1 AND occurredAt >= :startTime AND occurredAt <= :endTime")
    fun getMonthlyIncome(profileId: String, startTime: Long, endTime: Long): Flow<Long?>

    @Query("SELECT COUNT(*) FROM transactions WHERE isConfirmed = 0")
    fun getUnconfirmedCount(): Flow<Int>

    @Query("SELECT * FROM transactions WHERE merchant LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun search(query: String): Flow<List<Transaction>>

    @Insert
    suspend fun insert(transaction: Transaction)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(transaction: Transaction)

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("SELECT * FROM transactions ORDER BY occurredAt DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE profileId = :profileId ORDER BY occurredAt DESC LIMIT :limit")
    fun getByProfileRecentTransactions(profileId: String, limit: Int): Flow<List<Transaction>>

    @Query("SELECT categoryId, SUM(amountMinor) AS total FROM transactions WHERE profileId = :profileId AND direction = 'OUT' AND isConfirmed = 1 AND occurredAt >= :startTime AND occurredAt <= :endTime AND categoryId IS NOT NULL GROUP BY categoryId")
    fun getCategoryExpenses(profileId: String, startTime: Long, endTime: Long): Flow<List<CategoryExpense>>
}
