package com.fintrack.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "bank_accounts")
data class BankAccount(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val lastFourDigits: String = "",   // optional display digits
    val createdDate: LocalDate = LocalDate.now()
)
