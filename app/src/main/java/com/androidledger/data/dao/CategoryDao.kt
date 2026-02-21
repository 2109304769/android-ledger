package com.androidledger.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.androidledger.data.entity.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM category")
    fun getAll(): Flow<List<Category>>

    @Query("SELECT * FROM category WHERE type = :type")
    fun getByType(type: String): Flow<List<Category>>

    @Query("SELECT * FROM category WHERE id = :id")
    fun getById(id: String): Flow<Category?>

    @Insert
    suspend fun insert(category: Category)

    @Insert
    suspend fun insertAll(categories: List<Category>)

    @Update
    suspend fun update(category: Category)

    @Delete
    suspend fun delete(category: Category)

    @Query("SELECT COUNT(*) FROM category")
    suspend fun getCount(): Int
}
