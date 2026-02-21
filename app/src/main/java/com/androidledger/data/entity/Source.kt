package com.androidledger.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "source",
    foreignKeys = [
        ForeignKey(
            entity = Wallet::class,
            parentColumns = ["id"],
            childColumns = ["walletId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["walletId"])]
)
data class Source(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "walletId")
    val walletId: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "balanceSnapshot", defaultValue = "0")
    val balanceSnapshot: Long = 0,

    @ColumnInfo(name = "balanceUpdatedAt", defaultValue = "0")
    val balanceUpdatedAt: Long = 0,

    @ColumnInfo(name = "icon")
    val icon: String? = null,

    @ColumnInfo(name = "isArchived", defaultValue = "0")
    val isArchived: Int = 0
)
