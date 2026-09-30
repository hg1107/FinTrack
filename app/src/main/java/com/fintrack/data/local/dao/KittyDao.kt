package com.fintrack.data.local.dao

import androidx.room.*
import com.fintrack.data.local.entity.Kitty
import com.fintrack.data.local.entity.KittyMember
import com.fintrack.data.local.entity.KittyPayment
import com.fintrack.data.local.entity.KittyPayout
import kotlinx.coroutines.flow.Flow

/** Simple projection for member count per kitty (used on list screen). */
data class KittyMemberCount(val kittyId: Long, val count: Int)

@Dao
interface KittyDao {

    // --- Kitty ---
    @Query("SELECT * FROM kitties ORDER BY createdDate DESC")
    fun getAllKitties(): Flow<List<Kitty>>

    @Query("SELECT * FROM kitties WHERE id = :id")
    suspend fun getKittyById(id: Long): Kitty?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKitty(kitty: Kitty): Long

    @Update
    suspend fun updateKitty(kitty: Kitty)

    @Delete
    suspend fun deleteKitty(kitty: Kitty)

    // --- Members ---
    @Query("SELECT * FROM kitty_members WHERE kittyId = :kittyId ORDER BY hostMonth ASC")
    fun getMembersForKitty(kittyId: Long): Flow<List<KittyMember>>

    @Query("SELECT * FROM kitty_members WHERE id = :id")
    suspend fun getMemberById(id: Long): KittyMember?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: KittyMember): Long

    @Update
    suspend fun updateMember(member: KittyMember)

    @Delete
    suspend fun deleteMember(member: KittyMember)

    /** Returns member count per kitty — used to populate list-screen badges without N+1 queries. */
    @Query("SELECT kittyId, COUNT(*) as count FROM kitty_members GROUP BY kittyId")
    fun getMemberCountsForAllKitties(): Flow<List<KittyMemberCount>>

    // --- Payments ---
    @Query("SELECT * FROM kitty_payments WHERE memberId = :memberId ORDER BY year ASC, month ASC")
    fun getPaymentsForMember(memberId: Long): Flow<List<KittyPayment>>

    @Query("""
        SELECT * FROM kitty_payments 
        WHERE memberId IN (SELECT id FROM kitty_members WHERE kittyId = :kittyId)
        AND month = :month AND year = :year
    """)
    fun getPaymentsForKittyMonth(kittyId: Long, month: Int, year: Int): Flow<List<KittyPayment>>

    @Query("""
        SELECT * FROM kitty_payments 
        WHERE memberId IN (SELECT id FROM kitty_members WHERE kittyId = :kittyId)
        AND month = :month AND year = :year
    """)
    suspend fun getPaymentsForKittyMonthSuspend(kittyId: Long, month: Int, year: Int): List<KittyPayment>

    @Query("""
        SELECT * FROM kitty_payments 
        WHERE memberId IN (SELECT id FROM kitty_members WHERE kittyId = :kittyId)
        AND isPaid = 1
        ORDER BY year DESC, month DESC LIMIT 1
    """)
    suspend fun getLatestPaymentForKitty(kittyId: Long): KittyPayment?

    @Query("""
        UPDATE kitty_payments 
        SET isPaid = 0, amountPaid = 0.0, datePaid = NULL, paymentMode = NULL, onlineAccountName = NULL, note = NULL
        WHERE memberId IN (SELECT id FROM kitty_members WHERE kittyId = :kittyId)
        AND month = :month AND year = :year
    """)
    suspend fun resetPaymentsForKittyMonth(kittyId: Long, month: Int, year: Int)

    @Query("SELECT * FROM kitty_payments WHERE memberId = :memberId AND month = :month AND year = :year LIMIT 1")
    suspend fun getPaymentForMemberMonth(memberId: Long, month: Int, year: Int): KittyPayment?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: KittyPayment): Long

    @Update
    suspend fun updatePayment(payment: KittyPayment)

    @Delete
    suspend fun deletePayment(payment: KittyPayment)

    @Query("SELECT COUNT(*) FROM kitties")
    fun getKittyCount(): Flow<Int>

    // --- Payouts ---
    @Query("SELECT * FROM kitty_payouts WHERE kittyId = :kittyId ORDER BY year DESC, month DESC")
    fun getPayoutsForKitty(kittyId: Long): Flow<List<KittyPayout>>

    @Query("SELECT * FROM kitty_payouts WHERE kittyId = :kittyId AND month = :month AND year = :year ORDER BY id ASC")
    fun getPayoutsForKittyMonth(kittyId: Long, month: Int, year: Int): Flow<List<KittyPayout>>

    @Query("SELECT * FROM kitty_payouts WHERE kittyId = :kittyId AND month = :month AND year = :year LIMIT 1")
    suspend fun getPayoutForKittyMonthSuspend(kittyId: Long, month: Int, year: Int): KittyPayout?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayout(payout: KittyPayout): Long

    @Update
    suspend fun updatePayout(payout: KittyPayout)

    @Delete
    suspend fun deletePayout(payout: KittyPayout)
}
