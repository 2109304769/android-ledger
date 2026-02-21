package com.androidledger.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.androidledger.data.entity.Tag
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Query("SELECT * FROM tag")
    fun getAll(): Flow<List<Tag>>

    @Query("SELECT * FROM tag WHERE id = :id")
    fun getById(id: String): Flow<Tag?>

    @Insert
    suspend fun insert(tag: Tag)

    @Update
    suspend fun update(tag: Tag)

    @Delete
    suspend fun delete(tag: Tag)
}
