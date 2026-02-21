package com.androidledger.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exchange_rate")
data class ExchangeRate(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "fromCurrency")
    val fromCurrency: String,

    @ColumnInfo(name = "toCurrency")
    val toCurrency: String,

    @ColumnInfo(name = "rate")
    val rate: Double,

    @ColumnInfo(name = "updatedAt")
    val updatedAt: Long
)
