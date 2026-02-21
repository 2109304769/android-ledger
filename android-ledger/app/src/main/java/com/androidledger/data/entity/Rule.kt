package com.androidledger.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Rule")
data class Rule(
    @PrimaryKey val id: String,
    val pattern: String,
    val targetCategoryId: String,
    val targetSourceId: String?,
    val priority: Int,
    val isRegex: Int
)
