package com.fintrack.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class PaymentMode { OFFICE, ONLINE, CASH }

@Entity(
    tableName = "kitty_payments",
    foreignKeys = [ForeignKey(
        entity = KittyMember::class,
        parentColumns = ["id"],
        childColumns = ["memberId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [
        Index("memberId"),
        Index(value = ["memberId", "month", "year"], unique = true)
    ]
)
data class KittyPayment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val memberId: Long,
    val month: Int,       // 1–12
    val year: Int,
    val isPaid: Boolean = false,
    val amountPaid: Double = 0.0,
    val datePaid: LocalDate? = null,
    val paymentMode: PaymentMode? = null,
    val onlineAccountName: String? = null,  // e.g. "GPay", "PhonePe" — only for ONLINE mode
    val note: String? = null
)
