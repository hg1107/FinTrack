package com.fintrack.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class LedgerSourceType { KITTY_PAYMENT, CARD_TXN, ACCOUNT_TXN, MANUAL, RECURRING }

@Entity(
    tableName = "ledger_entries",
    foreignKeys = [ForeignKey(
        entity = Ledger::class,
        parentColumns = ["id"],
        childColumns = ["ledgerId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [
        Index("ledgerId"),
        Index(value = ["sourceType", "sourceId"])
    ]
)
data class LedgerEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ledgerId: Long,
    val amount: Double,
    val date: LocalDate,
    val note: String? = null,
    val sourceType: LedgerSourceType,
    val sourceId: Long? = null,
    val isRecurring: Boolean = false
)
