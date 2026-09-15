package com.fintrack.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class LedgerType { AUTO, MANUAL }

@Entity(tableName = "ledgers")
data class Ledger(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: LedgerType,
    val colorHex: String = "#2ECC71",
    val iconId: String = "book",
    val createdDate: LocalDate = LocalDate.now(),
    val isArchived: Boolean = false
)
