package com.fintrack.data.local.db

import androidx.room.TypeConverter
import com.fintrack.data.local.entity.LedgerSourceType
import com.fintrack.data.local.entity.LedgerType
import com.fintrack.data.local.entity.PaymentMode
import com.fintrack.data.local.entity.TransactionType
import java.time.LocalDate

class Converters {

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    @TypeConverter
    fun fromPaymentMode(mode: PaymentMode?): String? = mode?.name

    @TypeConverter
    fun toPaymentMode(value: String?): PaymentMode? = value?.let { PaymentMode.valueOf(it) }

    @TypeConverter
    fun fromTransactionType(type: TransactionType): String = type.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

    @TypeConverter
    fun fromLedgerType(type: LedgerType): String = type.name

    @TypeConverter
    fun toLedgerType(value: String): LedgerType = LedgerType.valueOf(value)

    @TypeConverter
    fun fromLedgerSourceType(type: LedgerSourceType): String = type.name

    @TypeConverter
    fun toLedgerSourceType(value: String): LedgerSourceType = LedgerSourceType.valueOf(value)
}

