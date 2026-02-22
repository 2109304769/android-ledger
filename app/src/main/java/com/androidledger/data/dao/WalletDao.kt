package com.androidledger.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.androidledger.data.entity.Wallet
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {

    @Query("SELECT * FROM wallet")
    fun getAll(): Flow<List<Wallet>>

    @Query("SELECT * FROM wallet WHERE profileId = :profileId")
    fun getByProfileId(profileId: String): Flow<List<Wallet>>

    @Query("SELECT * FROM wallet WHERE id = :id")
    fun getById(id: String): Flow<Wallet?>

    @Query("SELECT * FROM wallet WHERE id = :id")
    suspend fun getByIdSync(id: String): Wallet?

    @Insert
    suspend fun insert(wallet: Wallet)

    @Update
    suspend fun update(wallet: Wallet)

    @Delete
    suspend fun delete(wallet: Wallet)
}
