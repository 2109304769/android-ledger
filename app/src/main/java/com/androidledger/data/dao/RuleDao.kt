package com.androidledger.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.androidledger.data.entity.Rule
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {

    @Query("SELECT * FROM rule")
    fun getAll(): Flow<List<Rule>>

    @Query("SELECT * FROM rule ORDER BY priority ASC")
    fun getAllSorted(): Flow<List<Rule>>

    @Query("SELECT * FROM rule WHERE id = :id")
    fun getById(id: String): Flow<Rule?>

    @Insert
    suspend fun insert(rule: Rule)

    @Update
    suspend fun update(rule: Rule)

    @Delete
    suspend fun delete(rule: Rule)
}
