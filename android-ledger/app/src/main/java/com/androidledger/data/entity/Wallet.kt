package com.androidledger.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Wallet")
data class Wallet(
    @PrimaryKey val id: String,
    val profileId: String,
    val name: String,
    val currency: String,
    val sortOrder: Int,
    val createdAt: Long
)
