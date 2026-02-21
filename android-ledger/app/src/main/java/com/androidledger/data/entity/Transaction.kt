package com.androidledger.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "Transaction",
    indices = [
        Index(value = ["source", "externalId"], unique = true)
    ]
)
data class Transaction(
    @PrimaryKey val id: String,
    val profileId: String,
    val walletId: String,
    val sourceId: String,
    val occurredAt: Long,
    val amountMinor: Long,
    val currency: String,
    val direction: String, // "IN" / "OUT" / "TRANSFER"
    val merchant: String?,
    val description: String?,
    val categoryId: String?,
    val tagIds: String?, // 逗号分隔
    val source: String, // "MANUAL" / "CSV" / "NOTIFICATION"
    val externalId: String?,
    val isConfirmed: Int,
    val createdAt: Long
)
