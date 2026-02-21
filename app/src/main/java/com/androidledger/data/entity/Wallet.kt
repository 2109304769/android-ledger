package com.androidledger.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "wallet",
    foreignKeys = [
        ForeignKey(
            entity = Profile::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["profileId"])]
)
data class Wallet(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "profileId")
    val profileId: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "currency")
    val currency: String,

    @ColumnInfo(name = "sortOrder", defaultValue = "0")
    val sortOrder: Int = 0,

    @ColumnInfo(name = "createdAt")
    val createdAt: Long
)
