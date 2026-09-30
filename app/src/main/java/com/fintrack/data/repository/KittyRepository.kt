package com.fintrack.data.repository

import com.fintrack.data.local.dao.KittyDao
import com.fintrack.data.local.dao.KittyMemberCount
import com.fintrack.data.local.entity.Kitty
import com.fintrack.data.local.entity.KittyMember
import com.fintrack.data.local.entity.KittyPayment
import com.fintrack.data.local.entity.KittyPayout
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KittyRepository @Inject constructor(private val dao: KittyDao) {

    fun getAllKitties(): Flow<List<Kitty>> = dao.getAllKitties()
    fun getKittyCount(): Flow<Int> = dao.getKittyCount()
    suspend fun getKittyById(id: Long): Kitty? = dao.getKittyById(id)
    suspend fun insertKitty(kitty: Kitty): Long = dao.insertKitty(kitty)
    suspend fun updateKitty(kitty: Kitty) = dao.updateKitty(kitty)
    suspend fun deleteKitty(kitty: Kitty) = dao.deleteKitty(kitty)

    fun getMembersForKitty(kittyId: Long): Flow<List<KittyMember>> = dao.getMembersForKitty(kittyId)
    suspend fun getMemberById(id: Long): KittyMember? = dao.getMemberById(id)
    suspend fun insertMember(member: KittyMember): Long = dao.insertMember(member)
    suspend fun updateMember(member: KittyMember) = dao.updateMember(member)
    suspend fun deleteMember(member: KittyMember) = dao.deleteMember(member)
    fun getMemberCountsForAllKitties(): Flow<List<KittyMemberCount>> = dao.getMemberCountsForAllKitties()

    fun getPaymentsForMember(memberId: Long): Flow<List<KittyPayment>> = dao.getPaymentsForMember(memberId)
    fun getPaymentsForKittyMonth(kittyId: Long, month: Int, year: Int): Flow<List<KittyPayment>> =
        dao.getPaymentsForKittyMonth(kittyId, month, year)
    suspend fun getPaymentsForKittyMonthSuspend(kittyId: Long, month: Int, year: Int): List<KittyPayment> =
        dao.getPaymentsForKittyMonthSuspend(kittyId, month, year)
    suspend fun getLatestPaymentForKitty(kittyId: Long): KittyPayment? =
        dao.getLatestPaymentForKitty(kittyId)
    suspend fun resetPaymentsForKittyMonth(kittyId: Long, month: Int, year: Int) =
        dao.resetPaymentsForKittyMonth(kittyId, month, year)
    suspend fun getPaymentForMemberMonth(memberId: Long, month: Int, year: Int): KittyPayment? =
        dao.getPaymentForMemberMonth(memberId, month, year)
    suspend fun upsertPayment(payment: KittyPayment): Long = dao.insertPayment(payment)
    suspend fun updatePayment(payment: KittyPayment) = dao.updatePayment(payment)

    // Payouts
    fun getPayoutsForKitty(kittyId: Long): Flow<List<KittyPayout>> = dao.getPayoutsForKitty(kittyId)
    fun getPayoutsForKittyMonth(kittyId: Long, month: Int, year: Int): Flow<List<KittyPayout>> =
        dao.getPayoutsForKittyMonth(kittyId, month, year)
    suspend fun getPayoutForKittyMonthSuspend(kittyId: Long, month: Int, year: Int): KittyPayout? =
        dao.getPayoutForKittyMonthSuspend(kittyId, month, year)
    suspend fun upsertPayout(payout: KittyPayout): Long = dao.insertPayout(payout)
    suspend fun deletePayout(payout: KittyPayout) = dao.deletePayout(payout)
}
