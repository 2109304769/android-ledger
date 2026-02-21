package com.androidledger.data.repository

import com.androidledger.data.dao.ExchangeRateDao
import com.androidledger.data.dao.RuleDao
import com.androidledger.data.dao.TagDao
import com.androidledger.data.entity.ExchangeRate
import com.androidledger.data.entity.Rule
import com.androidledger.data.entity.Tag
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val ruleDao: RuleDao,
    private val tagDao: TagDao,
    private val exchangeRateDao: ExchangeRateDao
) {

    // ---- Rules ----

    fun getAllRules(): Flow<List<Rule>> = ruleDao.getAll()

    suspend fun addRule(rule: Rule) = ruleDao.insert(rule)

    suspend fun updateRule(rule: Rule) = ruleDao.update(rule)

    suspend fun deleteRule(rule: Rule) = ruleDao.delete(rule)

    // ---- Tags ----

    fun getAllTags(): Flow<List<Tag>> = tagDao.getAll()

    suspend fun addTag(tag: Tag) = tagDao.insert(tag)

    suspend fun updateTag(tag: Tag) = tagDao.update(tag)

    suspend fun deleteTag(tag: Tag) = tagDao.delete(tag)

    // ---- Exchange Rates ----

    fun getRate(fromCurrency: String, toCurrency: String): Flow<ExchangeRate?> =
        exchangeRateDao.getRate(fromCurrency, toCurrency)

    fun getAllExchangeRates(): Flow<List<ExchangeRate>> = exchangeRateDao.getAll()

    suspend fun updateRate(exchangeRate: ExchangeRate) =
        exchangeRateDao.insertOrUpdate(exchangeRate)
}
