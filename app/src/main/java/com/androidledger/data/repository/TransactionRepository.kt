package com.androidledger.data.repository

import com.androidledger.data.dao.CategoryExpense
import com.androidledger.data.dao.TransactionDao
import com.androidledger.data.entity.Transaction
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao
) {

    fun getAllTransactions(): Flow<List<Transaction>> = transactionDao.getAll()

    fun getTransactionById(id: String): Flow<Transaction?> = transactionDao.getById(id)

    fun getByProfile(profileId: String): Flow<List<Transaction>> =
        transactionDao.getByProfileId(profileId)

    fun getBySource(sourceId: String): Flow<List<Transaction>> =
        transactionDao.getBySourceId(sourceId)

    fun getByDateRange(
        profileId: String,
        startTime: Long,
        endTime: Long
    ): Flow<List<Transaction>> =
        transactionDao.getByProfileAndDateRange(profileId, startTime, endTime)

    fun getMonthlyExpense(
        profileId: String,
        startTime: Long,
        endTime: Long
    ): Flow<Long?> =
        transactionDao.getMonthlyExpense(profileId, startTime, endTime)

    fun getMonthlyIncome(
        profileId: String,
        startTime: Long,
        endTime: Long
    ): Flow<Long?> =
        transactionDao.getMonthlyIncome(profileId, startTime, endTime)

    fun getUnconfirmedCount(): Flow<Int> = transactionDao.getUnconfirmedCount()

    fun search(query: String): Flow<List<Transaction>> = transactionDao.search(query)

    suspend fun addTransaction(transaction: Transaction) =
        transactionDao.insert(transaction)

    suspend fun addTransactionOrIgnore(transaction: Transaction) =
        transactionDao.insertOrIgnore(transaction)

    suspend fun updateTransaction(transaction: Transaction) =
        transactionDao.update(transaction)

    suspend fun deleteTransaction(transaction: Transaction) =
        transactionDao.delete(transaction)

    fun getRecentTransactions(
        profileId: String,
        limit: Int = 20
    ): Flow<List<Transaction>> =
        transactionDao.getByProfileRecentTransactions(profileId, limit)

    fun getCategoryExpenses(
        profileId: String,
        startTime: Long,
        endTime: Long
    ): Flow<List<CategoryExpense>> =
        transactionDao.getCategoryExpenses(profileId, startTime, endTime)
}
