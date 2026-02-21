package com.androidledger.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ExchangeRate")
data class ExchangeRate(
    @PrimaryKey val id: String,
    val fromCurrency: String,
    val toCurrency: String,
    val rate: Double,
    val updatedAt: Long
)
