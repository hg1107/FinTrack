package com.fintrack.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Records who received the kitty collection in a given month.
 * One payout record per kitty per month (unique constraint).
 */
@Entity(
    tableName = "kitty_payouts",
    foreignKeys = [
        ForeignKey(
            entity = Kitty::class,
            parentColumns = ["id"],
            childColumns = ["kittyId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = KittyMember::class,
            parentColumns = ["id"],
            childColumns = ["hostMemberId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("kittyId"),
        Index("hostMemberId"),
        Index(value = ["kittyId", "month", "year"], unique = true)
    ]
)
data class KittyPayout(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kittyId: Long,
    val month: Int,                        // 1–12
    val year: Int,
    val hostMemberId: Long,                // which member received the payout
    val amountReceived: Double = 0.0,
    val receivedDate: LocalDate? = null,
    val notes: String = ""
)
