package com.androidledger.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.androidledger.data.entity.ExchangeRate
import kotlinx.coroutines.flow.Flow

@Dao
interface ExchangeRateDao {

    @Query("SELECT * FROM exchange_rate")
    fun getAll(): Flow<List<ExchangeRate>>

    @Query("SELECT * FROM exchange_rate WHERE fromCurrency = :fromCurrency AND toCurrency = :toCurrency LIMIT 1")
    fun getRate(fromCurrency: String, toCurrency: String): Flow<ExchangeRate?>

    @Insert
    suspend fun insert(rate: ExchangeRate)

    @Update
    suspend fun update(rate: ExchangeRate)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(rate: ExchangeRate)
}
