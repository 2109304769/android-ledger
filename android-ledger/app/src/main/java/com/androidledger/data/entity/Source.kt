package com.androidledger.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Source")
data class Source(
    @PrimaryKey val id: String,
    val walletId: String,
    val name: String,
    val type: String, // "BANK" / "EWALLET" / "CASH" / "OTHER"
    val balanceSnapshot: Long,
    val balanceUpdatedAt: Long,
    val icon: String?,
    val isArchived: Int
)
