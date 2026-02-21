package com.androidledger.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Profile::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Wallet::class,
            parentColumns = ["id"],
            childColumns = ["walletId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Source::class,
            parentColumns = ["id"],
            childColumns = ["sourceId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["profileId"]),
        Index(value = ["walletId"]),
        Index(value = ["sourceId"]),
        Index(value = ["categoryId"]),
        Index(value = ["entry_source", "externalId"], unique = true)
    ]
)
data class Transaction(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "profileId")
    val profileId: String,

    @ColumnInfo(name = "walletId")
    val walletId: String,

    @ColumnInfo(name = "sourceId")
    val sourceId: String,

    @ColumnInfo(name = "occurredAt")
    val occurredAt: Long,

    @ColumnInfo(name = "amountMinor")
    val amountMinor: Long,

    @ColumnInfo(name = "currency")
    val currency: String,

    @ColumnInfo(name = "direction")
    val direction: String,

    @ColumnInfo(name = "merchant")
    val merchant: String? = null,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "categoryId")
    val categoryId: String? = null,

    @ColumnInfo(name = "tagIds")
    val tagIds: String? = null,

    @ColumnInfo(name = "entry_source")
    val source: String,

    @ColumnInfo(name = "externalId")
    val externalId: String? = null,

    @ColumnInfo(name = "isConfirmed", defaultValue = "1")
    val isConfirmed: Int = 1,

    @ColumnInfo(name = "transferGroupId")
    val transferGroupId: String? = null,

    @ColumnInfo(name = "createdAt")
    val createdAt: Long
)
