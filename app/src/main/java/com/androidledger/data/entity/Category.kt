package com.androidledger.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "category")
data class Category(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "icon")
    val icon: String,

    @ColumnInfo(name = "color")
    val color: String,

    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "isDefault", defaultValue = "0")
    val isDefault: Int = 0
)
