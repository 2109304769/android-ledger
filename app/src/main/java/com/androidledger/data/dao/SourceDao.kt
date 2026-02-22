package com.androidledger.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.androidledger.data.entity.Source
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceDao {

    @Query("SELECT * FROM source")
    fun getAll(): Flow<List<Source>>

    @Query("SELECT * FROM source WHERE walletId = :walletId")
    fun getByWalletId(walletId: String): Flow<List<Source>>

    @Query("SELECT * FROM source WHERE id = :id")
    fun getById(id: String): Flow<Source?>

    @Query("SELECT * FROM source WHERE walletId = :walletId AND isArchived = 0")
    fun getActiveByWalletId(walletId: String): Flow<List<Source>>

    @Insert
    suspend fun insert(source: Source)

    @Update
    suspend fun update(source: Source)

    @Delete
    suspend fun delete(source: Source)

    @Query("UPDATE source SET balanceSnapshot = :balanceSnapshot, balanceUpdatedAt = :balanceUpdatedAt WHERE id = :id")
    suspend fun updateBalance(id: String, balanceSnapshot: Long, balanceUpdatedAt: Long)

    @Query("""
        SELECT source.* FROM source
        INNER JOIN wallet ON source.walletId = wallet.id
        WHERE wallet.currency = :currency AND source.isArchived = 0
        LIMIT 1
    """)
    suspend fun getFirstSourceByCurrency(currency: String): Source?
}
