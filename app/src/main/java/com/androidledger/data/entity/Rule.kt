package com.androidledger.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "rule",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["targetCategoryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Source::class,
            parentColumns = ["id"],
            childColumns = ["targetSourceId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["targetCategoryId"]),
        Index(value = ["targetSourceId"])
    ]
)
data class Rule(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "pattern")
    val pattern: String,

    @ColumnInfo(name = "targetCategoryId")
    val targetCategoryId: String,

    @ColumnInfo(name = "targetSourceId")
    val targetSourceId: String? = null,

    @ColumnInfo(name = "priority", defaultValue = "0")
    val priority: Int = 0,

    @ColumnInfo(name = "isRegex", defaultValue = "0")
    val isRegex: Int = 0
)
