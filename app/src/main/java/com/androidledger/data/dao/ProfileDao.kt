package com.androidledger.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.androidledger.data.entity.Profile
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ProfileDao {

    @Query("SELECT * FROM profile")
    abstract fun getAll(): Flow<List<Profile>>

    @Query("SELECT * FROM profile WHERE id = :id")
    abstract fun getById(id: String): Flow<Profile?>

    @Query("SELECT * FROM profile WHERE isDefault = 1 LIMIT 1")
    abstract fun getDefault(): Flow<Profile?>

    @Insert
    abstract suspend fun insert(profile: Profile)

    @Update
    abstract suspend fun update(profile: Profile)

    @Delete
    abstract suspend fun delete(profile: Profile)

    @Transaction
    open suspend fun setDefault(id: String) {
        clearDefault()
        markDefault(id)
    }

    @Query("UPDATE profile SET isDefault = 0 WHERE isDefault = 1")
    abstract suspend fun clearDefault()

    @Query("UPDATE profile SET isDefault = 1 WHERE id = :id")
    abstract suspend fun markDefault(id: String)
}
