package com.androidledger.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Tag")
data class Tag(
    @PrimaryKey val id: String,
    val name: String,
    val color: String
)
