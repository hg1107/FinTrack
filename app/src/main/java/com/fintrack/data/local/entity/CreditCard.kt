package com.fintrack.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "credit_cards")
data class CreditCard(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val lastFourDigits: String = "",   // optional display digits
    val creditLimit: Double = 0.0,
    val billingDueDay: Int = 0,        // day of month payment is due (0 = not set)
    val statementDay: Int = 0,         // day of month statement is generated (0 = not set)
    val createdDate: LocalDate = LocalDate.now()
)
