package com.androidledger.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profile")
data class Profile(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "emoji")
    val emoji: String,

    @ColumnInfo(name = "isDefault", defaultValue = "0")
    val isDefault: Int = 0,

    @ColumnInfo(name = "createdAt")
    val createdAt: Long
)
