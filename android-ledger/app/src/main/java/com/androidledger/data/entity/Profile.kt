package com.androidledger.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Profile")
data class Profile(
    @PrimaryKey val id: String,
    val name: String,
    val type: String, // "PERSONAL" / "BUSINESS"
    val emoji: String,
    val isDefault: Int, // 1=默认账户（App启动时显示）
    val createdAt: Long
)
