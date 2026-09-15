package com.fintrack.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "account_transactions",
    foreignKeys = [ForeignKey(
        entity = BankAccount::class,
        parentColumns = ["id"],
        childColumns = ["accountId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("accountId")]
)
data class AccountTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val date: LocalDate,
    val amount: Double,
    val type: TransactionType,   // reusing enum from CardTransaction.kt
    val note: String = ""
)
