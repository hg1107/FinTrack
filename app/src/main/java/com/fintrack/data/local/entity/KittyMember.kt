package com.fintrack.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "kitty_members",
    foreignKeys = [ForeignKey(
        entity = Kitty::class,
        parentColumns = ["id"],
        childColumns = ["kittyId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("kittyId")]
)
data class KittyMember(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kittyId: Long,
    val name: String,
    val hostMonth: Int,   // 1–12: the month this member hosts the kitty
    val amount: Double    // this member's monthly contribution amount (₹)
)
