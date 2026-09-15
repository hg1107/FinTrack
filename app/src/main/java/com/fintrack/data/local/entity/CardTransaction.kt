package com.fintrack.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class TransactionType { DEBIT, CREDIT }

@Entity(
    tableName = "card_transactions",
    foreignKeys = [ForeignKey(
        entity = CreditCard::class,
        parentColumns = ["id"],
        childColumns = ["cardId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("cardId")]
)
data class CardTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: Long,
    val date: LocalDate,
    val amount: Double,
    val type: TransactionType,
    val note: String = ""
)
