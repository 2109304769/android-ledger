package com.androidledger.data.repository

import com.androidledger.data.dao.CategoryDao
import com.androidledger.data.entity.Category
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) {

    fun getAll(): Flow<List<Category>> = categoryDao.getAll()

    fun getByType(type: String): Flow<List<Category>> = categoryDao.getByType(type)

    fun getById(id: String): Flow<Category?> = categoryDao.getById(id)

    suspend fun add(category: Category) = categoryDao.insert(category)

    suspend fun update(category: Category) = categoryDao.update(category)

    suspend fun delete(category: Category) = categoryDao.delete(category)
}
