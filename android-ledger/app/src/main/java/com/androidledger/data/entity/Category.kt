package com.androidledger.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Category")
data class Category(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val color: String,
    val type: String, // "EXPENSE" / "INCOME" / "BOTH"
    val isDefault: Int
)
