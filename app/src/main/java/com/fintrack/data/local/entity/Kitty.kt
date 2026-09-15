package com.fintrack.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "kitties")
data class Kitty(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val startMonth: Int,   // 1–12
    val startYear: Int,
    val createdDate: LocalDate = LocalDate.now()
)
