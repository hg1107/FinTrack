package com.fintrack.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "emis",
    foreignKeys = [ForeignKey(
        entity = BankAccount::class,
        parentColumns = ["id"],
        childColumns = ["accountId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("accountId")]
)
data class Emi(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val label: String,              // e.g. "Home Loan", "Car Loan"
    val amount: Double,
    val dueDayOfMonth: Int,         // day of month (1–31)
    val tenureMonths: Int = 0,      // total number of installments (0 = indefinite/unknown)
    val startDate: LocalDate? = null, // when the EMI started, used to compute remaining
    val isPaid: Boolean = false,
    val paidDate: LocalDate? = null,
    val reminderDaysBefore: Int = 2,
    val isReminderEnabled: Boolean = true
)
